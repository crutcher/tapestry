package loom.graph.export;

import com.google.common.html.HtmlEscapers;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.attribute.Shape;
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
import loom.graph.TEdge;
import loom.graph.TGraph;
import loom.graph.TTag;

@SuperBuilder
public final class TGraphDotExporter {
  @Builder.Default private boolean enableNodeIds = false;

  public TGraphDotExporter() {}

  public BufferedImage toImage(TGraph tgraph) {
    return Graphviz.fromGraph(toGraph(tgraph)).render(Format.PNG).toImage();
  }

  static String formatData(Object data) {
    if (data instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> m = (Map<String, Object>) data;

      return "<table border=\"0\" cellborder=\"1\" cellspacing\"0\">"
          + formatDataTableRow(m)
          + "</table>";
    } else {
      return HtmlEscapers.htmlEscaper().escape(data.toString());
    }
  }

  static String formatDataTableRow(Map<String, Object> data) {
    StringBuilder sb = new StringBuilder();
    for (var entry : data.entrySet()) {
      sb.append("<tr>")
          .append("<td align=\"right\"><b>")
          .append(entry.getKey())
          .append(":</b></td>")
          .append("<td align=\"left\">")
          .append(formatData(entry.getValue()))
          .append("</td>")
          .append("</tr>");
    }
    return sb.toString();
  }

  // TODO: builder options
  // - show/hide ids
  public Graph toGraph(TGraph tgraph) {
    Graph g =
        Factory.graph("G")
            .directed()
            .graphAttr()
            .with(Rank.dir(Rank.RankDir.RIGHT_TO_LEFT))
            .graphAttr()
            .with("nodesep", "0.7");

    class GenSym {
      int idx = 0;

      String next() {
        return Integer.toHexString(++idx);
      }
    }

    var gensym = new GenSym();

    Map<UUID, String> displayIds =
        tgraph
            .queryNodes()
            .toStream()
            .collect(
                Collectors.toMap(
                    tnode -> tnode.id,
                    tnode -> {
                      String pre = "N";
                      if (tnode instanceof TEdge) {
                        pre = "E";
                      } else if (tnode instanceof TTag) {
                        pre = "T";
                      }
                      return tnode.jsonTypeName() + "." + pre + gensym.next();
                    }));

    for (var tnode : tgraph) {
      Map<String, Object> data = JsonUtil.toMap(tnode);
      data.remove("@type");
      if (!enableNodeIds) {
        data.remove("id");

        if (tnode instanceof TTag<?>) {
          data.remove("sourceId");
        }
        if (tnode instanceof TEdge<?, ?>) {
          data.remove("targetId");
        }
      }

      Map<String, String> tableAttrs =
          new HashMap<>(
              Map.of(
                  "border", "0",
                  "cellborder", "1",
                  "cellspacing", "0",
                  "cellpadding", "4"));

      String bgcolor = tnode.displayBackgroundColor();
      if (!bgcolor.isEmpty()) {
        tableAttrs.put("bgcolor", bgcolor);
      }

      String title = displayIds.get(tnode.id);

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
              + formatDataTableRow(data)
              + "</table>>";

      var gnode = Factory.node(tnode.id.toString()).with(Shape.PLAIN).with(Label.raw(label));

      g = g.with(gnode);
    }

    for (TTag<?> ttag : tgraph.queryTags(TTag.class).toList()) {
      g =
          g.with(
              Factory.node(ttag.sourceId.toString())
                  .link(Factory.to(Factory.node(ttag.id.toString()))));
    }

    for (TEdge<?, ?> tedge : tgraph.queryEdges(TEdge.class).toList()) {
      g =
          g.with(
              Factory.node(tedge.id.toString())
                  .link(Factory.to(Factory.node(tedge.targetId.toString()))));
    }

    return g;
  }
}
