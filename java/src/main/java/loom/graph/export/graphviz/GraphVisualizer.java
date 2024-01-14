package loom.graph.export.graphviz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import guru.nidi.graphviz.attribute.*;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.GraphvizCmdLineEngine;
import guru.nidi.graphviz.model.*;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import loom.common.json.JsonUtil;
import loom.graph.LoomGraph;
import loom.graph.LoomNode;
import loom.graph.nodes.*;
import loom.polyhedral.IndexProjectionFunction;
import loom.zspace.ZRange;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

@Data
@Builder
public class GraphVisualizer {
  static {
    // TODO: Make this configurable.
    // TODO: how do I shut up the INFO level logging on this?
    Graphviz.useEngine(new GraphvizCmdLineEngine());
  }

  @Builder.Default private final int minLabelLen = 6;
  @Builder.Default private final Rank.RankDir rankDir = Rank.RankDir.TOP_TO_BOTTOM;

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
    exporter.getNodeTypeExporters().put(ApplicationNode.TYPE, new ApplicationNodeExporter());
    exporter.getNodeTypeExporters().put(OperationSignatureNode.TYPE, new OperationNodeExporter());
    return exporter;
  }

  public interface NodeTypeExporter {
    void exportNode(
        GraphVisualizer visualizer,
        ExportContext context,
        LoomNode<?, ?> loomNode,
        MutableNode gvNode);
  }

  public static class DefaultNodeExporter implements NodeTypeExporter {
    @Override
    public void exportNode(
        GraphVisualizer visualizer,
        ExportContext context,
        LoomNode<?, ?> loomNode,
        MutableNode gvNode) {

      gvNode.add(Shape.RECTANGLE);
      gvNode.add(Style.FILLED);
      gvNode.add(Color.named("gray").fill());

      gvNode.add(
          asHtmlLabel(
              GH.table()
                  .border(0)
                  .cellborder(0)
                  .cellspacing(2)
                  .cellpadding(2)
                  .bgcolor("white")
                  .add(
                      context.renderDataTable(
                          loomNode.getTypeAlias(), loomNode.getBodyAsJsonNode()))));

      ExportUtils.findLinks(
          loomNode.getBodyAsJsonNode(),
          context.getGraph()::hasNode,
          (path, targetId) -> {
            // Remove "$." prefix.
            var p = path.substring(2);

            context.addLink(loomNode.getId(), targetId, link -> link.with(Label.of(p)));
          });
    }
  }

  public static class TensorNodeExporter implements NodeTypeExporter {
    @Override
    public void exportNode(
        GraphVisualizer visualizer,
        ExportContext context,
        LoomNode<?, ?> loomNode,
        MutableNode gvNode) {
      var tensorNode = (TensorNode) loomNode;

      gvNode.add(Shape.BOX_3D);
      gvNode.add(Style.FILLED);
      gvNode.add(Color.rgb("#74CFFF").fill());

      gvNode.add(
          asHtmlLabel(
              GH.table()
                  .border(0)
                  .cellborder(0)
                  .cellspacing(0)
                  .tr(context.renderDataTypeTitle(loomNode.getTypeAlias()))
                  .add(
                      context.asDataKeyValueTR("dtype", tensorNode.getDtype()),
                      context.asDataKeyValueTR("shape", tensorNode.getShape().toString()),
                      context.asDataKeyValueTR("range", tensorNode.getRange().toRangeString()))));
    }
  }

  public abstract static class TensorSelectionMapBaseExporter implements NodeTypeExporter {
    protected static void tensorSelectionMapEdges(
        ExportContext context,
        LoomNode<?, ?> node,
        Map<String, List<TensorSelection>> inputs,
        boolean isInput) {
      UUID nodeId = node.getId();

      for (var entry : inputs.entrySet()) {
        var key = entry.getKey();
        var slices = entry.getValue();
        for (int idx = 0; idx < slices.size(); idx++) {
          var slice = slices.get(idx);

          var label =
              asHtmlLabel(
                  GH.bold(
                      GH.table()
                          .border(0)
                          .cellborder(0)
                          .cellspacing(0)
                          .cellpadding(0)
                          .add("%s[%d]".formatted(key, idx))
                          .add(slice.getRange().toRangeString())));

          UUID tensorId = slice.getTensorId();

          String targetAlias = context.getNodeHexAliasMap().get(tensorId);

          if (isInput) {
            var link =
                Link.to(Factory.mutNode(nodeId.toString()).port(targetAlias).port(Compass.NORTH))
                    .with(label);

            context.exportGraph.add(Factory.mutNode(tensorId.toString()).addLink(link));

          } else {
            var source = Factory.mutNode(nodeId.toString());

            var link =
                source
                    .port(targetAlias)
                    .port(Compass.SOUTH)
                    .linkTo(Factory.mutNode(tensorId.toString()))
                    .with(label);

            source.addLink(link);

            source.addTo(context.exportGraph);
          }
        }
      }
    }
  }

  public static class ApplicationNodeExporter extends TensorSelectionMapBaseExporter {
    @Override
    public void exportNode(
        GraphVisualizer visualizer,
        ExportContext context,
        LoomNode<?, ?> loomNode,
        MutableNode gvNode) {
      var appNode = (ApplicationNode) loomNode;

      gvNode.add(Shape.TAB);
      gvNode.add(Style.FILLED);
      gvNode.add(Color.named("green").fill());

      gvNode.add(
          asHtmlLabel(
              GH.table()
                  .bgcolor("white")
                  .border(0)
                  .cellborder(0)
                  .cellspacing(0)
                  .add(
                      context.renderDataTable(
                          loomNode.getTypeAlias(), loomNode.getBodyAsJsonNode()))));

      {
        UUID operationId = appNode.getOperationId();
        String operatorAlias = context.getNodeHexAliasMap().get(operationId);
        var link =
            Link.to(
                    Factory.mutNode(loomNode.getId().toString())
                        .port(operatorAlias)
                        .port(Compass.NORTH))
                .with("dir", "both")
                .with(Style.DOTTED);
        context.exportGraph.add(Factory.mutNode(operationId.toString()).addLink(link));
      }

      tensorSelectionMapEdges(context, appNode, appNode.getInputs(), true);
      tensorSelectionMapEdges(context, appNode, appNode.getOutputs(), false);
    }
  }

  public static class OperationNodeExporter extends TensorSelectionMapBaseExporter {
    @Override
    public void exportNode(
        GraphVisualizer visualizer,
        ExportContext context,
        LoomNode<?, ?> loomNode,
        MutableNode gvNode) {
      gvNode.add(Shape.TAB);
      gvNode.add(Style.FILLED);
      gvNode.add(Color.named("blue").fill());

      gvNode.add(
          asHtmlLabel(
              GH.table()
                  .bgcolor("white")
                  .border(0)
                  .cellborder(0)
                  .cellspacing(0)
                  .add(
                      context.renderDataTable(
                          loomNode.getTypeAlias(), loomNode.getBodyAsJsonNode()))));

      // tensorSelectionMapEdges(context, opNode, opNode.getInputs(), true);
      // tensorSelectionMapEdges(context, opNode, opNode.getOutputs(), false);
    }
  }

  public static Label asHtmlLabel(Object obj) {
    return Label.html(obj.toString());
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

    /**
     * Constraint nodes to be on the same rank.
     *
     * @param nodeIds the ids of the nodes to constrain.
     */
    public void sameRank(Object... nodeIds) {
      var nodes = Arrays.stream(nodeIds).map(Object::toString).map(Factory::mutNode).toList();
      exportGraph.add(
          Factory.mutGraph()
              .setDirected(true)
              .graphAttrs()
              .add(Rank.inSubgraph(Rank.RankType.SAME))
              .add(nodes));
    }

    public void addLink(Object fromId, Object toId, Function<Link, Link> config) {
      var link = Link.to(Factory.mutNode(toId.toString()));
      link = config.apply(link);
      exportGraph.add(Factory.mutNode(fromId.toString()).addLink(link));
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
        var table =
            GH.table()
                .border(0)
                .cellborder(0)
                .cellspacing(0)
                .cellpadding(0)
                .add(nodeAliasTable(node.getId()));
        if (node.getLabel() != null) {
          table.add(GH.font().color("green").add(GH.bold("\"%s\"".formatted(node.getLabel()))));
        }
        gvnode.add(asHtmlLabel(table).external());
      }

      exportGraph.add(gvnode);

      exporterForNodeType(node.getType())
          .exportNode(GraphVisualizer.this, ExportContext.this, node, gvnode);

      if (!node.getAnnotations().isEmpty()) {
        String gvAnnotationNodeId = node.getId() + "#a";
        var aNode = Factory.mutNode(gvAnnotationNodeId);
        aNode.add(Shape.COMPONENT);
        aNode.add(Style.FILLED);
        aNode.add(Color.rgb("#45eebf").fill());

        var table = GH.table().border(0).cellborder(0).cellspacing(2).cellpadding(2);
        for (var t : renderAnnotationTables(node)) {
          t.bgcolor("white");
          table.add(t);
        }
        aNode.add(asHtmlLabel(table));

        exportGraph.add(aNode);

        addLink(
            node.getId(),
            gvAnnotationNodeId,
            link -> link.with(Arrow.DOT.open().and(Arrow.DOT.open())).with(Style.BOLD));

        sameRank(node.getId(), gvAnnotationNodeId);
      }
    }

    public List<GH.TableWrapper> renderAnnotationTables(LoomNode<?, ?> loomNode) {
      List<GH.TableWrapper> tables = new ArrayList<>();
      loomNode.getAnnotations().entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(entry -> tables.add(renderAnnotationTable(entry.getKey(), entry.getValue())));
      return tables;
    }

    public GH.TableWrapper renderAnnotationTable(String key, Object value) {
      var env = graph.getEnv();
      String typeAlias = env.getAnnotationTypeAlias(key);
      return renderDataTable(typeAlias, (ObjectNode) JsonUtil.convertValue(value, JsonNode.class));
    }

    public GH.TableDataWrapper renderDataTypeTitle(String title) {
      return GH.td()
          .colspan(2)
          .align(GH.TableDataAlign.LEFT)
          .add(GH.font().color("teal").add(GH.bold(" %s ".formatted(title))));
    }

    public GH.TableWrapper renderDataTable(String title, ObjectNode data) {
      return GH.table()
          .border(0)
          .cellborder(1)
          .cellspacing(0)
          .cellpadding(0)
          .add(renderDataTypeTitle(title))
          .addAll(jsonToDataKeyValueTRs(data));
    }

    private Graphviz renderGraphviz() {
      return Graphviz.fromGraph(exportGraph).scale(graphScale);
    }

    public GH.ElementWrapper<?> asDataKeyValueTR(Object key, Object... values) {
      return GH.tr(asDataKeyTD(key.toString()), asDataValueTD(values));
    }

    public GH.TableDataWrapper asDataKeyTD(String key) {
      return GH.td()
          .align(GH.TableDataAlign.RIGHT)
          .valign(GH.VerticalAlign.TOP)
          .add(GH.bold(" %s ".formatted(key)));
    }

    public GH.TableDataWrapper asDataValueTD(Object... value) {
      return GH.td()
          .align(GH.TableDataAlign.LEFT)
          .valign(GH.VerticalAlign.TOP)
          .add(GH.bold().add(" ").add(value).add(" "));
    }

    public List<GH.ElementWrapper<?>> jsonToDataKeyValueTRs(ObjectNode node) {
      List<GH.ElementWrapper<?>> rows = new ArrayList<>();
      node.fields()
          .forEachRemaining(
              entry -> rows.add(asDataKeyValueTR(entry.getKey(), jsonToElement(entry.getValue()))));
      return rows;
    }

    public GH.ElementWrapper<?> jsonToElement(JsonNode node) {

      if (node instanceof ObjectNode objectNode) {
        if (objectNode.has("affineMap")) {
          try {
            var ipf = JsonUtil.convertValue(objectNode, IndexProjectionFunction.class);
            return ipfTable(ipf);
          } catch (Exception ignored) {
            // ignore
          }
        }
        if (objectNode.has("start")) {
          try {
            var zrange = JsonUtil.convertValue(objectNode, ZRange.class);
            return GH.table()
                .border(0)
                .cellborder(0)
                .add(GH.bold(zrange.toRangeString()))
                .add(GH.bold(zrange.toShapeString()));
          } catch (Exception ignored) {
            // ignore
          }
        }
        if (objectNode.has("tensorId")) {
          try {
            var sel = JsonUtil.convertValue(objectNode, TensorSelection.class);
            return GH.table()
                .border(0)
                .cellborder(0)
                .tr(
                    jsonToElement(JsonUtil.valueToJsonNodeTree(sel.getTensorId())),
                    GH.bold(sel.getRange().toRangeString()),
                    GH.bold(sel.getRange().toShapeString()));
          } catch (Exception ignored) {
            // ignore
          }
        }
      }

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
                    JsonUtil.Tree.stream(array)
                        .map(item -> GH.tr(asDataValueTD(jsonToElement(item)))));
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
                                  asDataKeyTD(entry.getKey()),
                                  asDataValueTD(jsonToElement(entry.getValue())))));
        }
        default -> {
          var text = node.asText();
          var targetNode = maybeNode(text);
          if (targetNode != null) {
            return nodeAliasTable(targetNode.getId());
          } else {
            return GH.bold(text);
          }
        }
      }
    }

    private static final List<Pair<String, String>> FG_BG_CONTRAST_PAIRS =
        List.of(
            Pair.of("black", "#ff45bb"),
            Pair.of("black", "#45bbff"),
            Pair.of("black", "#45ffbb"),
            Pair.of("black", "#cc65ff"),
            Pair.of("black", "#ffbb45"));

    public GH.TableWrapper nodeAliasTable(UUID id) {
      String alias = getNodeHexAliasMap().get(id);

      var table =
          GH.table()
              .border(0)
              .cellborder(0)
              .cellspacing(0)
              .cellpadding(0)
              .port(alias)
              .fixedsize(true)
              .width(alias.length());

      var row = GH.tr().withParent(table);

      final int runSize = 2;
      String rem = alias;
      int last = 0;
      while (!rem.isEmpty()) {
        String run = rem.substring(0, Math.min(runSize, rem.length()));
        rem = rem.substring(run.length());

        var td = GH.td().withParent(row);
        var font = GH.font().withParent(td).add(GH.bold(run));

        try {
          int val = Integer.parseInt(run, 16) % FG_BG_CONTRAST_PAIRS.size();
          if (val == last) {
            val = (val + 1) % FG_BG_CONTRAST_PAIRS.size();
          }
          last = val;

          var colors = FG_BG_CONTRAST_PAIRS.get(val);

          font.color(colors.getLeft());
          td.bgcolor(colors.getRight());

        } catch (NumberFormatException e) {
          // ignore
        }
      }

      return table;
    }
  }

  public GH.ElementWrapper<?> ipfTable(IndexProjectionFunction ipf) {
    /*
        foo[label=<<b><table border="0">
    <tr>
      <td>
        <table border="0" cellspacing="0" cellborder="0">
          <tr><td border="1" sides="L">1</td><td border="1" sides="R">0</td><td border="1" sides="R">3</td></tr>
          <tr><td border="1" sides="L">0</td><td border="1" sides="R">1</td><td border="1" sides="R">4</td></tr>
        </table>
        </td>
        <td> ⊕ [1, 1]</td>

    </tr>
    </table></b>>]
       */
    var outerTable = GH.table().border(0).cellborder(0).cellspacing(0);
    var outerRow = GH.tr().withParent(outerTable);

    var affineMap = ipf.getAffineMap();
    var projTable = GH.table().border(0).cellborder(0).cellspacing(0);
    outerRow.add(GH.td(projTable));

    var projection = affineMap.projection;
    var offset = affineMap.getOffset();
    for (int i = 0; i < affineMap.outputNDim(); ++i) {
      var tr = GH.tr().withParent(projTable);
      for (int j = 0; j < affineMap.inputNDim(); ++j) {
        var td = GH.td().withParent(tr);
        if (j == 0) {
          td.border(1);
          td.sides("L");
        }
        if (j == affineMap.inputNDim() - 1) {
          td.border(1);
          td.sides("R");
        }
        td.add(Integer.toString(projection.get(i, j)));
      }
      GH.td().withParent(tr).border(1).sides("R").add(Integer.toString(offset.get(i)));
    }

    outerRow.add(GH.td(" ⊕ " + ipf.getShape()));

    return outerTable;
  }

  public ExportContext export(LoomGraph graph) {
    var e = new ExportContext(graph);
    e.export();
    return e;
  }
}
