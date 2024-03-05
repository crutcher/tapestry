package org.tensortapestry.loom.graph.export.graphviz;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.annotation.Nullable;
import lombok.experimental.SuperBuilder;
import org.tensortapestry.graphviz.DotGraph;
import org.tensortapestry.graphviz.FormatUtils;
import org.tensortapestry.graphviz.GraphvizAttribute;
import org.tensortapestry.graphviz.HtmlLabel;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.dialects.tensorops.*;

@SuperBuilder
public class ApplicationExpressionLoomGraphvizExporter
  extends OperationExpressionLoomGraphvizExporter {

  @Override
  public ExportContext export(LoomGraph graph) {
    var context = newContext(graph);

    for (var t : graph.byType(TensorNode.class)) {
      exportTensorNode(context, t);
    }
    for (var op : graph.byType(OperationNode.class)) {
      var opPair = exportOperationEntityNodeAndCluster(context, op);
      var opDotNode = opPair.getKey();
      var opCluster = opPair.getValue();

      Map<UUID, DotGraph.Node> routeProxies = new HashMap<>();
      if (op.getApplicationNodes().stream().count() > 1) {
        routeProxies.putAll(selectionMapRibbonRouteProxies(context, op, op.getInputs(), true));
        routeProxies.putAll(selectionMapRibbonRouteProxies(context, op, op.getOutputs(), false));
      }

      for (var app : op.getApplicationNodes()) {
        exportApplicationNode(context, op, opDotNode, opCluster, routeProxies, app);
      }
    }

    return context;
  }

  protected static Map<UUID, DotGraph.Node> selectionMapRibbonRouteProxies(
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

  public static void exportApplicationNode(
    ExportContext context,
    OperationNode operation,
    DotGraph.Node operationDotNode,
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

    var appDotNode = dotCluster.createNode(application.getId().toString());
    context.decorateEntityNode(application.unwrap(), appDotNode);
    appDotNode
      .set(GraphvizAttribute.SHAPE, "note")
      .set(GraphvizAttribute.STYLE, "filled")
      .set(GraphvizAttribute.FILLCOLOR, operationColor);

    dotGraph.sameRank(appDotNode, operationDotNode);

    dotGraph
      .createEdge(appDotNode, operationDotNode)
      .set(GraphvizAttribute.CONSTRAINT, false)
      .set(GraphvizAttribute.STYLE, "invis");

    context.renderNodeTags(application.unwrap(), dotCluster);

    GH.TableWrapper labelTable = GH
      .table()
      .bgcolor("white")
      .border(0)
      .cellborder(0)
      .cellspacing(0)
      .add(context.renderDataTable(application.getTypeAlias(), application.viewBodyAsJsonNode()));

    appDotNode.set(GraphvizAttribute.LABEL, HtmlLabel.from(labelTable));

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
  }
}
