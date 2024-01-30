package org.tensortapestry.loom.graph.export.graphviz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import guru.nidi.graphviz.attribute.*;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.GraphvizCmdLineEngine;
import guru.nidi.graphviz.model.Factory;
import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import java.util.*;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;
import org.tensortapestry.loom.common.collections.IteratorUtils;
import org.tensortapestry.loom.common.json.JsonUtil;
import org.tensortapestry.loom.common.json.JsonViewWrapper;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.LoomNode;
import org.tensortapestry.loom.graph.TraversalUtils;
import org.tensortapestry.loom.graph.dialects.tensorops.*;
import org.tensortapestry.loom.graph.export.ExportUtils;
import org.tensortapestry.loom.zspace.ZRange;
import org.tensortapestry.loom.zspace.ZRangeProjectionMap;

@Data
@Builder
public class GraphVisualizer {
  static {
    // TODO: Make this configurable.
    // TODO: how do I shut up the INFO level logging on this?
    Graphviz.useEngine(new GraphvizCmdLineEngine());
  }

  @Builder.Default
  private final boolean displayColorLegend = false;

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
      var gvNode = context.standardNodePrefix(loomNode);
      context.maybeRenderAnnotations(loomNode);

      gvNode.add(Shape.RECTANGLE);
      gvNode.add(Style.FILLED);
      gvNode.add(Color.named("gray").fill());

      gvNode.add(
        asHtmlLabel(
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

      ExportUtils.findLinks(
        loomNode.viewBodyAsJsonNode(),
        context.getGraph()::hasNode,
        (path, targetId) -> {
          // Remove "$." prefix.
          var p = path.substring(2);

          context.addLink(loomNode.getId(), targetId, link -> link.with(Label.of(p)));
        }
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
    private MutableGraph exportGraph = null;

    @Getter(lazy = true)
    private final Graphviz graphviz = renderGraphviz();

    private Map<UUID, String> renderNodeHexAliasMap() {
      return AliasUtils.uuidAliasMap(
        getGraph().stream().map(LoomNode::getId).toList(),
        minLabelLen
      );
    }

    @Nullable private LoomNode maybeNode(UUID id) {
      return graph.getNode(id);
    }

    @Nullable private LoomNode maybeNode(String idString) {
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
        Factory
          .mutGraph()
          .setDirected(true)
          .graphAttrs()
          .add(Rank.inSubgraph(Rank.RankType.SAME))
          .add(nodes)
      );
    }

    public void addLink(Object fromId, Object toId, Function<Link, Link> config) {
      var link = Link.to(Factory.mutNode(toId.toString()));
      link = config.apply(link);
      exportGraph.add(Factory.mutNode(fromId.toString()).addLink(link));
    }

    private void export() {
      colorTensorOperationNodes();

      exportGraph = Factory.mutGraph("graph");
      exportGraph.setDirected(true);

      // default: 0.25
      // exportGraph.graphAttrs().add("nodesep", "0.4");

      // default: 0.5
      // exportGraph.graphAttrs().add("ranksep", "0.8");

      if (rankDir != Rank.RankDir.TOP_TO_BOTTOM) {
        exportGraph.graphAttrs().add(Rank.dir(rankDir));
      }

      exportGraph.graphAttrs().add("splines", "true");
      exportGraph.graphAttrs().add("bgcolor", backgroundColor);

      if (displayColorLegend) {
        var table = GH
          .table()
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

      for (var node : graph) {
        exporterForNodeType(node.getType()).exportNode(GraphVisualizer.this, this, node);
      }
    }

    public MutableNode standardNodePrefix(LoomNode node) {
      var gvnode = Factory.mutNode(node.getId().toString());

      var table = GH
        .table()
        .border(0)
        .cellborder(0)
        .cellspacing(0)
        .cellpadding(0)
        .add(nodeAliasTable(node.getId()));
      if (node.getLabel() != null) {
        table.add(GH.font().color("green").add(GH.bold("\"%s\"".formatted(node.getLabel()))));
      }
      gvnode.add(asHtmlLabel(table).external());

      exportGraph.add(gvnode);

      return gvnode;
    }

    public void maybeRenderAnnotations(LoomNode node) {
      maybeRenderAnnotations(node.getId().toString(), node.getAnnotations());
    }

    public void maybeRenderAnnotations(String nodeId, Map<String, JsonViewWrapper> annotations) {
      if (annotations.isEmpty()) {
        return;
      }
      var env = graph.assertEnv();

      String gvAnnotationNodeId = nodeId + "#annotations";
      var aNode = Factory.mutNode(gvAnnotationNodeId);
      aNode.add(Shape.COMPONENT);
      aNode.add(Style.FILLED);
      aNode.add("penwidth", 2);
      aNode.add(Color.rgb("#45eebf").fill());

      var wrapperTable = GH.table().border(0).cellborder(0).cellspacing(2).cellpadding(2);
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
          .withParent(wrapperTable);
      }

      aNode.add(asHtmlLabel(wrapperTable));

      exportGraph.add(aNode);

      addLink(
        nodeId,
        gvAnnotationNodeId,
        link -> link.with(Arrow.DOT.open().and(Arrow.DOT.open())).with(Style.BOLD).with("weight", 5)
      );

      sameRank(nodeId, gvAnnotationNodeId);
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
      return Graphviz.fromGraph(exportGraph).scale(graphScale);
    }

    public GH.ElementWrapper<?> asDataKeyValueTR(Object key, Object... values) {
      return GH.tr(asDataKeyTD(key.toString()), asDataValueTD(values));
    }

    public GH.TableDataWrapper asDataKeyTD(String key) {
      return GH
        .td()
        .align(GH.TableDataAlign.RIGHT)
        .valign(GH.VerticalAlign.TOP)
        .add(GH.bold(" %s ".formatted(key)));
    }

    public GH.TableDataWrapper asDataValueTD(Object... value) {
      return GH
        .td()
        .align(GH.TableDataAlign.LEFT)
        .valign(GH.VerticalAlign.TOP)
        .add(GH.bold().add(" ").add(value).add(" "));
    }

    public List<GH.ElementWrapper<?>> jsonToDataKeyValueTRs(ObjectNode node) {
      List<GH.ElementWrapper<?>> rows = new ArrayList<>();
      node
        .fields()
        .forEachRemaining(entry ->
          rows.add(asDataKeyValueTR(entry.getKey(), jsonToElement(entry.getValue())))
        );
      return rows;
    }

    public GH.ElementWrapper<?> jsonToElement(JsonNode node) {
      if (node instanceof ObjectNode objectNode) {
        if (objectNode.has("affineMap")) {
          try {
            var ipf = JsonUtil.convertValue(objectNode, ZRangeProjectionMap.class);
            return IPFFormatter.renderIPF(ipf);
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
              .tr(
                jsonToElement(JsonUtil.valueToJsonNodeTree(sel.getTensorId())),
                GH.bold(sel.getRange().toRangeString()),
                GH.bold(sel.getRange().toShapeString())
              );
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
              .cellborder(0)
              .cellspacing(2)
              .addAll(
                JsonUtil.Tree.stream(array).map(item -> GH.tr(asDataValueTD(jsonToElement(item))))
              );
          }
        }
        case ObjectNode object -> {
          return GH
            .table()
            .border(0)
            .cellborder(1)
            .cellspacing(0)
            .addAll(
              JsonUtil.Tree
                .stream(object)
                .map(entry ->
                  GH.tr(asDataKeyTD(entry.getKey()), asDataValueTD(jsonToElement(entry.getValue())))
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

      for (Pair<Integer, Set<UUID>> colorPair : IteratorUtils.enumerate(
        coloring.getColorClasses()
      )) {
        int color = colorPair.getLeft();
        var colorSet = colorPair.getRight();

        for (var idxId : IteratorUtils.enumerate(colorSet.stream().sorted().toList())) {
          int idx = idxId.getLeft();
          var id = idxId.getRight();
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

    public Color colorSchemeForNode(UUID id) {
      var colorPair = getColoringForNode(id);
      return Color.named(colorPair.getLeft()).gradient(Color.named(colorPair.getRight()));
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
