package loom.graph.export.graphviz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import guru.nidi.graphviz.attribute.*;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.GraphvizCmdLineEngine;
import guru.nidi.graphviz.model.*;
import java.util.*;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

@Data
@Builder
public class GraphVisualizer {
  static {
    // TODO: Make this configurable.
    // TODO: how do I shut up the INFO level logging on this?
    Graphviz.useEngine(new GraphvizCmdLineEngine());
  }

  @Builder.Default private final boolean displayColorLegend = false;

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

      String tensorColor = context.colorForTensor(loomNode.getId());
      gvNode.add(Color.named(tensorColor).fill());
      // gvNode.add(Color.rgb("#74CFFF").fill());

      gvNode.add(
          asHtmlLabel(
              GH.table()
                  .border(0)
                  .cellborder(0)
                  .cellspacing(0)
                  .tr(context.renderDataTypeTitle(loomNode.getTypeAlias()))
                  .add(
                      context.asDataKeyValueTR("dtype", tensorNode.getDtype()),
                      context.asDataKeyValueTR("range", tensorNode.getRange().toRangeString()),
                      context.asDataKeyValueTR("shape", tensorNode.getRange().toShapeString()))));
    }
  }

  public abstract static class TensorSelectionMapBaseExporter implements NodeTypeExporter {
    protected static void tensorSelectionMapEdges(
        ExportContext context,
        LoomNode<?, ?> node,
        Map<String, List<TensorSelection>> inputs,
        boolean isInput) {
      UUID nodeId = node.getId();

      var desc = isInput ? "inputs" : "outputs";

      for (var entry : inputs.entrySet()) {
        var key = entry.getKey();
        var slices = entry.getValue();
        for (int idx = 0; idx < slices.size(); idx++) {
          var slice = slices.get(idx);

          UUID tensorId = slice.getTensorId();

          LoomNode<?, ?> node1 = context.getGraph().assertNode(tensorId);
          String targetNodeColor = context.colorForTensor(node1.getId());
          var targetColor = Color.named(targetNodeColor);
          var color = targetColor.and(targetColor, Color.BLACK);

          Function<Link, Link> config =
              link -> link.with("penwidth", "2").with(color).with(Arrow.NORMAL);

          var selNode =
              Factory.mutNode(node.getId() + "#" + key + "#" + idx)
                  .add(Shape.BOX_3D)
                  .add(Style.FILLED)
                  .add(Color.named(context.colorForTensor(tensorId)).fill());
          selNode.add(
              asHtmlLabel(
                  GH.table()
                      .border(0)
                      .cellborder(0)
                      .cellspacing(0)
                      .cellpadding(0)
                      .add(
                          context.asDataKeyValueTR("range", slice.getRange().toRangeString()),
                          context.asDataKeyValueTR("shape", slice.getRange().toShapeString()))));
          context.exportGraph.add(selNode);

          var nodeProxy = Factory.mutNode(nodeId.toString());
          var tensorProxy = Factory.mutNode(tensorId.toString());

          String port = "%s.%s.%d".formatted(desc, key, idx);

          if (isInput) {
            context.exportGraph.add(
                tensorProxy.addLink(
                    config.apply(
                        tensorProxy.port(Compass.SOUTH).linkTo(selNode.port(Compass.NORTH)))));

            context.exportGraph.add(
                selNode.addLink(
                    config
                        .apply(
                            selNode
                                .port(Compass.SOUTH)
                                .linkTo(nodeProxy.port(port).port(Compass.NORTH)))
                        .with("weight", 4)));

          } else {
            context.exportGraph.add(
                nodeProxy.addLink(
                    config
                        .apply(
                            nodeProxy
                                .port(port)
                                .port(Compass.SOUTH)
                                .linkTo(selNode.port(Compass.NORTH)))
                        .with("weight", 4)));

            context.exportGraph.add(
                selNode.addLink(
                    config.apply(
                        selNode.port(Compass.SOUTH).linkTo(tensorProxy.port(Compass.NORTH)))));
          }
        }
      }
    }
  }

  public static class ApplicationNodeExporter extends TensorSelectionMapBaseExporter {
    public GH.TableWrapper padRow(
        ExportContext context, Map<String, List<TensorSelection>> selectionMap, boolean isInput) {

      var desc = isInput ? "inputs" : "outputs";

      var inputTable =
          GH.table()
              .align(GH.HorizontalAlign.LEFT)
              .fixedsize(true)
              .border(0)
              .cellborder(1)
              .cellspacing(0)
              .cellpadding(1);

      var padRow = GH.tr();
      var labelRow = GH.tr();
      if (isInput) {
        inputTable.add(padRow, labelRow);
      } else {
        inputTable.add(labelRow, padRow);
      }

      var keys = selectionMap.keySet().stream().sorted().toList();
      for (int k = 0; k < keys.size(); ++k) {
        var key = keys.get(k);
        var argSize = selectionMap.get(key).size();

        if (k != 0) {
          padRow.add(GH.td().border(0).add(" "));
          labelRow.add(GH.td().border(0).add(" "));
        }

        for (int i = 0; i < argSize; ++i) {
          var sel = selectionMap.get(key).get(i);
          var color = context.colorForTensor(sel.getTensorId());

          padRow.add(
              GH.td()
                  .port("%s.%s.%d".formatted(desc, key, i))
                  .bgcolor(color)
                  .add(GH.bold(Integer.toString(i))));
        }
        labelRow.add(GH.td().colspan(argSize).add(GH.bold(key)));
      }

      return inputTable;
    }

    @Override
    public void exportNode(
        GraphVisualizer visualizer,
        ExportContext context,
        LoomNode<?, ?> loomNode,
        MutableNode gvNode) {
      var appNode = (ApplicationNode) loomNode;

      var opNode = appNode.getOperationSignatureNode();

      gvNode.add(Shape.COMPONENT);

      var table = GH.table().border(0).cellspacing(0).cellpadding(2);
      table.add(GH.tr(GH.td().add(padRow(context, appNode.getInputs(), true))));

      var innerTable = GH.table().border(0).cellborder(1).cellspacing(0).cellpadding(0);
      table.add(GH.tr(GH.td().add(innerTable)));

      innerTable
          .add(context.renderDataTypeTitle(loomNode.getTypeAlias()))
          .add(context.asDataKeyValueTR("kernel", opNode.getKernel()));

      if (!opNode.getParams().isEmpty()) {
        innerTable.add(context.renderDataTypeTitle("params"));

        var paramsOsObjectNode =
            (ObjectNode) JsonUtil.convertValue(opNode.getParams(), JsonNode.class);
        innerTable.addAll(context.jsonToDataKeyValueTRs(paramsOsObjectNode));
      }

      {
        innerTable.add(GH.hr());
        innerTable.add(context.renderDataTypeTitle("inputs"));

        var keys = appNode.getInputs().keySet().stream().sorted().toList();
        for (var key : keys) {
          var sels = appNode.getInputs().get(key);

          var tr = GH.tr();
          innerTable.add(tr);

          tr.add(GH.td().add(GH.bold(key)));

          var selTable = GH.table();
          tr.add(GH.td().add(selTable));

          selTable.border(0).cellpadding(2);

          for (int i = 0; i < sels.size(); ++i) {
            var sel = sels.get(i);
            var color = context.colorForTensor(sel.getTensorId());

            selTable.add(
                GH.tr(
                    GH.td().bgcolor(color).add(GH.bold(Integer.toString(i))),
                    GH.td().add(context.nodeAliasTable(sel.getTensorId())),
                    GH.td().add(GH.bold(sel.getRange().toRangeString())),
                    GH.td().add(GH.bold(sel.getRange().toShapeString()))));
          }
        }
      }
      {
        innerTable.add(context.renderDataTypeTitle("outputs"));

        var keys = appNode.getOutputs().keySet().stream().sorted().toList();
        for (var key : keys) {
          var sels = appNode.getOutputs().get(key);

          var tr = GH.tr();
          innerTable.add(tr);

          tr.add(GH.td().add(GH.bold(key)));

          var selTable = GH.table();
          tr.add(GH.td().add(selTable));

          selTable.border(0).cellpadding(2);

          for (int i = 0; i < sels.size(); ++i) {
            var sel = sels.get(i);
            var color = context.colorForTensor(sel.getTensorId());

            selTable.add(
                GH.tr(
                    GH.td().bgcolor(color).add(GH.bold(Integer.toString(i))),
                    GH.td().add(GH.bold(sel.getRange().toRangeString())),
                    GH.td().add(GH.bold(sel.getRange().toShapeString()))));
          }
        }
      }

      table.add(GH.tr(GH.td().add(padRow(context, appNode.getOutputs(), false))));

      gvNode.add(asHtmlLabel(table));

      {
        UUID operationId = appNode.getOperationId();
        String operatorAlias = context.getNodeHexAliasMap().get(operationId);
        var link =
            Link.to(
                    Factory.mutNode(loomNode.getId().toString())
                        .port(operatorAlias)
                        .port(Compass.NORTH))
                // .with("constraint", false)
                .with("weight", 0)
                .with("dir", "both")
                .with(Style.DOTTED);
        context.exportGraph.add(Factory.mutNode(operationId.toString()).addLink(link));

        // context.sameRank(loomNode.getId(), operationId);
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
      exportGraph.graphAttrs().add("smoothing", "spring");

      if (displayColorLegend) {
        var table =
            GH.table()
                .border(0)
                .cellborder(0)
                .cellspacing(0)
                .cellpadding(0)
                .add(GH.td().colspan(2).add(GH.bold("Color Legend")));

        table.add(GH.td().colspan(2).add(GH.bold("Node Edge Colors")));
        for (var color : NODE_COLORS) {
          table.add(GH.tr(GH.td().width(10).bgcolor(color).add(" "), GH.td().add(color)));
        }
        exportGraph.add(Factory.mutNode("colors").add(Shape.NONE).add(asHtmlLabel(table)));
      }

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
            link ->
                link.with(Arrow.DOT.open().and(Arrow.DOT.open()))
                    .with(Style.BOLD)
                    .with("weight", 5));

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

    private static final List<String> NODE_COLORS =
        List.of(
            "orchid1",
            "turquoise1",
            "maroon1",
            "mistyrose1",
            "plum1",
            "sienna1",
            "springgreen1",
            "lavenderblush1",
            "aquamarine1",
            "lightsteelblue1");

    // List.of("#696969", "#8b4513", "#006400", "#00008b", "#ff0000", "#00ced1", "#ffa500",
    // "#00ff00", "#00fa9a", "#0000ff", "#ff00ff", "#1e90ff", "#ffff54", "#dda0dd", "#ff1493",
    // "#ffe4b5");

    private final Map<UUID, String> colorAssignments = new LinkedHashMap<>();

    public synchronized String colorForTensor(UUID id) {
      if (colorAssignments.containsKey(id)) {
        return colorAssignments.get(id);
      }
      var color = NODE_COLORS.get(colorAssignments.size() % NODE_COLORS.size());
      colorAssignments.put(id, color);
      return color;
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
