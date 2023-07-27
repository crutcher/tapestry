package loom.alt.attrgraph;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.html.HtmlEscapers;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Factory;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.Node;
import lombok.Builder;
import loom.common.JsonUtil;
import org.apache.commons.text.StringEscapeUtils;

import javax.annotation.Nonnull;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Builder
public class LoomGraphExporter {

  @Builder.Default private Rank.RankDir rankDir = Rank.RankDir.RIGHT_TO_LEFT;

  @Builder.Default private Integer minPrefixLength = 2;

  @Nonnull private LoomGraph graph;

  @Builder.Default private Map<UUID, String> idToAliasMap = new HashMap<>();

  @Builder.Default private Map<String, String> nsToAliasMap = new HashMap<>();

  private LoomEnvironment environment;

  public BufferedImage toImage() {
    return Graphviz.fromGraph(toGraph()).render(Format.PNG).toImage();
  }

  /**
   * Assign display symbols to nodes.
   *
   * <p>We want to give every node a short display name. 1. We'd like to derive the symbol from the
   * node's UUID. 2. We'd like the symbol to be as short as possible.
   *
   * <p>Process: 1. Take the md5 hash of the UUID, and 2. Find the shortest prefixes of these ids
   * that are unique.
   *
   * @param graph The graph to assign symbols to.
   * @param minPrefixLength The minimum length of the prefix to try.
   * @return A map from node UUIDs to symbols.
   */
  static Map<UUID, String> buildIdToAliasMap(LoomGraph graph, int minPrefixLength) {
    Map<UUID, String> longSyms = new HashMap<>();
    for (var tnode : graph.nodes()) {
      try {
        var digest =
            MessageDigest.getInstance("MD5")
                .digest(tnode.getId().toString().getBytes(StandardCharsets.UTF_8));
        longSyms.put(tnode.getId(), HexFormat.of().formatHex(digest).toUpperCase(Locale.ROOT));
      } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
      }
    }

    for (int i = minPrefixLength; i <= 32; i++) {
      Map<UUID, String> shortSyms = new HashMap<>();
      for (var entry : longSyms.entrySet()) {
        shortSyms.put(entry.getKey(), entry.getValue().substring(0, i));
      }
      if (shortSyms.size() == longSyms.size()) {
        return shortSyms;
      }
    }

    return longSyms;
  }

  /**
   * Map positive integers to base-26 strings.
   *
   * @param i The integer to map.
   * @return The base-26 string.
   */
  public static String toBase26(int i) {
    i--;
    if (i < 26) {
      return "" + (char) ('a' + i);
    } else {
      return toBase26(i / 26) + toBase26(i % 26);
    }
  }

  private Map<String, String> buildNamespaceToAliasMap() {
    Map<String, String> abbrev = new TreeMap<>();

    environment.aliasMap.forEach((k, v) -> abbrev.put(v, k));

    Supplier<String> nextSym =
        () -> {
          int idx = abbrev.size();
          while (true) {
            var abrv = toBase26(idx);
            if (!abbrev.containsValue(abrv)) {
              return abrv;
            }
            idx++;
          }
        };

    Consumer<String> addAbbrev =
        (namespace) -> {
          if (abbrev.containsKey(namespace)) {
            return;
          }
          abbrev.put(namespace, nextSym.get());
        };

    graph.namespaces().stream().sorted().forEach(addAbbrev);
    return abbrev;
  }

  /**
   * Escape a string for use in HTML.
   *
   * @param s The string to escape.
   * @return The escaped string.
   */
  static String htmlEscape(String s) {
    return HtmlEscapers.htmlEscaper().escape(s);
  }

  /**
   * Get the abbreviated name of a NSName.
   *
   * @param name The name to abbreviate.
   * @return The abbreviated name.
   */
  String abbreviatedName(NSName name) {
    return String.format("%s:%s", nsToAliasMap.get(name.urn()), name.name());
  }

  public Graph toGraph() {
    idToAliasMap = buildIdToAliasMap(graph, minPrefixLength);
    nsToAliasMap = buildNamespaceToAliasMap();

    Graph g =
        Factory.graph("G")
            .directed()
            .graphAttr()
            .with(Rank.dir(rankDir))
            .graphAttr()
            .with("nodesep", "0.7")
            .nodeAttr()
            .with("margin", "0.1")
            .nodeAttr()
            .with("shape", "box");

    {
      // Generate a legend node for the abbreviations.
      var legend = Factory.node("legend").with("shape", "none");
      var sb = new StringBuilder();
      sb.append("<table border=\"0\" cellborder=\"0\" cellspacing=\"0\">");
      sb.append("<tr><td colspan=\"2\"><b><u>Namespaces</u></b></td></tr>");
      for (var entry : nsToAliasMap.entrySet()) {
        sb.append("<tr><td>")
            .append(entry.getValue())
            .append(":</td><td>")
            .append(htmlEscape(entry.getKey()))
            .append("</td></tr>");
      }
      sb.append("</table>");
      legend = legend.with(Label.raw("<" + sb.toString() + ">"));
      g = g.with(legend);
    }

    for (var node :
        graph.nodeStream().sorted(Comparator.comparing(LoomGraph.Node::getId)).toList()) {
      var id = node.getId();
      var sym = idToAliasMap.get(id);
      var gnode = Factory.node(id.toString()).with("xlabel", "#" + sym).with("style", "filled");

      var links = new HashMap<List<Object>, UUID>();

      var sb = new StringBuilder();
      sb.append("<table border=\"0\">");
      sb.append("<tr><td><b>");
      sb.append(abbreviatedName(node.getType()));
      sb.append("</b></td></tr>");
      if (node.getAttrs().size() > 0) {
        sb.append("<tr><td><table border=\"0\" cellspacing=\"0\" cellborder=\"1\">");
        for (var attr : node.attrStream().sorted(Map.Entry.comparingByKey()).toList()) {
          NSName name = attr.getKey();
          JsonNode value = attr.getValue();
          findLinks(List.of(name), value, links);

          sb.append("<tr><td>");
          sb.append(abbreviatedName(name));
          sb.append("</td><td>");
          sb.append(displayAttribute(List.of(name), value));
          sb.append("</td></tr>");
        }
        sb.append("</table></td></tr>");
      }
      sb.append("</table>");

      gnode = gnode.with("label", Label.raw("<" + sb.toString() + ">"));

      g = g.with(gnode);

      for (var entry : links.entrySet()) {
        var path = entry.getKey();
        var attr = NSName.class.cast(path.get(0));
        var target = entry.getValue();

        if (!graph.hasNode(target)) {
          continue;
        }

        LoomSchema.Attribute attribute = null;
        try {
          attribute = environment.getAttribute(attr);
        } catch (NoSuchElementException e) {
          // ignore
        }
        var invertEdge = attribute != null && attribute.isInvertEdge();

        var label = formatAttributePath(path);

        var linkNode = Factory.node(target.toString());

        Node from, to;
        if (invertEdge) {
          from = linkNode;
          to = gnode;
        } else {
          from = gnode;
          to = linkNode;
        }
        g = g.with(from.link(Factory.to(to).with(Label.of(label))));
      }
    }

    return g;
  }

  /**
   * Given a path of [NSName, (String | Integer) ...] return a string representation of the path.
   *
   * <p>Usess the namespace abbreviations to shorten the NSName, displays String names with a '/'
   * prefix, and displays Integer names with a '[]' wrapper..
   *
   * @param path the path.
   * @return the string representation.
   */
  String formatAttributePath(List<Object> path) {
    var sb = new StringBuilder();
    for (int i = 0; i < path.size(); ++i) {
      var p = path.get(i);
      if (p instanceof NSName name) {
        sb.append(abbreviatedName(name));
      }
      if (p instanceof String) {
        sb.append("/").append(p);
      }
      if (p instanceof Integer) {
        sb.append("[" + p + "]");
      }
    }
    return sb.toString();
  }

  /**
   * Given an attribute prefix path and node value, find nested graph links.
   *
   * @param path the attribute prefix path.
   * @param node the node value.
   * @param links the map of links to update.
   */
  void findLinks(List<Object> path, JsonNode node, Map<List<Object>, UUID> links) {
    if (node.isValueNode()) {
      String text = node.asText();
      if (node.isTextual()) {
        try {
          var id = UUID.fromString(text);
          links.put(path, id);
        } catch (IllegalArgumentException e) {
          // ignore
        }
      }
    } else if (node instanceof ArrayNode arrayNode) {
      for (int i = 0; i < node.size(); ++i) {
        var p = new ArrayList<>(path);
        p.add(i);
        findLinks(p, arrayNode.get(i), links);
      }
    } else if (node instanceof ObjectNode objectNode) {
      objectNode
          .fields()
          .forEachRemaining(
              e -> {
                var p = new ArrayList<>(path);
                p.add(e.getKey());
                findLinks(p, e.getValue(), links);
              });
    }
  }

  /**
   * Recursively a display table for an attribute.
   *
   * @param path the path to the attribute.
   * @param node the current node.
   * @return the display table.
   */
  String displayAttribute(List<Object> path, JsonNode node) {
    if (node.isValueNode()) {
      String text = node.asText();
      if (node.isTextual()) {
        try {
          var id = UUID.fromString(text);
          if (idToAliasMap.containsKey(id)) {
            return "#" + idToAliasMap.get(id);
          }
        } catch (IllegalArgumentException e) {
          // ignore
        }

        try {
          var name = NSName.parse(text);
          return abbreviatedName(name);
        } catch (IllegalArgumentException e) {
          // ignore
        }

        text = String.format("\"%s\"", StringEscapeUtils.escapeJava(text));
      }

      return htmlEscape(text);

    } else if (node instanceof ArrayNode arrayNode) {
      if (JsonUtil.isValueArray(arrayNode)) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int idx = 0; idx < arrayNode.size(); idx++) {
          if (idx > 0) {
            sb.append(", ");
          }

          var itemPath = new ArrayList<>(path);
          itemPath.add(idx);
          sb.append(displayAttribute(itemPath, arrayNode.get(idx)));
        }
        sb.append("]");
        return sb.toString();

      } else {
        StringBuilder sb = new StringBuilder();
        sb.append("<table border=\"0\" cellspacing=\"0\" cellborder=\"1\">");
        for (int idx = 0; idx < arrayNode.size(); idx++) {
          var itemPath = new ArrayList<>(path);
          itemPath.add(idx);
          sb.append("<tr><td>")
              .append(displayAttribute(itemPath, arrayNode.get(idx)))
              .append("</td></tr>");
        }
        sb.append("</table>");
        return sb.toString();
      }

    } else if (node instanceof ObjectNode objectNode) {
      StringBuilder sb = new StringBuilder();
      sb.append("<table border=\"0\" cellspacing=\"0\" cellborder=\"1\">");
      objectNode
          .fields()
          .forEachRemaining(
              (entry) -> {
                var itemPath = new ArrayList<>(path);
                itemPath.add(entry.getKey());

                sb.append("<tr><td>")
                    .append(entry.getKey())
                    .append("</td><td>")
                    .append(displayAttribute(itemPath, entry.getValue()))
                    .append("</td></tr>");
              });
      sb.append("</table>");
      return sb.toString();
    } else {
      throw new IllegalArgumentException("Unknown node type: " + node.getClass());
    }
  }
}
