package loom.graph.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Streams;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.GraphvizCmdLineEngine;
import guru.nidi.graphviz.model.Factory;
import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

@Data
@Builder
public class GraphExporter {
  static {
    // TODO: Make this configurable.
    // TODO: how do I shut up the INFO level logging on this?
    Graphviz.useEngine(new GraphvizCmdLineEngine());
  }

  @Builder.Default private final int minLabelLen = 2;
  @Builder.Default private final Rank.RankDir rankDir = Rank.RankDir.LEFT_TO_RIGHT;

  @Builder.Default private double graphScale = 2.0;

  public static GraphExporter buildDefault() {
    var exporter = builder().build();
    exporter.setDefaultNodeTypeExporter(new DefaultNodeExporter());
    exporter.getNodeTypeExporters().put(TensorNode.TYPE, new TensorNodeExporter());
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

      var labelTable =
          GH.table()
              .border(0)
              .cellborder(0)
              .cellspacing(0)
              .tr(
                  GH.td()
                      .colspan(2)
                      .add(GH.font().color("teal").add(GH.bold(
                                graphExporter.typeAlias(node.getType())))));

      if (node.getLabel() != null) {
        labelTable.tr(
            GH.td()
                .colspan(2)
                .add(GH.font().attr("color", "green").add(GH.bold(node.getLabel()))));
      }

      labelTable.addXml(export.toRows((ObjectNode) node.getBodyAsJsonNode()));
      labelTable.addAll(export.renderAnnotationRows(node));

      gvizNode.add(Label.html(labelTable.toString()));

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

  public String typeAlias(String type) {
    // TODO: something real.
    return "loom:" + type;
  }

  public GH.ElementWrapper<?> dataRow(Object key, Object... values) {
    return GH.tr(
        GH.td().align(GH.TableDataAlign.RIGHT).valign(GH.VerticalAlign.TOP).add(GH.bold(key)),
        GH.td().align(GH.TableDataAlign.LEFT).valign(GH.VerticalAlign.TOP).add(values));
  }

  public static class TensorNodeExporter implements NodeTypeExporter {
    @Override
    public void exportNode(
        GraphExporter graphExporter, Export export, LoomNode<?, ?> node, MutableNode gvizNode) {
      var tensorNode = (TensorNode) node;
      gvizNode.add(Shape.BOX_3D);
      gvizNode.add("style", "filled");
      gvizNode.add("fillcolor", "lightblue");

      var labelTable =
          GH.table()
              .border(0)
              .cellborder(0)
              .cellspacing(0)
              .tr(
                  GH.td()
                      .colspan(2)
                      .add(GH.font().color("teal").add(GH.bold(graphExporter.typeAlias(node.getType())))));

      if (node.getLabel() != null) {
        labelTable.tr(
            GH.td()
                .colspan(2)
                .add(GH.font().attr("color", "green").add(GH.bold(node.getLabel()))));
      }

      labelTable.add(
              graphExporter.dataRow("dtype", tensorNode.getDtype()),
          graphExporter.dataRow("shape", tensorNode.getShape().toString()),
          graphExporter.dataRow("range", tensorNode.getRange().toRangeString()));

      labelTable.addAll(export.renderAnnotationRows(node));

      gvizNode.add(Label.html(labelTable.toString()));
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
    @Nonnull private final LoomGraph graph;

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
      // exportGraph.graphAttrs().add("forcelabels", true);

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

    public List<GH.ElementWrapper<?>> objectNodeToRows(ObjectNode node) {
      List<GH.ElementWrapper<?>> rows = new ArrayList<>();
      for (var it = node.fields(); it.hasNext(); ) {
        var entry = it.next();
        rows.add(
            GH.tr(
                GH.td()
                    .align(GH.TableDataAlign.RIGHT)
                    .valign(GH.VerticalAlign.TOP)
                    .add(GH.bold(entry.getKey())),
                GH.td()
                    .align(GH.TableDataAlign.LEFT)
                    .valign(GH.VerticalAlign.TOP)
                    .addXml(jsonToCell(entry.getValue()))));
      }
      return rows;
    }

    public List<GH.ElementWrapper<?>> renderAnnotationRows(LoomNode<?, ?> loomNode) {
      List<GH.ElementWrapper<?>> rows = new ArrayList<>();
      for (var annEntry :
          loomNode.getAnnotations().entrySet().stream()
              .sorted(Map.Entry.comparingByKey())
              .toList()) {
        var key = annEntry.getKey();
        var value = annEntry.getValue();

        rows.add(GH.tr(GH.td().colspan(2).add(GH.font().color("teal").add(GH.bold(typeAlias(key))))));
        rows.addAll(objectNodeToRows((ObjectNode) JsonUtil.convertValue(value, JsonNode.class)));
      }
      return rows;
    }

    @Getter(lazy = true)
    private final Graphviz graphviz = renderGraphviz();

    private Graphviz renderGraphviz() {
      return Graphviz.fromGraph(exportGraph).scale(graphScale);
    }

    public String jsonToTable(ObjectNode node) {
      return "<table border=\"0\" cellborder=\"0\" cellspacing=\"0\">" + toRows(node) + "</table>";
    }

    public String renderKeyCell(String text) {
      return "<td valign=\"top\"><b>%s</b></td>".formatted(text);
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
              return "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\">"
                  + toRows(array)
                  + "</table>";
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

    public String formattedNodeLabel(UUID id) {
      return "<font color=\"teal\">%s</font>".formatted(nodeLabel(id));
    }

    public String formattedNodeLabel(LoomNode<?, ?> node) {
      return formattedNodeLabel(node.getId());
    }
  }

  public Export export(LoomGraph graph) {
    var e = new Export(graph);
    e.export();
    return e;
  }
}
