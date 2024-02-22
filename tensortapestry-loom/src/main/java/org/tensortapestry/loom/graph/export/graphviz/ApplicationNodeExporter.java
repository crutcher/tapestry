package org.tensortapestry.loom.graph.export.graphviz;

import java.awt.*;
import java.util.*;
import java.util.List;
import org.tensortapestry.graphviz.*;
import org.tensortapestry.loom.graph.LoomNode;
import org.tensortapestry.loom.graph.dialects.tensorops.*;

public class ApplicationNodeExporter implements GraphVisualizer.NodeTypeExporter {

  @Override
  public void exportNode(
    GraphVisualizer visualizer,
    GraphVisualizer.ExportContext context,
    LoomNode appNode
  ) {
    DotGraph dotGraph = context.getDotGraph();
    var application = ApplicationNode.wrap(appNode);
    var operation = application.getOperationNode();

    var opCluster = dotGraph.assertLookup(
      operation.getId() + "_op_cluster",
      DotGraph.Cluster.class
    );

    Color operationColor = context.colorSchemeForNode(operation.getId()).getKey();

    var clusterColor =
      "%s:%s".formatted(
          FormatUtils.colorToRgbaString(operationColor.brighter().brighter().brighter()),
          FormatUtils.colorToRgbaString(operationColor.brighter().brighter())
        );

    var dotCluster = opCluster.createCluster("app_%s".formatted(appNode.getId()));
    dotCluster
      .getAttributes()
      .set(GraphvizAttribute.NODESEP, 0.2)
      .set(GraphvizAttribute.RANKSEP, 0.2)
      .set(GraphvizAttribute.PENWIDTH, 2)
      .set(GraphvizAttribute.BGCOLOR, clusterColor)
      .set(GraphvizAttribute.GRADIENTANGLE, 315)
      .set(GraphvizAttribute.STYLE, "filled, dashed, bold, rounded");

    var dotOpNode = dotGraph.assertLookup(operation.getId().toString(), DotGraph.Node.class);

    var dotNode = dotCluster.createNode(appNode.getId().toString());
    context.decoratePrimaryNode(appNode, dotNode);
    dotNode
      .set(GraphvizAttribute.SHAPE, "note")
      .set(GraphvizAttribute.STYLE, "filled")
      .set(GraphvizAttribute.FILLCOLOR, operationColor);

    dotGraph.sameRank(dotNode, dotOpNode);

    dotGraph
      .createEdge(dotNode, dotOpNode)
      .set(GraphvizAttribute.CONSTRAINT, false)
      .set(GraphvizAttribute.STYLE, "invis");

    context.renderTags(appNode, dotCluster);

    GH.TableWrapper labelTable = GH
      .table()
      .bgcolor("white")
      .border(0)
      .cellborder(0)
      .cellspacing(0)
      .add(context.renderDataTable(appNode.getTypeAlias(), appNode.viewBodyAsJsonNode()));

    dotNode.set(GraphvizAttribute.LABEL, HtmlLabel.from(labelTable));

    var useRouteNodes = operation.getApplicationNodes().stream().count() > 1;

    selectionMapNodes(
      context,
      dotCluster,
      application,
      dotNode,
      application.getInputs(),
      useRouteNodes,
      true
    );
    selectionMapNodes(
      context,
      dotCluster,
      application,
      dotNode,
      application.getOutputs(),
      useRouteNodes,
      false
    );
  }

  protected static void selectionMapNodes(
    GraphVisualizer.ExportContext context,
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

        var tensorColor = context.getPrimaryColorForNode(tensorId);

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
                GH.tr(GH.bold(selDesc), context.nodeAliasTable(tensorId)),
                context.asDataKeyValueTr("range", tensorSelection.getRange().toRangeString()),
                context.asDataKeyValueTr("shape", tensorSelection.getRange().toShapeString())
              )
          )
        );

        DotGraph.Node sourceNode;
        if (useRouteNodes) {
          var routeId = "%s_route_%s_%s".formatted(application.getOperationId(), ioDesc, tensorId);
          sourceNode = context.getDotGraph().assertLookup(routeId, DotGraph.Node.class);
        } else {
          sourceNode = context.getDotGraph().assertLookup(tensorId.toString(), DotGraph.Node.class);
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
          var last = selCluster.getNodes().get(selCluster.getNodes().indexOf(dotSelectionNode) - 1);
          selCluster
            .createEdge(last, dotSelectionNode)
            .set(GraphvizAttribute.STYLE, "invis")
            .set(GraphvizAttribute.WEIGHT, 10);
        }
      }
    }
  }
}
