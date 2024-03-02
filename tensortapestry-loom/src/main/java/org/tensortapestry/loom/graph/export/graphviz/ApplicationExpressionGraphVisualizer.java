package org.tensortapestry.loom.graph.export.graphviz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import guru.nidi.graphviz.engine.Graphviz;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.tensortapestry.common.json.JsonUtil;
import org.tensortapestry.common.json.JsonViewWrapper;
import org.tensortapestry.graphviz.DotGraph;
import org.tensortapestry.graphviz.FormatUtils;
import org.tensortapestry.graphviz.GraphvizAttribute;
import org.tensortapestry.graphviz.HtmlLabel;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.LoomNode;
import org.tensortapestry.loom.graph.LoomNodeWrapper;
import org.tensortapestry.loom.graph.dialects.tensorops.*;
import org.tensortapestry.zspace.ZRange;
import org.tensortapestry.zspace.ZRangeProjectionMap;

@Data
@Builder
public class ApplicationExpressionGraphVisualizer {

  @Data
  public static class ExportContext {

    @Nonnull
    private final LoomGraph graph;

    public ExportContext(@Nonnull LoomGraph graph) {
      this.graph = Objects.requireNonNull(graph);
    }

    @Getter
    private DotGraph dotGraph = new DotGraph();

    @Getter(lazy = true)
    private final Graphviz graphviz = renderGraphviz();

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

    public static void exportTensorNode(ExportContext context, TensorNode tensorNode) {
      var colorScheme = context.colorSchemeForNode(tensorNode);

      var dotNode = context.createPrimaryNode(tensorNode.unwrap());
      dotNode
        .set(GraphvizAttribute.SHAPE, "box3d")
        .set(GraphvizAttribute.STYLE, "filled")
        .set(GraphvizAttribute.FILLCOLOR, colorScheme.getPrimary())
        .set(GraphvizAttribute.GRADIENTANGLE, 315)
        .set(GraphvizAttribute.MARGIN, 0.2);

      context.renderNodeTags(tensorNode.unwrap());

      GH.TableWrapper labelTable = GH
        .table()
        .bgcolor("white")
        .border(1)
        .cellborder(0)
        .cellspacing(0)
        .add(
          GH
            .td()
            .colspan(2)
            .align(GH.TableDataAlign.LEFT)
            .add(GH.font().add(GH.bold(" %s ".formatted(tensorNode.getTypeAlias())))),
          context.asDataKeyValueTr("dtype", tensorNode.getDtype()),
          context.asDataKeyValueTr("range", tensorNode.getRange().toRangeString()),
          context.asDataKeyValueTr("shape", tensorNode.getRange().toShapeString())
        );

      dotNode.set(GraphvizAttribute.LABEL, HtmlLabel.from(labelTable));
    }

    public static void exportOperationNode(ExportContext context, OperationNode operationNode) {
      var opColorScheme = context.colorSchemeForNode(operationNode);

      var opCluster = context.getDotGraph().createCluster(operationNode.getId() + "_op_cluster");
      opCluster
        .getAttributes()
        .set(GraphvizAttribute.MARGIN, 16)
        .set(GraphvizAttribute.PERIPHERIES, 2)
        .set(GraphvizAttribute.PENWIDTH, 4)
        .set(GraphvizAttribute.STYLE, "rounded");

      if (operationNode.hasTag(TensorOpNodes.IO_SEQUENCE_POINT_TYPE)) {
        // TODO: color alone is not enough to distinguish IO operations..
        opCluster
          .getAttributes()
          .set(GraphvizAttribute.BGCOLOR, "lightblue")
          .set(GraphvizAttribute.STYLE, "filled, rounded");
      }

      var dotNode = opCluster.createNode(operationNode.getId().toString());
      context.decoratePrimaryNode(operationNode.unwrap(), dotNode);
      dotNode
        .set(GraphvizAttribute.SHAPE, "tab")
        .set(GraphvizAttribute.STYLE, "filled")
        .set(GraphvizAttribute.FILLCOLOR, opColorScheme.getPrimary());

      context.renderNodeTags(operationNode.unwrap(), opCluster);

      GH.TableWrapper labelTable = GH
        .table()
        .bgcolor("white")
        .border(0)
        .cellborder(0)
        .cellspacing(0)
        .add(
          context.renderDataTable(operationNode.getTypeAlias(), operationNode.viewBodyAsJsonNode())
        );

      dotNode.set(GraphvizAttribute.LABEL, HtmlLabel.from(labelTable));

      if (operationNode.getApplicationNodes().stream().count() > 1) {
        routeNodes(context, operationNode, operationNode.getInputs(), true);
        routeNodes(context, operationNode, operationNode.getOutputs(), false);
      }
    }

    protected static void routeNodes(
      ExportContext context,
      OperationNode operationNode,
      Map<String, List<TensorSelection>> selectionMap,
      boolean isInput
    ) {
      var ioDesc = isInput ? "input" : "output";

      var graph = operationNode.assertGraph();
      var operationId = operationNode.getId();

      DotGraph dotGraph = context.getDotGraph();
      var routeCluster = dotGraph.createCluster(
        "%s_route_cluster_%s".formatted(operationId, ioDesc)
      );
      routeCluster
        .getAttributes()
        .set(GraphvizAttribute.PERIPHERIES, 0)
        .set(GraphvizAttribute.NODESEP, 0)
        .set(GraphvizAttribute.RANKSEP, 0);

      Set<UUID> routedTensors = new HashSet<>();
      List<DotGraph.Node> routeNodes = new ArrayList<>();
      for (var entry : selectionMap.entrySet()) {
        var slices = entry.getValue();
        for (TensorSelection tensorSelection : slices) {
          var tensorId = tensorSelection.getTensorId();
          var tensorNode = graph.assertNode(tensorId, TensorNode.class);

          if (!routedTensors.add(tensorId)) {
            continue;
          }

          var tensorDotNode = dotGraph.assertLookup(tensorId.toString(), DotGraph.Node.class);

          var routeId = "%s_route_%s_%s".formatted(operationId, ioDesc, tensorId);

          var tensorColor = context.colorSchemeForNode(tensorNode).getPrimary();

          var routeNode = routeCluster.createNode(routeId);
          routeNode
            .set(GraphvizAttribute.LABEL, "")
            .set(GraphvizAttribute.SHAPE, "box")
            .set(GraphvizAttribute.HEIGHT, 0.4)
            .set(GraphvizAttribute.WIDTH, 0.4)
            .set(GraphvizAttribute.STYLE, "filled")
            .set(GraphvizAttribute.COLOR, tensorColor);

          routeNodes.add(routeNode);

          DotGraph.Edge routeEdge;
          if (isInput) {
            routeEdge = dotGraph.createEdge(tensorDotNode, routeNode);
            routeEdge.set(GraphvizAttribute.HEADCLIP, false);
          } else {
            routeEdge = dotGraph.createEdge(routeNode, tensorDotNode);
            routeEdge.set(GraphvizAttribute.TAILCLIP, false);
          }

          routeEdge
            .set(GraphvizAttribute.ARROWHEAD, "none")
            .set(GraphvizAttribute.COLOR, tensorColor)
            .set(GraphvizAttribute.PENWIDTH, 24);
        }
      }

      // Create a grid of route and index nodes to force a diagonal spacing layout.
      if (routeNodes.size() > 1) {
        for (var id : routedTensors) {
          var tensorNode = dotGraph.assertLookup(id.toString(), DotGraph.Node.class);
          DotGraph.Edge orderingEdge;
          if (isInput) {
            orderingEdge = dotGraph.createEdge(tensorNode, routeNodes.getFirst());
          } else {
            orderingEdge = dotGraph.createEdge(routeNodes.getFirst(), tensorNode);
          }
          orderingEdge.set(GraphvizAttribute.STYLE, "invis");
        }

        List<List<DotGraph.Node>> rows = new ArrayList<>();
        for (int r = 0; r < routeNodes.size(); r++) {
          var row = new ArrayList<DotGraph.Node>();
          rows.add(row);
          for (int c = 0; c < routeNodes.size(); c++) {
            DotGraph.Node cur;
            if (r == c) {
              cur = routeNodes.get(r);
            } else {
              cur =
                routeCluster.createNode("%s_route_%s_%d_%d".formatted(operationId, ioDesc, r, c));

              cur
                .set(GraphvizAttribute.STYLE, "invis")
                .set(GraphvizAttribute.WIDTH, 2)
                .set(GraphvizAttribute.HEIGHT, 0.1)
                .set(GraphvizAttribute.SHAPE, "box")
                .set(GraphvizAttribute.LABEL, "");
            }
            row.add(cur);

            if (r > 0) {
              var prev = rows.get(r - 1).get(c);
              routeCluster.createEdge(prev, cur).set(GraphvizAttribute.STYLE, "invis");
            }
            if (c > 0) {
              var prev = row.get(c - 1);
              routeCluster
                .createEdge(prev, cur)
                // .set(GraphvizAttribute.MINLEN, 4)
                .set(GraphvizAttribute.STYLE, "invis");
            }

            if (r > 0 && c > 0) {
              var prev = rows.get(r - 1).get(c - 1);
              routeCluster.createEdge(prev, cur).set(GraphvizAttribute.STYLE, "invis");
            }
          }
        }
        for (var row : rows) {
          dotGraph.sameRank(row);
        }
      }
    }

    public static void exportApplicationNode(
      ExportContext context,
      ApplicationNode applicationNode
    ) {
      DotGraph dotGraph = context.getDotGraph();
      var operation = applicationNode.getOperationNode();

      var opCluster = dotGraph.assertLookup(
        operation.getId() + "_op_cluster",
        DotGraph.Cluster.class
      );

      Color operationColor = context.colorSchemeForNode(operation).getPrimary();

      var clusterColor =
        "%s:%s".formatted(
            FormatUtils.colorToRgbaString(operationColor.brighter().brighter().brighter()),
            FormatUtils.colorToRgbaString(operationColor.brighter().brighter())
          );

      var dotCluster = opCluster.createCluster("app_%s".formatted(applicationNode.getId()));
      dotCluster
        .getAttributes()
        .set(GraphvizAttribute.NODESEP, 0.2)
        .set(GraphvizAttribute.RANKSEP, 0.2)
        .set(GraphvizAttribute.PENWIDTH, 2)
        .set(GraphvizAttribute.BGCOLOR, clusterColor)
        .set(GraphvizAttribute.GRADIENTANGLE, 315)
        .set(GraphvizAttribute.STYLE, "filled, dashed, bold, rounded");

      var dotOpNode = dotGraph.assertLookup(operation.getId().toString(), DotGraph.Node.class);

      var dotNode = dotCluster.createNode(applicationNode.getId().toString());
      context.decoratePrimaryNode(applicationNode.unwrap(), dotNode);
      dotNode
        .set(GraphvizAttribute.SHAPE, "note")
        .set(GraphvizAttribute.STYLE, "filled")
        .set(GraphvizAttribute.FILLCOLOR, operationColor);

      dotGraph.sameRank(dotNode, dotOpNode);

      dotGraph
        .createEdge(dotNode, dotOpNode)
        .set(GraphvizAttribute.CONSTRAINT, false)
        .set(GraphvizAttribute.STYLE, "invis");

      context.renderNodeTags(applicationNode.unwrap(), dotCluster);

      GH.TableWrapper labelTable = GH
        .table()
        .bgcolor("white")
        .border(0)
        .cellborder(0)
        .cellspacing(0)
        .add(
          context.renderDataTable(
            applicationNode.getTypeAlias(),
            applicationNode.viewBodyAsJsonNode()
          )
        );

      dotNode.set(GraphvizAttribute.LABEL, HtmlLabel.from(labelTable));

      var useRouteNodes = operation.getApplicationNodes().stream().count() > 1;

      selectionMapNodes(
        context,
        dotCluster,
        applicationNode,
        dotNode,
        applicationNode.getInputs(),
        useRouteNodes,
        true
      );
      selectionMapNodes(
        context,
        dotCluster,
        applicationNode,
        dotNode,
        applicationNode.getOutputs(),
        useRouteNodes,
        false
      );
    }

    protected static void selectionMapNodes(
      ExportContext context,
      DotGraph.Cluster dotCluster,
      ApplicationNode application,
      DotGraph.Node dotNode,
      Map<String, List<TensorSelection>> inputs,
      boolean useRouteNodes,
      boolean isInput
    ) {
      var dotGraph = context.getDotGraph();

      UUID nodeId = application.getId();

      var ioDesc = isInput ? "input" : "output";
      var selCluster = dotCluster.createCluster("%s_%s".formatted(nodeId, ioDesc));

      selCluster
        .getAttributes()
        .set(GraphvizAttribute.STYLE, "invis")
        .set(GraphvizAttribute.PERIPHERIES, 0)
        .set(GraphvizAttribute.RANK, "same");

      for (var entry : inputs.entrySet()) {
        var key = entry.getKey();
        var slices = entry.getValue();
        for (int idx = 0; idx < slices.size(); idx++) {
          var tensorSelection = slices.get(idx);

          String selDesc = "%s[%d]".formatted(key, idx);

          String selNodeId = nodeId + "_sel_" + key + "_" + idx;

          UUID tensorId = tensorSelection.getTensorId();
          var tensorNode = context.getGraph().assertNode(tensorId, TensorNode.class);

          var tensorColor = context.colorSchemeForNode(tensorNode).getPrimary();

          var dotSelectionNode = selCluster.createNode(selNodeId);
          dotSelectionNode
            .set(GraphvizAttribute.SHAPE, "box3d")
            .set(GraphvizAttribute.FILLCOLOR, tensorColor)
            .set(GraphvizAttribute.STYLE, "filled")
            .set(GraphvizAttribute.GRADIENTANGLE, 315)
            .set(GraphvizAttribute.PENWIDTH, 2)
            .set(GraphvizAttribute.MARGIN, 0.15);

          dotSelectionNode.set(
            GraphvizAttribute.LABEL,
            HtmlLabel.from(
              GH
                .table()
                .bgcolor("white")
                .border(1)
                .cellborder(0)
                .cellspacing(0)
                .cellpadding(0)
                .add(
                  GH.tr(GH.bold(selDesc), GH.bold(context.nodeAlias(tensorId))),
                  context.asDataKeyValueTr("range", tensorSelection.getRange().toRangeString()),
                  context.asDataKeyValueTr("shape", tensorSelection.getRange().toShapeString())
                )
            )
          );

          DotGraph.Node sourceNode;
          if (useRouteNodes) {
            var routeId =
              "%s_route_%s_%s".formatted(application.getOperationId(), ioDesc, tensorId);
            sourceNode = context.getDotGraph().assertLookup(routeId, DotGraph.Node.class);
          } else {
            sourceNode =
              context.getDotGraph().assertLookup(tensorId.toString(), DotGraph.Node.class);
          }

          DotGraph.Edge routeEdge;
          DotGraph.Edge selEdge;

          if (isInput) {
            routeEdge = dotGraph.createEdge(sourceNode, dotSelectionNode);
            selEdge = dotCluster.createEdge(dotSelectionNode, dotNode);

            if (useRouteNodes) {
              routeEdge.set(GraphvizAttribute.TAILCLIP, false);
            }
          } else {
            selEdge = dotCluster.createEdge(dotNode, dotSelectionNode);
            routeEdge = dotGraph.createEdge(dotSelectionNode, sourceNode);

            if (useRouteNodes) {
              routeEdge.set(GraphvizAttribute.HEADCLIP, false);
            }
          }

          String colorStr = FormatUtils.colorToRgbaString(tensorColor);
          String transColorStr = colorStr + "C0";

          selEdge
            .set(GraphvizAttribute.PENWIDTH, 24)
            .set(GraphvizAttribute.COLOR, colorStr)
            .set(GraphvizAttribute.ARROWHEAD, "none");
          routeEdge
            .set(GraphvizAttribute.PENWIDTH, 24)
            .set(GraphvizAttribute.COLOR, transColorStr)
            .set(GraphvizAttribute.ARROWHEAD, "none");

          // Force the selection nodes to cluster and layout in call order.
          if (selCluster.getNodes().size() > 1) {
            var last = selCluster
              .getNodes()
              .get(selCluster.getNodes().indexOf(dotSelectionNode) - 1);
            selCluster
              .createEdge(last, dotSelectionNode)
              .set(GraphvizAttribute.STYLE, "invis")
              .set(GraphvizAttribute.WEIGHT, 10);
          }
        }
      }
    }

    private void export() {
      nodeColorings = OperationExpressionColoring.builder().graph(getGraph()).build();

      dotGraph
        .getAttributes()
        .set(GraphvizAttribute.SCALE, 2.5)
        .set(GraphvizAttribute.NEWRANK, true)
        .set(GraphvizAttribute.SPLINES, "ortho")
        .set(GraphvizAttribute.CONCENTRATE, true)
        // .set(GraphAttribute.CLUSTERRANK, "local")
        // .set(GraphAttribute.NODESEP, 0.4)
        .set(GraphvizAttribute.RANKSEP, 0.6)
        .set(GraphvizAttribute.BGCOLOR, "#E2E2E2");

      for (var t : graph.byType(TensorNode.class)) {
        exportTensorNode(this, t);
      }
      for (var op : graph.byType(OperationNode.class)) {
        exportOperationNode(this, op);
      }
      for (var app : graph.byType(ApplicationNode.class)) {
        exportApplicationNode(this, app);
      }
    }

    public void decoratePrimaryNode(LoomNode loomNode, DotGraph.Node dotNode) {
      var table = GH
        .table()
        .border(0)
        .cellborder(0)
        .cellspacing(0)
        .cellpadding(0)
        .add(GH.bold(nodeAlias(loomNode.getId())));

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

    public void renderNodeTags(LoomNode node, DotGraph.SubGraph subGraph) {
      renderNodeTags(node.getId().toString(), node.getTags(), subGraph);
    }

    public void renderNodeTags(LoomNode node) {
      renderNodeTags(node, dotGraph.getRoot());
    }

    public void renderNodeTags(
      String nodeId,
      Map<String, JsonViewWrapper> tags,
      DotGraph.SubGraph subGraph
    ) {
      if (tags.isEmpty()) {
        return;
      }
      var env = graph.assertEnv();

      String dotId = nodeId + "_tags";

      var dotTagNode = subGraph.createNode(dotId);
      dotTagNode
        .set(GraphvizAttribute.SHAPE, "component")
        .set(GraphvizAttribute.STYLE, "filled")
        .set(GraphvizAttribute.PENWIDTH, 2)
        .set(GraphvizAttribute.FILLCOLOR, "#45eebf");

      var labelTable = GH.table().border(0).cellborder(0).cellspacing(2).cellpadding(2);
      for (var entry : tags.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
        renderDataTable(
          env.getTypeAlias(entry.getKey()),
          JsonUtil.valueToJsonNodeTree(entry.getValue())
        )
          .bgcolor("white")
          .withParent(labelTable);
      }

      dotTagNode.set(GraphvizAttribute.LABEL, HtmlLabel.from(labelTable));

      var dotNode = dotGraph.assertLookup(nodeId, DotGraph.Node.class);
      dotGraph
        .createEdge(dotNode, dotTagNode)
        .set(GraphvizAttribute.PENWIDTH, 3)
        .set(GraphvizAttribute.COLOR, "black:white:white:white:white:white:white:black")
        .set(GraphvizAttribute.ARROWHEAD, "none")
        .set(GraphvizAttribute.WEIGHT, 5);

      dotGraph.sameRank(dotNode, dotTagNode);
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
      return Graphviz.fromString(dotGraph.toString());
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
            return GH.bold(nodeAlias(targetNode.getId()));
          } else {
            return GH.bold(text);
          }
        }
      }
    }

    public GraphEntityColorScheme colorSchemeForNode(LoomNodeWrapper node) {
      return Objects.requireNonNull(nodeColorings, "nodeColorings").colorSchemeForNode(node);
    }

    private GraphEntityColorSchemeProvider nodeColorings = null;

    private UuidAliasProvider uuidAliasProvider = new UuidAliasProvider();

    public String nodeAlias(UUID id) {
      return uuidAliasProvider.apply(id);
    }
  }

  public ExportContext export(LoomGraph graph) {
    var e = new ExportContext(graph);
    e.export();
    return e;
  }
}
