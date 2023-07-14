package loom.alt.linkgraph.graph.export;

import com.google.common.html.HtmlEscapers;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Factory;
import guru.nidi.graphviz.model.Graph;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import loom.alt.linkgraph.graph.*;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

@SuperBuilder
public final class TGraphDotExporter {
  @Builder.Default private boolean enableNodeIds = false;

  @Builder.Default private boolean markNotObservable = true;

  @Builder.Default private Rank.RankDir rankDir = Rank.RankDir.RIGHT_TO_LEFT;

  @Builder.Default private int minPrefixLength = 2;

  public BufferedImage toImage(TGraph tgraph) {
    return Graphviz.fromGraph(toGraph(tgraph)).render(Format.PNG).toImage();
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
   * @param tGraph The graph to assign symbols to.
   * @param minPrefixLength The minimum length of the prefix to try.
   * @return A map from node UUIDs to symbols.
   */
  private static Map<UUID, String> nodeSymbols(TGraph tGraph, int minPrefixLength) {
    Map<UUID, String> longSyms = new HashMap<>();
    for (var tnode : tGraph) {
      try {
        var digest =
            MessageDigest.getInstance("MD5")
                .digest(tnode.id.toString().getBytes(StandardCharsets.UTF_8));
        longSyms.put(tnode.id, HexFormat.of().formatHex(digest).toUpperCase(Locale.ROOT));
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

  public Graph toGraph(TGraph tgraph) {
    Graph g =
        Factory.graph("G")
            .directed()
            .graphAttr()
            .with(Rank.dir(rankDir))
            .graphAttr()
            .with("nodesep", "0.7")
            .nodeAttr()
            .with("margin", "0.1");

    Set<TNodeInterface> observedNodes = null;
    if (markNotObservable) {
      var observers = tgraph.queryNodes(TObserver.class).toList();
      if (observers.size() > 0) {
        observedNodes = observers.get(0).findObservedNodes();
      }
    }

    var nodeSyms = nodeSymbols(tgraph, minPrefixLength);

    for (var tnode : tgraph) {
      var sym = nodeSyms.get(tnode.id);

      String title = tnode.jsonTypeName();

      Map<String, Object> data = tnode.displayData();
      data.remove("@type");
      if (!enableNodeIds) {
        data.remove("id");

        if (tnode instanceof TTagBase<?>) {
          data.remove("sourceId");
        }
        if (tnode instanceof TEdgeBase<?, ?>) {
          data.remove("targetId");
        }
      }

      Map<String, String> tableAttrs =
          new HashMap<>(
              Map.of(
                  "border", "0",
                  "cellborder", "0",
                  "cellspacing", "0"));

      var attrs =
          tableAttrs.entrySet().stream()
              .map(e -> e.getKey() + "=\"" + e.getValue() + "\"")
              .collect(Collectors.joining(" "));

      String label =
          "<table "
              + attrs
              + ">"
              + "<tr><td colspan=\"2\">"
              + title
              + "</td></tr>"
              + formatRecursiveDataRow(data)
              + "</table>";

      var gnode =
          Factory.node(tnode.id.toString())
              .with(Label.raw("<" + label + ">"))
              .with("xlabel", "#" + sym);
      for (var entry : tnode.nodeDisplayOptions().nodeAttributes.entrySet()) {
        gnode = gnode.with(entry.getKey(), entry.getValue());
      }
      if (observedNodes != null && !observedNodes.contains(tnode)) {
        gnode = gnode.with("fillcolor", "#eeeeee");
      }

      if (tnode instanceof TEdgeBase<?, ?> tedge
          && data.isEmpty()
          && tgraph
              .queryEdges(TEdgeBase.class)
              .withTargetId(tedge.id)
              .toStream()
              .toList()
              .isEmpty()) {

        var link = Factory.to(Factory.node(tedge.targetId.toString()));
        if (!tedge.edgeDisplayOptions().constrainEdge) {
          link = link.with("constraint", "false");
        }

        g = g.with(Factory.node(tedge.sourceId.toString()).link(link.with(Label.of(title))));

        continue;
      }

      g = g.with(gnode);

      boolean isEdge = false;
      boolean constrainEdge = true;

      if (tnode instanceof TEdgeBase<?, ?> tedge) {
        var link =
            Factory.to(Factory.node(tedge.targetId.toString()))
                .with("dir", "both")
                .with("arrowtail", "oinv");

        constrainEdge = tedge.edgeDisplayOptions().constrainEdge;
        if (!constrainEdge) {
          link = link.with("constraint", "false");
        }

        g = g.with(Factory.node(tedge.id.toString()).link(link));
        isEdge = true;
      }

      if (tnode instanceof TTagBase<?> ttag) {
        // TODO: Better way of determining tag placement;
        // Assuming that we should flip the direction of stand-alone tags
        // (i.e. tags that are not edges) is a hack.

        if (!isEdge) {
          var link = Factory.to(Factory.node(ttag.sourceId.toString()));
          g = g.with(Factory.node(ttag.id.toString()).link(link));
        } else {
          var link = Factory.to(Factory.node(ttag.id.toString()));
          if (!constrainEdge) {
            link = link.with("constraint", "false");
          }
          g = g.with(Factory.node(ttag.sourceId.toString()).link(link.with("arrowhead", "odot")));
        }
      }
    }

    return g;
  }

  private String formatRecursiveData(Object data) {
    if (data instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> m = (Map<String, Object>) data;

      return "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\">"
          + formatRecursiveDataRow(m)
          + "</table>";
    } else {
      String field;
      if (data instanceof String) {
        field = String.format("\"%s\"", data);
      } else {
        field = data.toString();
      }
      return HtmlEscapers.htmlEscaper().escape(field);
    }
  }

  private String formatRecursiveDataRow(Map<String, Object> data) {
    StringBuilder sb = new StringBuilder();
    for (var entry : data.entrySet()) {
      sb.append("<tr>")
          .append("<td align=\"right\"><b>")
          .append(entry.getKey())
          .append(":</b></td>")
          .append("<td align=\"left\">")
          .append(formatRecursiveData(entry.getValue()))
          .append("</td>")
          .append("</tr>");
    }
    return sb.toString();
  }
}
