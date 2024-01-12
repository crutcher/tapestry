package loom.graph.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Streams;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Factory;
import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import java.util.*;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import loom.common.DigestUtils;
import loom.common.json.JsonUtil;
import loom.common.text.TextUtils;
import loom.graph.LoomGraph;
import loom.graph.LoomNode;
import loom.graphviz.GH;

@Data
@Builder
public class GraphExporter {
  @Builder.Default private final int minLabelLen = 2;
  @Builder.Default private final Rank.RankDir rankDir = Rank.RankDir.LEFT_TO_RIGHT;

  @Builder.Default private double graphScale = 2.0;

  public static GraphExporter buildDefault() {
    var exporter = builder().build();
    exporter.setDefaultNodeTypeExporter(new DefaultNodeExporter());
    exporter.getNodeTypeExporters().put(TensorNode.TYPE, new TensorNodeExporter());
    // exporter.getNodeTypeExporters().put(OperationSignatureNode.TYPE, new
    // OperationSignatureNodeExporter());
    // exporter.getNodeTypeExporters().put(ApplicationNode.TYPE, new ApplicationNodeExporter());
    return exporter;
  }

  public interface NodeTypeExporter {
    void exportNode(
        GraphExporter graphExporter, Export export, LoomNode<?, ?> node, MutableNode gvizNode);
  }

  public static class DefaultNodeExporter implements NodeTypeExporter {
    @Override
    public void exportNode(
        GraphExporter graphExporter, Export export, LoomNode<?, ?> node, MutableNode gvizNode) {

      gvizNode.add(Shape.RECTANGLE);

      var table =
          GH.table()
              .border(0)
              .cellborder(0)
              .cellspacing(0)
              .tr(
                  GH.td()
                      .colspan(2)
                      .add(GH.font().color("teal").add(GH.bold(GH.text(node.getType())))));

      if (node.getLabel() != null) {
        table.tr(
            GH.td()
                .colspan(2)
                .add(GH.font().attr("color", "green").add(GH.bold(GH.text(node.getLabel())))));
      }

      table.parseAndAdd(export.toRows((ObjectNode) node.getBodyAsJsonNode()));
      table.parseAndAdd(export.renderAnnotations(node));

      gvizNode.add(Label.html(table.toString()));

      List<Link> links = new ArrayList<>();
      var body = (ObjectNode) node.getBodyAsJsonNode();
      var graph = export.getGraph();
      ExportUtils.findLinks(
          body,
          graph::hasNode,
          (path, id) -> {
            // Remove "$." prefix.
            path = path.substring(2);
            Link e = Link.to(Factory.node(id.toString())).with(Label.of(path));
            links.add(e);
          });

      for (var link : links) {
        gvizNode.addLink(link);
      }
    }
  }

  public static class TensorNodeExporter implements NodeTypeExporter {
    @Override
    public void exportNode(
        GraphExporter graphExporter, Export export, LoomNode<?, ?> node, MutableNode gvizNode) {
      var tensorNode = (TensorNode) node;
      gvizNode.add(Shape.BOX_3D);
      gvizNode.add("style", "filled");
      gvizNode.add("fillcolor", "lightblue");

      StringBuilder label = new StringBuilder();
      label.append("<table border=\"0\" cellborder=\"0\" cellspacing=\"0\">");
      label.append(export.renderSpanningHeader(export.formatNodeType(node)));
      if (node.getLabel() != null) {
        label.append(
            export.renderSpanningHeader(
                "<font color=\"green\"><b>\"%s\"</b></font>".formatted(node.getLabel())));
      }
      label.append(export.renderTableRow("dtype", tensorNode.getDtype()));
      label.append(export.renderTableRow("range", tensorNode.getRange().toRangeString()));

      label.append(export.renderAnnotations(node));

      label.append("</table>");

      gvizNode.add(Label.html(label.toString()));
    }
  }

  public static class ApplicationNodeExporter implements NodeTypeExporter {
    @Override
    public void exportNode(
        GraphExporter graphExporter, Export export, LoomNode<?, ?> node, MutableNode gvizNode) {
      // pass
    }
  }

  public static class OperationSignatureNodeExporter implements NodeTypeExporter {
    @Override
    public void exportNode(
        GraphExporter graphExporter, Export export, LoomNode<?, ?> node, MutableNode gvizNode) {
      var opSig = (OperationSignatureNode) node;
      gvizNode.add(Shape.BOX_3D);

      StringBuilder label = new StringBuilder();
      label.append("<table border=\"0\" cellborder=\"0\" cellspacing=\"0\">");
      label.append(export.renderSpanningHeader(export.formatNodeType(node)));
      if (node.getLabel() != null) {
        label.append(
            export.renderSpanningHeader(
                "<font color=\"green\"><b>\"%s\"</b></font>".formatted(node.getLabel())));
      }
      label.append(export.toRows((ObjectNode) node.getBodyAsJsonNode()));

      label.append(export.renderAnnotations(node));

      label.append("</table>");

      gvizNode.add(Label.html(label.toString()));

      for (var entry : opSig.getInputs().entrySet()) {
        var name = entry.getKey();
        var selections = entry.getValue();

        for (int idx = 0; idx < selections.size(); ++idx) {
          var selection = selections.get(idx);
          var target = selection.getTensorId();
          var linkLabel =
              "<table border=\"0\" cellborder=\"0\"><tr><td>inputs.%s[%d]</td></tr><tr><td>%s</td></tr></table>"
                  .formatted(name, idx, selection.getRange().toRangeString());

          Link link = Link.to(Factory.node(target.toString())).with(Label.html(linkLabel));
          gvizNode.addLink(link);
        }
      }
      for (var entry : opSig.getOutputs().entrySet()) {
        var name = entry.getKey();
        var selections = entry.getValue();

        for (int idx = 0; idx < selections.size(); ++idx) {
          var selection = selections.get(idx);
          var target = selection.getTensorId();
          var linkLabel =
              "<table border=\"0\" cellborder=\"0\"><tr><td>outputs.%s[%d]</td></tr><tr><td>%s</td></tr></table>"
                  .formatted(name, idx, selection.getRange().toRangeString());

          // TODO: Add a reverse link.
          Link link = Link.to(Factory.node(target.toString())).with(Label.html(linkLabel));
          gvizNode.addLink(link);
        }
      }
    }
  }

  @Builder.Default private Map<String, NodeTypeExporter> nodeTypeExporters = new HashMap<>();
  @Builder.Default private NodeTypeExporter defaultNodeTypeExporter = null;

  public NodeTypeExporter exporterForNodeType(String type) {
    var exp = nodeTypeExporters.get(type);
    if (exp == null) {
      exp = defaultNodeTypeExporter;
    }
    if (exp == null) {
      throw new IllegalStateException("No exporter for type: " + type);
    }
    return exp;
  }

  @Data
  @RequiredArgsConstructor
  public class Export {
    private final LoomGraph graph;

    @Getter(lazy = true)
    private final Map<UUID, String> nodeLabels = renderNodeLabels();

    @Getter private MutableGraph exportGraph = null;

    private Map<UUID, String> renderNodeLabels() {
      var ids = graph.nodeScan().asStream().map(LoomNode::getId).toList();

      var idHashes = ids.stream().map(id -> DigestUtils.toMD5HexString(id.toString())).toList();

      var labelLen = Math.max(TextUtils.longestCommonPrefix(idHashes).length() + 1, minLabelLen);

      var labels = new HashMap<UUID, String>();
      Streams.forEachPair(
          ids.stream(),
          idHashes.stream(),
          (id, hash) -> labels.put(id, "#" + hash.substring(0, labelLen)));

      return Collections.unmodifiableMap(labels);
    }

    @Nullable private LoomNode<?, ?> maybeNode(UUID id) {
      return graph.getNode(id);
    }

    @Nullable private LoomNode<?, ?> maybeNode(String idString) {
      try {
        var id = UUID.fromString(idString);
        return maybeNode(id);
      } catch (IllegalArgumentException e) {
        return null;
      }
    }

    private void export() {
      exportGraph = Factory.mutGraph("graph");
      exportGraph.setDirected(true);
      exportGraph.graphAttrs().add(Rank.dir(rankDir));

      // Force support for "xlabel":
      exportGraph.graphAttrs().add("forcelabels", true);

      for (var nodeIt = graph.nodeScan().asStream().iterator(); nodeIt.hasNext(); ) {
        var loomNode = nodeIt.next();

        exportNode(loomNode);
      }
    }

    private void exportNode(LoomNode<?, ?> node) {
      var gvnode = Factory.mutNode(node.getId().toString());
      gvnode.add("xlabel", Label.html(formattedNodeLabel(node)));
      exportGraph.add(gvnode);
      exporterForNodeType(node.getType()).exportNode(GraphExporter.this, Export.this, node, gvnode);
    }

    public String renderAnnotations(LoomNode<?, ?> loomNode) {
      StringBuilder sb = new StringBuilder();
      for (var annEntry : loomNode.getAnnotations().entrySet()) {
        var key = annEntry.getKey();
        var value = annEntry.getValue();
        sb.append(exportAnnotation(key, value));
      }
      return sb.toString();
    }

    public String exportAnnotation(String key, Object value) {
      StringBuilder rows = new StringBuilder();
      rows.append("<tr><td colspan=\"2\"><b>%s</b></td></tr>".formatted(formatAnnotationType(key)));
      rows.append(toRows((ObjectNode) JsonUtil.convertValue(value, JsonNode.class)));
      return rows.toString();
    }

    @Getter(lazy = true)
    private final Graphviz graphviz = renderGraphviz();

    private Graphviz renderGraphviz() {
      return Graphviz.fromGraph(exportGraph).scale(graphScale);
    }

    public String jsonToTable(ObjectNode node) {
      var table = new StringBuilder();
      table.append("<table border=\"0\" cellborder=\"0\" cellspacing=\"0\">");
      table.append(toRows(node));
      table.append("</table>");
      return table.toString();
    }

    public String renderKeyCell(String text) {
      return "<td valign=\"top\"><b>%s</b></td>".formatted(text);
    }

    public String renderSpanningHeader(String text) {
      return "<tr><td colspan=\"2\"><b>%s</b></td></tr>".formatted(text);
    }

    public String renderTableRow(String key, String value) {
      return "<tr>%s<td>%s</td></tr>".formatted(renderKeyCell(key), value);
    }

    public String toRows(ObjectNode node) {
      var table = new StringBuilder();
      for (var it = node.fields(); it.hasNext(); ) {
        var entry = it.next();
        table.append(
            "<tr>%s<td>%s</td></tr>"
                .formatted(renderKeyCell(entry.getKey()), jsonToCell(entry.getValue())));
      }
      return table.toString();
    }

    public String toRows(ArrayNode node) {
      var table = new StringBuilder();
      for (var it = node.elements(); it.hasNext(); ) {
        var entry = it.next();
        table.append("<tr><td>%s</td></tr>".formatted(jsonToCell(entry)));
      }
      return table.toString();
    }

    public static boolean isAllNumeric(ArrayNode array) {
      for (var it = array.elements(); it.hasNext(); ) {
        var node = it.next();
        if (!node.isNumber()) {
          return false;
        }
      }
      return true;
    }

    public String jsonToCell(JsonNode node) {
      switch (node.getNodeType()) {
        case OBJECT:
          return jsonToTable((ObjectNode) node);

        case ARRAY:
          {
            var array = (ArrayNode) node;
            if (isAllNumeric(array)) {
              return JsonUtil.toJson(array);
            } else {
              var table = new StringBuilder();
              table.append("<table border=\"0\" cellborder=\"1\" cellspacing=\"0\">");
              table.append(toRows(array));
              table.append("</table>");
              return table.toString();
            }
          }

        default:
          var text = node.asText();
          var targetNode = maybeNode(text);
          if (targetNode != null) {
            return formattedNodeLabel(targetNode);
          }
          return text;
      }
    }

    public String nodeLabel(UUID id) {
      return getNodeLabels().get(id);
    }

    public String nodeLabel(LoomNode<?, ?> node) {
      return nodeLabel(node.getId());
    }

    public String formattedNodeLabel(UUID id) {
      return "<font color=\"teal\">%s</font>".formatted(nodeLabel(id));
    }

    public String formattedNodeLabel(LoomNode<?, ?> node) {
      return formattedNodeLabel(node.getId());
    }

    public String formatNodeType(LoomNode<?, ?> node) {
      return "<font color=\"teal\">%s</font>".formatted(node.getType());
    }

    public String formatAnnotationType(String type) {
      return "<font color=\"teal\">%s</font>".formatted(type);
    }
  }

  public Export export(LoomGraph graph) {
    var e = new Export(graph);
    e.export();
    return e;
  }
}
