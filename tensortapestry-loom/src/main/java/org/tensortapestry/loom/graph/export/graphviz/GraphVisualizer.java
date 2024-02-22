package org.tensortapestry.loom.graph.export.graphviz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.GraphvizCmdLineEngine;

import java.awt.Color;
import java.util.*;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;
import org.tensortapestry.common.collections.EnumerationUtils;
import org.tensortapestry.common.json.JsonUtil;
import org.tensortapestry.common.json.JsonViewWrapper;
import org.tensortapestry.graphviz.DotGraph;
import org.tensortapestry.graphviz.GraphvizAttribute;
import org.tensortapestry.graphviz.HtmlLabel;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.LoomNode;
import org.tensortapestry.loom.graph.TraversalUtils;
import org.tensortapestry.loom.graph.dialects.tensorops.*;
import org.tensortapestry.zspace.ZRange;
import org.tensortapestry.zspace.ZRangeProjectionMap;

@Data
@Builder
public class GraphVisualizer {
  static {
    // TODO: Make this configurable.
    // TODO: how do I shut up the INFO level logging on this?
    Graphviz.useEngine(new GraphvizCmdLineEngine());
  }

  @Builder.Default
  private final boolean foldApplicationNodes = true;

  @Builder.Default
  private final String backgroundColor = "#E2E2E2";

  @Builder.Default
  private final int minLabelLen = 6;

  @Builder.Default
  private final Rank.RankDir rankDir = Rank.RankDir.TOP_TO_BOTTOM;

  @Builder.Default
  private double graphScale = 2.5;

  @Builder.Default
  private Map<String, NodeTypeExporter> nodeTypeExporters = new HashMap<>();

  @Builder.Default
  private NodeTypeExporter defaultNodeTypeExporter = new DefaultNodeExporter();

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
    exporter.getNodeTypeExporters().put(OperationNode.TYPE, new OperationNodeExporter());
    return exporter;
  }

  public interface NodeTypeExporter {
    void exportNode(GraphVisualizer visualizer, ExportContext context, LoomNode loomNode);
  }

  public static class DefaultNodeExporter implements NodeTypeExporter {

    @Override
    public void exportNode(GraphVisualizer visualizer, ExportContext context, LoomNode loomNode) {
      var dotNode = context.createPrimaryNode(loomNode);
      dotNode
        .set(GraphvizAttribute.SHAPE, "rectangle")
        .set(GraphvizAttribute.STYLE, "filled")
        .set(GraphvizAttribute.COLOR, "gray");

      context.maybeRenderAnnotations(loomNode);

      dotNode.set(
        GraphvizAttribute.LABEL,
        HtmlLabel.from(
          GH
            .table()
            .border(0)
            .cellborder(0)
            .cellspacing(2)
            .cellpadding(2)
            .bgcolor("white")
            .add(context.renderDataTable(loomNode.getTypeAlias(), loomNode.viewBodyAsJsonNode()))
        )
      );
    }
  }

  public static Label asHtmlLabel(Object obj) {
    return Label.html(obj.toString());
  }

  @Data
  public class ExportContext {

    @Nonnull
    private final LoomGraph graph;

    public ExportContext(@Nonnull LoomGraph graph) {
      this.graph = Objects.requireNonNull(graph);
    }

    @Getter(lazy = true)
    private final Map<UUID, String> nodeHexAliasMap = renderNodeHexAliasMap();

    @Getter
    private DotGraph dotGraph = new DotGraph();

    @Getter(lazy = true)
    private final Graphviz graphviz = renderGraphviz();

    private Map<UUID, String> renderNodeHexAliasMap() {
      return AliasUtils.uuidAliasMap(
        getGraph().stream().map(LoomNode::getId).toList(),
        minLabelLen
      );
    }

    @Nullable
    private LoomNode maybeNode(UUID id) {
      return graph.getNode(id);
    }

    @Nullable
    private LoomNode maybeNode(String idString) {
      try {
        var id = UUID.fromString(idString);
        return maybeNode(id);
      } catch (IllegalArgumentException e) {
        return null;
      }
    }

    private void export() {
      colorTensorOperationNodes();

      dotGraph
        .getAttributes()
        .set(GraphvizAttribute.NEWRANK, true)
        .set(GraphvizAttribute.SPLINES, "ortho")
        .set(GraphvizAttribute.CONCENTRATE, true)
        // .set(GraphAttribute.CLUSTERRANK, "local")
        // .set(GraphAttribute.NODESEP, 0.4)
        .set(GraphvizAttribute.RANKSEP, 0.6)
        .set(GraphvizAttribute.BGCOLOR, backgroundColor);

      Set<TensorNode> sourceNodes = new HashSet<>();
      var visited = new HashSet<UUID>();
      for (var node : graph.byType(TensorNode.TYPE)) {
        if (!visited.add(node.getId())) {
          continue;
        }

        var tensorNode = TensorNode.wrap(node);
        sourceNodes.add(tensorNode);

        exporterForNodeType(node.getType()).exportNode(GraphVisualizer.this, this, node);
      }
      for (var node : graph.byType(OperationNode.TYPE)) {
        if (!visited.add(node.getId())) {
          continue;
        }

        var operationNode = OperationNode.wrap(node);
        operationNode
          .getOutputNodes()
          .values()
          .stream()
          .flatMap(List::stream)
          .forEach(sourceNodes::remove);

        exporterForNodeType(node.getType()).exportNode(GraphVisualizer.this, this, node);
      }
      for (var node : graph) {
        if (!visited.add(node.getId())) {
          continue;
        }
        exporterForNodeType(node.getType()).exportNode(GraphVisualizer.this, this, node);
      }

    }

    public void decoratePrimaryNode(LoomNode loomNode, DotGraph.Node dotNode) {
      var table = GH
        .table()
        .border(0)
        .cellborder(0)
        .cellspacing(0)
        .cellpadding(0)
        .add(nodeAliasTable(loomNode.getId()));

      if (loomNode.getLabel() != null) {
        table.add(GH.font().color("green").add(GH.bold("\"%s\"".formatted(loomNode.getLabel()))));
      }

      dotNode.set(GraphvizAttribute.XLABEL, HtmlLabel.from(table));
      dotNode.set(GraphvizAttribute.PENWIDTH, 2);
    }

    public DotGraph.Node createPrimaryNode(LoomNode node) {
      var dotNode = dotGraph.createNode(node.getId().toString());
      decoratePrimaryNode(node, dotNode);
      return dotNode;
    }

    public void maybeRenderAnnotations(LoomNode node, DotGraph.SubGraph subGraph) {
      maybeRenderAnnotations(node.getId().toString(), node.getAnnotations(), subGraph);
    }

    public void maybeRenderAnnotations(LoomNode node) {
      maybeRenderAnnotations(node, dotGraph.getRoot());
    }

    public void maybeRenderAnnotations(
      String nodeId,
      Map<String, JsonViewWrapper> annotations,
      DotGraph.SubGraph subGraph
    ) {
      if (annotations.isEmpty()) {
        return;
      }
      var env = graph.assertEnv();

      String dotId = nodeId + "#annotations";

      var dotAnnotationNode = subGraph.createNode(dotId);
      dotAnnotationNode
        .set(GraphvizAttribute.SHAPE, "component")
        .set(GraphvizAttribute.STYLE, "filled")
        .set(GraphvizAttribute.PENWIDTH, 2)
        .set(GraphvizAttribute.FILLCOLOR, "#45eebf");

      var labelTable = GH.table().border(0).cellborder(0).cellspacing(2).cellpadding(2);
      for (var entry : annotations
        .entrySet()
        .stream()
        .sorted(Map.Entry.comparingByKey())
        .toList()) {
        renderDataTable(
          env.getTypeAlias(entry.getKey()),
          JsonUtil.valueToJsonNodeTree(entry.getValue())
        )
          .bgcolor("white")
          .withParent(labelTable);
      }

      dotAnnotationNode.set(GraphvizAttribute.LABEL, HtmlLabel.from(labelTable));

      var dotNode = dotGraph.assertLookup(nodeId, DotGraph.Node.class);
      dotGraph.createEdge(dotNode, dotAnnotationNode)
        .set(GraphvizAttribute.PENWIDTH, 3)
        .set(GraphvizAttribute.COLOR, "black:white:white:white:white:white:white:black")
        .set(GraphvizAttribute.ARROWHEAD, "none")
        .set(GraphvizAttribute.WEIGHT, 5);

      dotGraph.sameRank(dotNode, dotAnnotationNode);
    }

    public GH.TableDataWrapper renderDataTypeTitle(String title) {
      return GH
        .td()
        .colspan(2)
        .align(GH.TableDataAlign.LEFT)
        .add(GH.font().color("teal").add(GH.bold(" %s ".formatted(title))));
    }

    public GH.TableWrapper renderDataTable(String title, JsonNode data) {
      return GH
        .table()
        .border(0)
        .cellborder(1)
        .cellspacing(0)
        .cellpadding(0)
        .add(renderDataTypeTitle(title))
        .add(GH.td().colspan(2).add(jsonToElement(data)));
    }

    private Graphviz renderGraphviz() {
      return Graphviz.fromString(dotGraph.toString()).scale(graphScale);
    }

    public GH.ElementWrapper<?> asDataKeyValueTr(Object key, Object... values) {
      return GH.tr(asDataKeyTd(key.toString()), asDataValueTd(values));
    }

    public GH.TableDataWrapper asDataKeyTd(String key) {
      return GH
        .td()
        .align(GH.TableDataAlign.RIGHT)
        .valign(GH.VerticalAlign.TOP)
        .add(GH.bold(" %s ".formatted(key)));
    }

    public GH.TableDataWrapper asDataValueTd(Object... value) {
      return GH
        .td()
        .align(GH.TableDataAlign.LEFT)
        .valign(GH.VerticalAlign.TOP)
        .add(GH.bold().add(" ").add(value).add(" "));
    }

    public List<GH.ElementWrapper<?>> jsonToDataKeyValueTrs(ObjectNode node) {
      List<GH.ElementWrapper<?>> rows = new ArrayList<>();
      node
        .fields()
        .forEachRemaining(entry ->
          rows.add(asDataKeyValueTr(entry.getKey(), jsonToElement(entry.getValue())))
        );
      return rows;
    }

    public GH.ElementWrapper<?> jsonToElement(JsonNode node) {
      if (node instanceof ObjectNode objectNode) {
        if (objectNode.has("affineMap")) {
          try {
            var ipf = JsonUtil.convertValue(objectNode, ZRangeProjectionMap.class);
            return IPFFormatter.renderRangeProjectionMap(ipf);
          } catch (Exception ignored) {
            // ignore
          }
        }
        if (objectNode.has("start")) {
          try {
            var zrange = JsonUtil.convertValue(objectNode, ZRange.class);
            return GH
              .table()
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
            return GH
              .table()
              .border(0)
              .cellborder(0)
              .tr(jsonToElement(JsonUtil.valueToJsonNodeTree(sel.getTensorId())))
              .tr(GH.bold(sel.getRange().toRangeString()))
              .tr(GH.bold(sel.getRange().toShapeString()));
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
            return GH
              .table()
              .border(0)
              .cellborder(1)
              .cellspacing(0)
              .cellpadding(2)
              .addAll(
                JsonUtil.Tree.stream(array).map(item -> GH.tr(asDataValueTd(jsonToElement(item))))
              );
          }
        }
        case ObjectNode object -> {
          if (object.isEmpty()) {
            return GH.bold("(empty)");
          }
          return GH
            .table()
            .border(0)
            .cellborder(1)
            .cellspacing(0)
            .addAll(
              JsonUtil.Tree
                .stream(object)
                .map(entry ->
                  GH.tr(asDataKeyTd(entry.getKey()), asDataValueTd(jsonToElement(entry.getValue())))
                )
            );
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

    public void colorTensorOperationNodes() {
      var coloring = TraversalUtils.tensorOperationColoring(getGraph());

      for (Map.Entry<Integer, Set<UUID>> colorPair : EnumerationUtils.enumerate(
        coloring.getColorClasses()
      )) {
        int color = colorPair.getKey();
        var colorSet = colorPair.getValue();

        for (var idxId : EnumerationUtils.enumerate(colorSet.stream().sorted().toList())) {
          int idx = idxId.getKey();
          var id = idxId.getValue();
          setColoringForNode(id, color, idx);
        }
      }
    }

    private static final List<String> NODE_COLORS = List.of(
      "#EF7BDE",
      "#38DAE0",
      "#ee9944",
      "#99dd55",
      "#DC9E87",
      "#44dd88",
      "#E98FA5",
      "#22ccbb"
    );

    public Pair<Color, Color> colorSchemeForNode(UUID id) {
      var p = getColoringForNode(id);
      return Pair.of(Color.decode(p.getLeft()), Color.decode(p.getRight()));
    }

    private final Map<UUID, Pair<String, String>> nodeColorings = new HashMap<>();

    public void setColoringForNode(UUID id, int color, int idx) {
      var k = NODE_COLORS.size();
      var h = color % k;
      var baseColor = NODE_COLORS.get(h);

      var s = (h + idx + 1 + (k / 2)) % k;
      if (s == h) {
        s = (s + 1) % k;
      }
      var stripeColor = NODE_COLORS.get(s);

      nodeColorings.put(id, Pair.of(baseColor, stripeColor));
    }

    public String getPrimaryColorForNode(UUID id) {
      return nodeColorings.get(id).getLeft();
    }

    public Pair<String, String> getColoringForNode(UUID id) {
      return nodeColorings.get(id);
    }

    private static final List<Pair<String, String>> FG_BG_CONTRAST_PAIRS = List.of(
      Pair.of("black", "#ffbbbb"),
      Pair.of("black", "#aabbff"),
      Pair.of("black", "#aaffbb"),
      Pair.of("black", "#ccbbff"),
      Pair.of("black", "#ffbbaa")
    );

    public GH.TableWrapper nodeAliasTable(UUID id) {
      String alias = getNodeHexAliasMap().get(id);

      var table = GH
        .table()
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

  public ExportContext export(LoomGraph graph) {
    var e = new ExportContext(graph);
    e.export();
    return e;
  }
}
