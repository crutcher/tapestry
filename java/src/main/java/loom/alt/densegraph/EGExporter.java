package loom.alt.densegraph;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.html.HtmlEscapers;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Factory;
import guru.nidi.graphviz.model.Graph;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import loom.common.JsonUtil;
import loom.common.collections.EntryPair;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@SuperBuilder
public class EGExporter {

  @Builder.Default private final Rank.RankDir rankDir = Rank.RankDir.RIGHT_TO_LEFT;

  @Builder.Default private final int minPrefixLength = 2;

  public BufferedImage toImage(ExprGraph eg) {
    return Graphviz.fromGraph(toGraph(eg)).render(Format.PNG).toImage();
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
   * @param eg The graph to assign symbols to.
   * @param minPrefixLength The minimum length of the prefix to try.
   * @return A map from node UUIDs to symbols.
   */
  private static Map<UUID, String> nodeSymbols(ExprGraph eg, int minPrefixLength) {
    Map<UUID, String> longSyms = new HashMap<>();
    for (var tnode : eg) {
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

  public Graph toGraph(ExprGraph eg) {
    Graph g =
        Factory.graph("G")
            .directed()
            .graphAttr()
            .with(Rank.dir(rankDir))
            .graphAttr()
            .with("nodesep", "0.7")
            .nodeAttr()
            .with("margin", "0.1");

    var nodeSyms = nodeSymbols(eg, minPrefixLength);

    for (var node : eg) {
      var sym = nodeSyms.get(node.id);
      var gnode =
          Factory.node(node.id.toString()).with("xlabel", "#" + sym).with("style", "filled");

      String title = node.jsonTypeName();

      List<Object> rows = new ArrayList<>();
      rows.add(title);

      if (node instanceof EGTensor tensor) {
        gnode = gnode.with(Shape.BOX_3D).with("fillcolor", "#d0d0ff");
        rows.add(EntryPair.of("shape", tensor.getShape()));
        rows.add(EntryPair.of("dtype", tensor.getDtype()));

      } else if (node instanceof EGOperation op) {
        gnode = gnode.with(Shape.R_ARROW).with("margin", "0.15").with("fillcolor", "#75DDDD");

        var meta = op.getSignature(eg);

        rows.add(EntryPair.of("op", meta.getOp().name()));

        if (meta.isExternal()) {
          gnode = gnode.with("color", "red").with("penwidth", "6");
        }

      } else if (node instanceof EGOpSignature signature) {
        gnode = gnode.with(Shape.COMPONENT).with("margin", "0.05").with("fillcolor", "#FDCEDF");

        rows.add(EntryPair.of("op", signature.getOp().toString()));
        rows.add(EntryPair.of("external", signature.isExternal()));
      }

      String label =
          "<table border=\"0\" cellborder=\"0\" cellspacing=\"0\">"
              + formatRecursiveDataRows(rows)
              + "</table>";

      gnode = gnode.with(Label.raw("<" + label + ">"));

      g = g.with(gnode);

      for (var attr : node.getAttributes().entrySet()) {
        String attrLabel;
        var tree = JsonUtil.readTree(attr.getValue());
        if (tree instanceof ObjectNode objectNode) {
          var attrData = JsonUtil.toMap(objectNode);
          attrLabel =
                  "<table border=\"0\" cellborder=\"0\" cellspacing=\"0\">"
                          + formatRecursiveDataRows(attrData)
                          + "</table>";

        } else {
            attrLabel = HtmlEscapers.htmlEscaper().escape(JsonUtil.reformat(attr.getValue()));
        }


        var attrNode = Factory.node(UUID.randomUUID().toString())
                .with("shape", "component")
                .with("style", "filled")
                .with("fillcolor", "#FDCEDF")
                        .with(Label.raw("<" + attrLabel + ">"));

        g = g.with(gnode.link(Factory.to(attrNode).with(Label.of(attr.getKey().toString()))));
      }

      if (node instanceof EGOperation op) {
        g =
            g.with(
                gnode.link(
                    Factory.to(Factory.node(op.getSignature().toString())).with(Style.DOTTED)));

        for (var input : op.getInputs().entrySet()) {
          var edgeLabel = String.format("\"%s\"", input.getKey());
          var tensors = input.getValue();
          if (tensors.size() == 1) {
            g =
                g.with(
                    gnode.link(
                        Factory.to(Factory.node(tensors.get(0).toString()))
                            .with(Label.of(edgeLabel))));
          } else
            for (int i = 0; i < tensors.size(); ++i) {
              g =
                  g.with(
                      gnode.link(
                          Factory.to(Factory.node(tensors.get(i).toString()))
                              .with(Label.of(String.format("%s[%d]", edgeLabel, i)))));
            }
        }

        for (var result : op.getResults().entrySet()) {
          var edgeLabel = String.format("\"%s\"", result.getKey());
          var tensors = result.getValue();
          if (tensors.size() == 1) {
            g =
                g.with(
                    Factory.node(tensors.get(0).toString())
                        .link(Factory.to(gnode).with(Label.of(edgeLabel))));

          } else
            for (int i = 0; i < tensors.size(); ++i) {
              g =
                  g.with(
                      Factory.node(tensors.get(i).toString())
                          .link(
                              Factory.to(gnode)
                                  .with(Label.of(String.format("%s[%d]", edgeLabel, i)))));
            }
        }
      }
    }

    return g;
  }

  private String formatRecursiveData(Object data, boolean quoteStrings) {
    if (data instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> m = (Map<String, Object>) data;

      if (((Map<?, ?>) data).isEmpty()) {
        return "";
      } else {
        return "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\">"
            + formatRecursiveDataRows(m)
            + "</table>";
      }
    } else {
      String field;
      if (quoteStrings && data instanceof String) {
        field = String.format("\"%s\"", data);
      } else {
        field = data.toString();
      }
      return HtmlEscapers.htmlEscaper().escape(field);
    }
  }

  private String formatRecursiveDataRows(Collection<?> rows) {
    StringBuilder sb = new StringBuilder();
    for (var row : rows) {
      if (row instanceof Map.Entry<?, ?> entry) {
        sb.append("<tr>")
            .append("<td align=\"right\"><b>")
            .append(entry.getKey())
            .append(":</b></td>")
            .append("<td align=\"left\">")
            .append(formatRecursiveData(entry.getValue(), true))
            .append("</td>")
            .append("</tr>");
      } else {
        sb.append("<tr><td colspan=\"2\">")
            .append(formatRecursiveData(row, false))
            .append("</td></tr>");
      }
    }
    return sb.toString();
  }

  private String formatRecursiveDataRows(Map<String, Object> data) {
    return formatRecursiveDataRows(data.entrySet());
  }
}
