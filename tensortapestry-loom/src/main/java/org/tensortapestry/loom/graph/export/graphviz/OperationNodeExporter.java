package org.tensortapestry.loom.graph.export.graphviz;

import java.util.*;
import java.util.List;

import org.tensortapestry.graphviz.DotGraph;
import org.tensortapestry.graphviz.GraphvizAttribute;
import org.tensortapestry.graphviz.HtmlLabel;
import org.tensortapestry.loom.graph.LoomNode;
import org.tensortapestry.loom.graph.dialects.tensorops.OperationNode;
import org.tensortapestry.loom.graph.dialects.tensorops.TensorOpNodes;
import org.tensortapestry.loom.graph.dialects.tensorops.TensorSelection;

public class OperationNodeExporter implements GraphVisualizer.NodeTypeExporter {

  @Override
  public void exportNode(
    GraphVisualizer visualizer,
    GraphVisualizer.ExportContext context,
    LoomNode loomNode
  ) {
    var operation = OperationNode.wrap(loomNode);

    var colorScheme = context.colorSchemeForNode(loomNode.getId());

    var opCluster = context.getDotGraph().createCluster(loomNode.getId() + "_op_cluster");
    opCluster
      .getAttributes()
      .set(GraphvizAttribute.MARGIN, 16)
      .set(GraphvizAttribute.PERIPHERIES, 2)
      .set(GraphvizAttribute.PENWIDTH, 4)
      .set(GraphvizAttribute.STYLE, "rounded");

    if (operation.hasTag(TensorOpNodes.IO_SEQUENCE_POINT_TYPE)) {
      opCluster
        .getAttributes()
        .set(GraphvizAttribute.BGCOLOR, "lightblue")
        .set(GraphvizAttribute.STYLE, "filled, rounded");
      // .set(GraphvizAttribute.BGCOLOR, "yellow:black:yellow:black:yellow:black")
      // .set(GraphvizAttribute.STYLE, "striped, dashed");
    }

    var dotNode = opCluster.createNode(loomNode.getId().toString());
    context.decoratePrimaryNode(loomNode, dotNode);
    dotNode
      .set(GraphvizAttribute.SHAPE, "tab")
      .set(GraphvizAttribute.STYLE, "filled")
      .set(GraphvizAttribute.FILLCOLOR, colorScheme.getKey());

    context.renderTags(loomNode, opCluster);

    GH.TableWrapper labelTable = GH
      .table()
      .bgcolor("white")
      .border(0)
      .cellborder(0)
      .cellspacing(0)
      .add(context.renderDataTable(loomNode.getTypeAlias(), loomNode.viewBodyAsJsonNode()));

    dotNode.set(GraphvizAttribute.LABEL, HtmlLabel.from(labelTable));

    if (operation.getApplicationNodes().stream().count() > 1) {
      routeNodes(context, operation.getId(), operation.getInputs(), true);
      routeNodes(context, operation.getId(), operation.getOutputs(), false);
    }
  }

  protected static void routeNodes(
    GraphVisualizer.ExportContext context,
    UUID operationId,
    Map<String, List<TensorSelection>> selectionMap,
    boolean isInput
  ) {
    var ioDesc = isInput ? "input" : "output";

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

        if (!routedTensors.add(tensorId)) {
          continue;
        }

        var tensorNode = dotGraph.assertLookup(tensorId.toString(), DotGraph.Node.class);

        var routeId = "%s_route_%s_%s".formatted(operationId, ioDesc, tensorId);

        var tensorColor = context.getColoringForNode(tensorId).getKey();

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
          routeEdge = dotGraph.createEdge(tensorNode, routeNode);
          routeEdge.set(GraphvizAttribute.HEADCLIP, false);
        } else {
          routeEdge = dotGraph.createEdge(routeNode, tensorNode);
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
          orderingEdge = dotGraph.createEdge(tensorNode, routeNodes.get(0));
        } else {
          orderingEdge = dotGraph.createEdge(routeNodes.get(0), tensorNode);
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
  }
}
