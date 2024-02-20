package org.tensortapestry.loom.graph.export.graphviz;

import java.util.*;
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
    var application = ApplicationNode.wrap(appNode);
    var operation = application.getOperationNode();

    var operationColor = context.colorSchemeForNode(operation.getId()).getKey();

    DotGraph dotGraph = context.getDotGraph();
    var dotCluster = dotGraph.createCluster("app_%s".formatted(appNode.getId()));
    dotCluster
      .getAttributes()
      .set(GraphvizAttribute.NODESEP, 0.2)
      .set(GraphvizAttribute.RANKSEP, 0.2)
      .set(GraphvizAttribute.FILLCOLOR, operationColor.brighter())
      .set(GraphvizAttribute.STYLE, "filled, dashed, rounded");

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

    context.maybeRenderAnnotations(appNode, dotCluster);

    GH.TableWrapper labelTable = GH
      .table()
      .bgcolor("white")
      .border(0)
      .cellborder(0)
      .cellspacing(0)
      .add(context.renderDataTable(appNode.getTypeAlias(), appNode.viewBodyAsJsonNode()));

    dotNode.set(GraphvizAttribute.LABEL, HtmlLabel.from(labelTable));

    selectionMapNodes(context, dotCluster, application, dotNode, application.getInputs(), true);
    selectionMapNodes(context, dotCluster, application, dotNode, application.getOutputs(), false);
  }

  protected static void selectionMapNodes(
    GraphVisualizer.ExportContext context,
    DotGraph.Cluster dotCluster,
    ApplicationNode application,
    DotGraph.Node dotNode,
    Map<String, List<TensorSelection>> inputs,
    boolean isInput
  ) {
    var dotGraph = context.getDotGraph();

    UUID nodeId = application.getId();

    var ioDesc = isInput ? "input" : "output";
    var selCluster = dotCluster.createCluster("%s_%s".formatted(nodeId, ioDesc));

    selCluster
      .getAttributes()
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

        var routeId = "%s_route_%s_%s".formatted(application.getOperationId(), ioDesc, tensorId);
        var routeNode = context.getDotGraph().assertLookup(routeId, DotGraph.Node.class);

        DotGraph.Edge routeEdge;
        DotGraph.Edge selEdge;

        if (isInput) {
          routeEdge = dotGraph.createEdge(routeNode, dotSelectionNode);
          selEdge = dotCluster.createEdge(dotSelectionNode, dotNode);
        } else {
          selEdge = dotCluster.createEdge(dotNode, dotSelectionNode);
          routeEdge = dotGraph.createEdge(dotSelectionNode, routeNode);
        }

        for (var e : List.of(routeEdge, selEdge)) {
          e.set(GraphvizAttribute.COLOR, tensorColor).set(GraphvizAttribute.PENWIDTH, 12);
        }

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
