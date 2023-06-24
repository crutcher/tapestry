package loom.graph.export;

import com.google.common.html.HtmlEscapers;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Factory;
import guru.nidi.graphviz.model.Graph;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import loom.common.JsonUtil;
import loom.graph.TEdgeBase;
import loom.graph.TGraph;
import loom.graph.TTagBase;

@SuperBuilder
public final class TGraphDotExporter {
  @Builder.Default private boolean enableNodeIds = false;

  public TGraphDotExporter() {}

  public BufferedImage toImage(TGraph tgraph) {
    return Graphviz.fromGraph(toGraph(tgraph)).render(Format.PNG).toImage();
  }

  public Graph toGraph(TGraph tgraph) {
    Graph g =
        Factory.graph("G")
            .directed()
            .graphAttr()
            .with(Rank.dir(Rank.RankDir.RIGHT_TO_LEFT))
            .graphAttr()
            .with("nodesep", "0.7");

    Map<UUID, String> syms = new HashMap<>();
    for (var tnode : tgraph) {
      String nodeTypePrefix;
      if (tnode instanceof TEdgeBase) {
        nodeTypePrefix = "E";
      } else if (tnode instanceof TTagBase) {
        nodeTypePrefix = "T";
      } else {
        nodeTypePrefix = "N";
      }
      var sym =
          String.format(
              "%s%s: %s",
              nodeTypePrefix, Integer.toHexString(syms.size() + 1), tnode.jsonTypeName());
      syms.put(tnode.id, sym);
    }

    for (var tnode : tgraph) {
      String title = syms.get(tnode.id);
      var displayOptions = tnode.displayOptions();

      Map<String, Object> data = JsonUtil.toMap(tnode);
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
          "<<table "
              + attrs
              + ">"
              + "<tr><td colspan=\"2\">"
              + title
              + "</td></tr>"
              + formatRecursiveDataRow(data)
              + "</table>>";

      var gnode =
          Factory.node(tnode.id.toString())
              .with("shape", displayOptions.nodeShape)
              .with("style", "filled")
              .with("fillcolor", displayOptions.backgroundColor)
              .with(Label.raw(label));

      g = g.with(gnode);

      if (tnode instanceof TTagBase<?> ttag) {
        g =
            g.with(
                Factory.node(ttag.sourceId.toString())
                    .link(Factory.to(Factory.node(ttag.id.toString()))));
      }

      if (tnode instanceof TEdgeBase<?, ?> tedge) {
        g =
            g.with(
                Factory.node(tedge.id.toString())
                    .link(Factory.to(Factory.node(tedge.targetId.toString()))));
      }
    }

    return g;
  }

  private String formatRecursiveData(Object data) {
    if (data instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> m = (Map<String, Object>) data;

      return "<table border=\"0\" cellborder=\"1\" cellspacing\"0\">"
          + formatRecursiveDataRow(m)
          + "</table>";
    } else {
      return HtmlEscapers.htmlEscaper().escape(data.toString());
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
