package loom.graph.export.graphviz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import loom.common.json.JsonUtil;
import loom.graph.LoomGraph;
import loom.graph.LoomNode;
import loom.graph.nodes.ExportUtils;
import loom.graph.nodes.TensorNode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

@Data
@Builder
public class GraphVisualizer {
  static {
    // TODO: Make this configurable.
    // TODO: how do I shut up the INFO level logging on this?
    Graphviz.useEngine(new GraphvizCmdLineEngine());
  }

  @Builder.Default private final int minLabelLen = 4;
  @Builder.Default private final Rank.RankDir rankDir = Rank.RankDir.LEFT_TO_RIGHT;

  @Builder.Default private double graphScale = 2.5;

  @Builder.Default private Map<String, NodeTypeExporter> nodeTypeExporters = new HashMap<>();
  @Builder.Default private NodeTypeExporter defaultNodeTypeExporter = new DefaultNodeExporter();

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

  public static GraphVisualizer buildDefault() {
    var exporter = builder().build();
    exporter.getNodeTypeExporters().put(TensorNode.TYPE, new TensorNodeExporter());
    return exporter;
  }

  public interface NodeTypeExporter {
    void exportNode(
        GraphVisualizer visualizer,
        ExportContext context,
        LoomNode<?, ?> node,
        MutableNode gvizNode);
  }

  public static class DefaultNodeExporter implements NodeTypeExporter {
    @Override
    public void exportNode(
        GraphVisualizer visualizer,
        ExportContext context,
        LoomNode<?, ?> node,
        MutableNode gvizNode) {

      gvizNode.add(Shape.RECTANGLE);

      var labelTable = GH.table().border(0).cellborder(0).cellspacing(2).cellpadding(2);

      labelTable.add(
          context.renderDataTable(visualizer.typeAlias(node.getType()), node.getBodyAsJsonNode()));
      labelTable.addAll(context.renderAnnotationTables(node));

      gvizNode.add(Label.html(labelTable.toString()));

      List<Link> links = new ArrayList<>();
      var body = (ObjectNode) node.getBodyAsJsonNode();
      var graph = context.getGraph();
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

  public static class TensorNodeExporter implements NodeTypeExporter {
    @Override
    public void exportNode(
        GraphVisualizer visualizer,
        ExportContext context,
        LoomNode<?, ?> node,
        MutableNode gvizNode) {
      var tensorNode = (TensorNode) node;
      gvizNode.add(Shape.BOX_3D);
      gvizNode.add("style", "filled");
      gvizNode.add("fillcolor", "lightblue");

      var labelTable =
          GH.table()
              .border(0)
              .cellborder(0)
              .cellspacing(0)
              .tr(context.renderDataTypeTitle(visualizer.typeAlias(node.getType())))
              .add(
                  context.dataRow("dtype", tensorNode.getDtype()),
                  context.dataRow("shape", tensorNode.getShape().toString()),
                  context.dataRow("range", tensorNode.getRange().toRangeString()))
              .addAll(context.renderAnnotationTables(node));

      gvizNode.add(Label.html(labelTable.toString()));
    }
  }

  @Data
  public class ExportContext {
    @Nonnull private final LoomGraph graph;

    public ExportContext(@Nonnull LoomGraph graph) {
      this.graph = Objects.requireNonNull(graph);
    }

    @Getter(lazy = true)
    private final Map<UUID, String> nodeHexAliasMap = renderNodeHexAliasMap();

    @Getter private MutableGraph exportGraph = null;

    @Getter(lazy = true)
    private final Graphviz graphviz = renderGraphviz();

    private Map<UUID, String> renderNodeHexAliasMap() {
      return AliasUtils.uuidAliasMap(
          getGraph().nodeScan().asStream().map(LoomNode::getId).toList(), minLabelLen);
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

      for (var nodeIt = graph.nodeScan().asStream().iterator(); nodeIt.hasNext(); ) {
        var loomNode = nodeIt.next();
        exportNode(loomNode);
      }
    }

    private void exportNode(LoomNode<?, ?> node) {
      var gvnode = Factory.mutNode(node.getId().toString());

      {
        var xlabelTable = GH.table().border(0).cellborder(0).cellspacing(0).cellpadding(0);
        xlabelTable.tr(GH.td().add(nodeLabelElement(node.getId())));
        if (node.getLabel() != null) {
          xlabelTable.tr(
              GH.td()
                  .add(GH.font().color("green").add(GH.bold("\"%s\"".formatted(node.getLabel())))));
        }
        gvnode.add("xlabel", Label.html(xlabelTable.toString()));
      }

      exportGraph.add(gvnode);
      exporterForNodeType(node.getType())
          .exportNode(GraphVisualizer.this, ExportContext.this, node, gvnode);
    }

    public List<GH.TableWrapper> renderAnnotationTables(LoomNode<?, ?> loomNode) {
      List<GH.TableWrapper> tables = new ArrayList<>();
      loomNode.getAnnotations().entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(
              entry ->
                  tables.add(
                      renderDataTable(
                          typeAlias(entry.getKey()),
                          (ObjectNode) JsonUtil.convertValue(entry.getValue(), JsonNode.class))));
      return tables;
    }

    public GH.TableDataWrapper renderDataTypeTitle(String title) {
      return GH.td().colspan(2).add(GH.font().color("teal").add(GH.bold(title)));
    }

    public List<GH.ElementWrapper<?>> renderDataType(String title, ObjectNode node) {
      List<GH.ElementWrapper<?>> rows = new ArrayList<>();
      rows.add(renderDataTypeTitle(title));
      rows.addAll(jsonObjectToRows(node));
      return rows;
    }

    public GH.TableWrapper renderDataTable(String title, ObjectNode node) {
      var table = GH.table().border(0).cellborder(1).cellspacing(0).cellpadding(0);
      table.addAll(renderDataType(title, node));
      return table;
    }

    private Graphviz renderGraphviz() {
      return Graphviz.fromGraph(exportGraph).scale(graphScale);
    }

    public GH.TableDataWrapper keyCell(String key) {
      return GH.td()
          .align(GH.TableDataAlign.RIGHT)
          .valign(GH.VerticalAlign.TOP)
          .add(GH.bold(" %s ".formatted(key)));
    }

    public GH.TableDataWrapper valueCell(Object... value) {
      return GH.td()
          .align(GH.TableDataAlign.LEFT)
          .valign(GH.VerticalAlign.TOP)
          .add(" ")
          .add(value)
          .add(" ");
    }

    public GH.ElementWrapper<?> dataRow(Object key, Object... values) {
      return GH.tr(keyCell(key.toString()), valueCell(values));
    }

    public List<GH.ElementWrapper<?>> jsonObjectToRows(ObjectNode node) {
      List<GH.ElementWrapper<?>> rows = new ArrayList<>();
      for (var it = node.fields(); it.hasNext(); ) {
        var entry = it.next();
        rows.add(GH.tr(keyCell(entry.getKey()), valueCell(jsonToElement(entry.getValue()))));
      }
      return rows;
    }

    public GH.ElementWrapper<?> jsonToElement(JsonNode node) {
      switch (node) {
        case ArrayNode array -> {
          if (JsonUtil.Tree.isAllNumeric(array)) {
            return GH.font().color("blue").add(GH.bold(JsonUtil.toJson(array)));
          } else {
            return GH.table()
                .border(0)
                .cellborder(0)
                .cellspacing(2)
                .addAll(
                    JsonUtil.Tree.stream(array).map(item -> GH.tr(valueCell(jsonToElement(item)))));
          }
        }
        case ObjectNode object -> {
          return GH.table()
              .border(0)
              .cellborder(1)
              .cellspacing(0)
              .addAll(
                  JsonUtil.Tree.stream(object)
                      .map(
                          entry ->
                              GH.tr(
                                  keyCell(entry.getKey()),
                                  valueCell(jsonToElement(entry.getValue())))));
        }
        default -> {
          var text = node.asText();
          var targetNode = maybeNode(text);
          if (targetNode != null) {
            return nodeLabelElement(targetNode.getId());
          } else {
            return GH.bold(text);
          }
        }
      }
    }

    private static final List<String> NODE_LABEL_HEX_COLORS =
        List.of("magenta", "red", "teal", "green");

    public GH.ElementWrapper<?> nodeLabelElement(UUID id) {
      var group = GH.bold();
      group.add(GH.font().color("blue").add("{#"));

      String alias = nodeHexAlias(id);
      for (int idx = 0; idx < alias.length(); idx++) {
        char c = alias.charAt(idx);
        String digit = String.valueOf(c);
        var f = GH.font().withParent(group).add(digit.toUpperCase(Locale.ROOT));
        try {
          int val = Integer.parseInt(digit, 16);
          f.color(NODE_LABEL_HEX_COLORS.get(val % NODE_LABEL_HEX_COLORS.size()));
        } catch (NumberFormatException e) {
          // ignore
        }
      }

      group.add(GH.font().color("blue").add("}"));
      return group;
    }

    public String nodeHexAlias(UUID id) {
      return getNodeHexAliasMap().get(id);
    }
  }

  public ExportContext export(LoomGraph graph) {
    var e = new ExportContext(graph);
    e.export();
    return e;
  }
}
