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
import lombok.Builder;
import org.apache.commons.text.StringEscapeUtils;

import javax.annotation.Nonnull;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Builder
public class LoomGraphExporter {

  @Builder.Default private Rank.RankDir rankDir = Rank.RankDir.RIGHT_TO_LEFT;

  @Builder.Default private Integer minPrefixLength = 2;

  @Nonnull private LoomGraph graph;

  @Builder.Default private Map<UUID, String> idSymbols = new HashMap<>();

  @Builder.Default private Map<String, String> nsAbbrevs = new HashMap<>();

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
  private static Map<UUID, String> nodeSymbols(LoomGraph graph, int minPrefixLength) {
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

  public static String toBase26(int i) {
    i--;
    if (i < 26) {
      return "" + (char) ('a' + i);
    } else {
      return toBase26(i / 26) + toBase26(i % 26);
    }
  }

  private static Map<String, String> namespaceAbbrev(LoomGraph graph) {
    Map<String, String> abbrev = new TreeMap<>();
    Consumer<String> addAbbrev =
        (namespace) -> {
          abbrev.put(namespace, toBase26(abbrev.size() + 1));
        };
    graph.namespaces().stream().distinct().sorted().forEach(addAbbrev);
    return abbrev;
  }

  private static String htmlEscape(String s) {
    return HtmlEscapers.htmlEscaper().escape(s);
  }

  private String displayName(NSName name) {
    return String.format("%s:%s", nsAbbrevs.get(name.urn()), name.name());
  }

  public Graph toGraph() {
    idSymbols = nodeSymbols(graph, minPrefixLength);
    nsAbbrevs = namespaceAbbrev(graph);

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
      for (var entry : nsAbbrevs.entrySet()) {
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
      var sym = idSymbols.get(id);
      var gnode = Factory.node(id.toString()).with("xlabel", "#" + sym).with("style", "filled");

      var links = new HashMap<List<Object>, UUID>();
      BiConsumer<List<Object>, UUID> onLink = links::put;

      var sb = new StringBuilder();
      sb.append("<table border=\"0\">");
      sb.append("<tr><td><b>");
      sb.append(displayName(node.getType()));
      sb.append("</b></td></tr>");
      if (node.getAttrs().size() > 0) {
        sb.append("<tr><td><table border=\"0\" cellspacing=\"0\" cellborder=\"1\">");
        for (var attr : node.attrStream().sorted(Map.Entry.comparingByKey()).toList()) {
          sb.append("<tr><td>");
          sb.append(displayName(attr.getKey()));
          sb.append("</td><td>");
          sb.append(displayAttribute(List.of(attr.getKey()), attr.getValue(), onLink));
          sb.append("</td></tr>");
        }
        sb.append("</table></td></tr>");
      }
      sb.append("</table>");

      gnode = gnode.with("label", Label.raw("<" + sb.toString() + ">"));

      g = g.with(gnode);

      for (var entry : links.entrySet()) {
        var path = entry.getKey();
        // var attr = NSName.class.cast(path.get(0));
        var target = entry.getValue();

        List<String> parts = new ArrayList<>();
        for (var p : path) {
          if (p instanceof NSName name) {
            parts.add(displayName(name));
          } else {
            parts.add(p.toString());
          }
        }
        var label = parts.stream().collect(Collectors.joining("/"));

        g = g.with(gnode.link(Factory.to(Factory.node(target.toString())).with(Label.of(label))));
      }
    }

    return g;
  }

  public boolean isValueArray(ArrayNode node) {
    for (JsonNode child : node) {
      if (!child.isValueNode()) {
        return false;
      }
    }
    return true;
  }

  private String displayAttribute(
      List<Object> path, JsonNode node, BiConsumer<List<Object>, UUID> onLink) {
    if (node.isValueNode()) {
      String text = node.asText();
      if (node.isTextual()) {
        try {
          var id = UUID.fromString(text);
          onLink.accept(path, id);
          if (idSymbols.containsKey(id)) {
            return "#" + idSymbols.get(id);
          }
        } catch (IllegalArgumentException e) {
          // ignore
        }

        try {
          var name = NSName.parse(text);
          return displayName(name);
        } catch (IllegalArgumentException e) {
          // ignore
        }

        text = String.format("\"%s\"", StringEscapeUtils.escapeJava(text));
      }

      return htmlEscape(text);

    } else if (node instanceof ArrayNode arrayNode) {
      if (isValueArray(arrayNode)) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int idx = 0; idx < arrayNode.size(); idx++) {
          if (idx > 0) {
            sb.append(", ");
          }

          var itemPath = new ArrayList<>(path);
          itemPath.add("[" + idx + "]");
          sb.append(displayAttribute(itemPath, arrayNode.get(idx), onLink));
        }
        sb.append("]");
        return sb.toString();

      } else {
        StringBuilder sb = new StringBuilder();
        sb.append("<table border=\"0\" cellspacing=\"0\" cellborder=\"1\">");
        for (int idx = 0; idx < arrayNode.size(); idx++) {
          var itemPath = new ArrayList<>(path);
          itemPath.add("[" + idx + "]");
          sb.append("<tr><td>")
              .append(displayAttribute(itemPath, arrayNode.get(idx), onLink))
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
                    .append(displayAttribute(itemPath, entry.getValue(), onLink))
                    .append("</td></tr>");
              });
      sb.append("</table>");
      return sb.toString();
    } else {
      throw new IllegalArgumentException("Unknown node type: " + node.getClass());
    }
  }
}
