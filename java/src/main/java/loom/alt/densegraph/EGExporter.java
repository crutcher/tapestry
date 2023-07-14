package loom.alt.densegraph;

import com.google.common.html.HtmlEscapers;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Factory;
import guru.nidi.graphviz.model.Graph;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.experimental.SuperBuilder;

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

      String title = node.jsonTypeName();

      Map<String, Object> data = new HashMap<>();

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
          Factory.node(node.id.toString())
              .with(Label.raw("<" + label + ">"))
              .with("xlabel", "#" + sym);

      g = g.with(gnode);

      if (node instanceof EGOperation op) {
        g = g.with(gnode.link(Factory.node(op.getSignature().toString())));

        for (var input : op.getInputs().entrySet()) {
          for (var inputId : input.getValue()) {
            g = g.with(gnode.link(Factory.node(inputId.toString())));
          }
        }

        for (var result : op.getResults().entrySet()) {
          for (var resultId : result.getValue()) {
            g = g.with(Factory.node(resultId.toString()).link(gnode));
          }
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
