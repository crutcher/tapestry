package org.tensortapestry.loom.graph.export.graphviz;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import org.tensortapestry.graphviz.DotGraph;
import org.tensortapestry.graphviz.FormatUtils;
import org.tensortapestry.graphviz.GraphvizAttribute;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.dialects.tensorops.*;
import org.tensortapestry.zspace.ZRange;

@SuperBuilder
public class ApplicationExpressionLoomGraphvizExporter
  extends OperationExpressionLoomGraphvizExporter {

  @Builder.Default
  private final boolean showOperations = true;

  @Override
  public ExportContext export(LoomGraph graph) {
    var context = newContext(graph);
    DotGraph dotGraph = context.getDotGraph();

    for (var t : graph.byType(TensorNode.class)) {
      exportTensorNode(context, t);
    }

    Map<UUID, List<Map.Entry<String, ZRange>>> inputSelections = new HashMap<>();
    Map<UUID, List<Map.Entry<String, ZRange>>> outputSelections = new HashMap<>();

    for (var op : graph.byType(OperationNode.class)) {
      DotGraph.Node opDotNode = null;
      DotGraph.SubGraph opCluster = context.getDotGraph().getRoot();

      if (showOperations) {
        var opPair = exportOperationEntityNodeAndCluster(context, op);
        opDotNode = opPair.getKey();
        opCluster = opPair.getValue();
      }

      Map<UUID, DotGraph.Node> routeProxies = new HashMap<>();
      if (op.getApplicationNodes().stream().count() > 1) {
        routeProxies.putAll(renderRibbonProxies(context, op, op.getInputs(), true));
        routeProxies.putAll(renderRibbonProxies(context, op, op.getOutputs(), false));
      }

      for (var app : op.getApplicationNodes()) {
        exportApplicationNode(context, op, opDotNode, opCluster, routeProxies, app);

        // TODO: common iteration for input and output selections
        for (var entry : app.getInputs().entrySet()) {
          var tensorId = entry.getKey();
          var slices = entry.getValue();
          for (int i = 0; i < slices.size(); i++) {
            var selection = slices.get(i);
            var range = selection.getRange();

            var selDotNodeId = selectionDotNodeId(app.getId().toString(), "input", tensorId, i);

            inputSelections
              .computeIfAbsent(selection.getTensorId(), k -> new ArrayList<>())
              .add(Map.entry(selDotNodeId, range));
          }
        }
        for (var entry : app.getOutputs().entrySet()) {
          var tensorId = entry.getKey();
          var slices = entry.getValue();
          for (int i = 0; i < slices.size(); i++) {
            var selection = slices.get(i);
            var range = selection.getRange();

            var selDotNodeId = selectionDotNodeId(app.getId().toString(), "output", tensorId, i);

            outputSelections
              .computeIfAbsent(selection.getTensorId(), k -> new ArrayList<>())
              .add(Map.entry(selDotNodeId, range));
          }
        }
      }
    }

    for (var t : graph.byType(TensorNode.class)) {
      var is = inputSelections.get(t.getId());
      var os = outputSelections.get(t.getId());

      if (is == null || os == null) {
        continue;
      }

      Map<ZRange, Map.Entry<List<String>, List<String>>> matchingSelections = new HashMap<>();

      for (var inputEntry : is) {
        var inputDotNode = inputEntry.getKey();
        var inputRange = inputEntry.getValue();

        for (var outputEntry : os) {
          var outputRange = outputEntry.getValue();
          var outputDotNode = outputEntry.getKey();

          if (inputRange.equals(outputRange)) {
            var rangeMatches = matchingSelections.computeIfAbsent(
              inputRange,
              k -> new AbstractMap.SimpleEntry<>(new ArrayList<>(), new ArrayList<>())
            );

            rangeMatches.getKey().add(outputDotNode);
            rangeMatches.getValue().add(inputDotNode);
          }
        }
      }

      for (var e : matchingSelections.values()) {
        var outputs = e.getKey();
        var inputs = e.getValue();

        if (outputs.size() == 1 && inputs.size() == 1) {
          var outputDotNode = outputs.get(0);
          var inputDotNode = inputs.get(0);

          var fusionEdge = dotGraph.createEdge(
            dotGraph.assertLookup(outputDotNode, DotGraph.Node.class),
            dotGraph.assertLookup(inputDotNode, DotGraph.Node.class)
          );
          fusionEdge
            .set(GraphvizAttribute.WEIGHT, 15)
            .set(GraphvizAttribute.STYLE, "dotted")
            .set(GraphvizAttribute.PENWIDTH, 12);
        }
      }
    }

    return context;
  }

  protected Map<UUID, DotGraph.Node> renderRibbonProxies(
    ExportContext context,
    OperationNode operationNode,
    Map<String, List<TensorSelection>> selectionMap,
    boolean isInput
  ) {
    var ioDesc = isInput ? "input" : "output";

    var graph = operationNode.assertGraph();
    var operationId = operationNode.getId();

    Map<UUID, DotGraph.Node> routeProxies = new HashMap<>();

    DotGraph dotGraph = context.getDotGraph();
    var routeCluster = dotGraph.createCluster("%s_route_cluster_%s".formatted(operationId, ioDesc));
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

        var routeId = "%s_route_proxy_%s_%s".formatted(operationId, ioDesc, tensorId);

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

        routeProxies.put(tensorId, routeNode);

        DotGraph.Edge routeEdge;
        if (isInput) {
          routeEdge = dotGraph.createEdge(tensorDotNode, routeNode);
          routeEdge.set(GraphvizAttribute.HEADCLIP, false);
        } else {
          routeEdge = dotGraph.createEdge(routeNode, tensorDotNode);
          routeEdge.set(GraphvizAttribute.TAILCLIP, false);

          routeEdge.set(GraphvizAttribute.WEIGHT, 10);
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
            cur = routeCluster.createNode("%s_route_%s_%d_%d".formatted(operationId, ioDesc, r, c));

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

    return routeProxies;
  }

  protected DotGraph.Node exportApplicationNode(
    ExportContext context,
    OperationNode operation,
    @Nullable DotGraph.Node operationDotNode,
    DotGraph.SubGraph opCluster,
    @Nullable Map<UUID, DotGraph.Node> routeProxies,
    ApplicationNode application
  ) {
    DotGraph dotGraph = context.getDotGraph();

    Color operationColor = context.colorSchemeForNode(operation).getPrimary();

    var clusterColor =
      "%s:%s".formatted(
          FormatUtils.colorToRgbaString(operationColor.brighter().brighter().brighter()),
          FormatUtils.colorToRgbaString(operationColor.brighter().brighter())
        );

    var dotCluster = opCluster.createCluster("app_%s".formatted(application.getId()));
    dotCluster
      .getAttributes()
      .set(GraphvizAttribute.NODESEP, 0.2)
      .set(GraphvizAttribute.RANKSEP, 0.2)
      .set(GraphvizAttribute.PENWIDTH, 2)
      .set(GraphvizAttribute.BGCOLOR, clusterColor)
      .set(GraphvizAttribute.GRADIENTANGLE, 315)
      .set(GraphvizAttribute.STYLE, "filled, dashed, bold, rounded");

    var entityContext = context.createEntityNode("Application", application, opCluster);

    entityContext
      .getDotNode()
      .set(GraphvizAttribute.SHAPE, "note")
      .set(GraphvizAttribute.FILLCOLOR, operationColor);

    var appDotNode = entityContext.getDotNode();

    if (operationDotNode != null) {
      dotGraph.sameRank(appDotNode, operationDotNode);

      dotGraph
        .createEdge(appDotNode, operationDotNode)
        .set(GraphvizAttribute.CONSTRAINT, false)
        .set(GraphvizAttribute.STYLE, "invis");
    }

    context.renderNodeTags(application.unwrap(), dotCluster);

    var labelTable = entityContext.getLabelTable();

    labelTable.add(
      context.asDataKeyValueTr("Operation", context.nodeAlias(application.getOperationId()))
    );

    var selTable = ioSelectionTable(context, application.getInputs(), application.getOutputs());
    if (selTable != null) {
      labelTable.add(GH.td().colspan(2).add(selTable));
    }

    exportSelectionMap(
      context,
      dotCluster,
      routeProxies,
      appDotNode,
      application.getInputs(),
      true
    );
    exportSelectionMap(
      context,
      dotCluster,
      routeProxies,
      appDotNode,
      application.getOutputs(),
      false
    );

    return entityContext.getDotNode();
  }
}
