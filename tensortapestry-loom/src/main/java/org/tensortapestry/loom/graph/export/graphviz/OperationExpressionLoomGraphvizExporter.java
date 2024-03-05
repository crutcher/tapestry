package org.tensortapestry.loom.graph.export.graphviz;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.tensortapestry.graphviz.DotGraph;
import org.tensortapestry.graphviz.FormatUtils;
import org.tensortapestry.graphviz.GraphvizAttribute;
import org.tensortapestry.graphviz.HtmlLabel;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.dialects.tensorops.*;

@SuperBuilder
public class OperationExpressionLoomGraphvizExporter extends LoomGraphvizExporter {

  /**
   * Export an Operation node and its Cluster.
   * <p>
   * Do not export the operation selection maps.
   *
   * @param context Export context.
   * @param op Operation node.
   * @return a pair of the operation cluster and the operation node.
   */
  protected Map.Entry<DotGraph.Node, DotGraph.Cluster> exportOperationEntityNodeAndCluster(
    ExportContext context,
    OperationNode op
  ) {
    var outer = context.getDotGraph().getRoot();

    var isIoOp = op.hasTag(TensorOpNodes.IO_SEQUENCE_POINT_TYPE);

    if (isIoOp) {
      var stripes = Collections
        .nCopies(4, List.of(Color.decode("#EDED5D"), Color.decode("#A0A0A0")))
        .stream()
        .flatMap(List::stream)
        .map(FormatUtils::colorToRgbaString)
        .map(s -> s + "C0")
        .collect(Collectors.joining(":"));

      outer = outer.createCluster(op.getId() + "_op_cluster_io");
      outer
        .getAttributes()
        .set(GraphvizAttribute.MARGIN, 24)
        .set(GraphvizAttribute.PERIPHERIES, 2)
        .set(GraphvizAttribute.PENWIDTH, 4)
        .set(GraphvizAttribute.BGCOLOR, stripes)
        .set(GraphvizAttribute.STYLE, "striped");
    }

    var opCluster = outer.createCluster(op.getId() + "_op_cluster");
    opCluster
      .getAttributes()
      .set(GraphvizAttribute.MARGIN, 16)
      .set(GraphvizAttribute.PERIPHERIES, 2)
      .set(GraphvizAttribute.PENWIDTH, 4)
      .set(GraphvizAttribute.BGCOLOR, getBgColor())
      .set(GraphvizAttribute.STYLE, "filled, rounded");

    var opDotNode = exportOperationNode(context, op, opCluster);

    return Pair.of(opDotNode, opCluster);
  }

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

      exportSelectionMap(context, opCluster, null, opDotNode, op.getInputs(), true);
      exportSelectionMap(context, opCluster, null, opDotNode, op.getOutputs(), false);
    }

    return context;
  }

  protected static void exportTensorNode(ExportContext context, TensorNode tensorNode) {
    var colorScheme = context.colorSchemeForNode(tensorNode);

    var entityContext = context.createEntityNode(
      "Tensor",
      tensorNode,
      context.getDotGraph().getRoot()
    );

    entityContext
      .getDotNode()
      .set(GraphvizAttribute.SHAPE, "box3d")
      .set(GraphvizAttribute.FILLCOLOR, colorScheme.getPrimary())
      .set(GraphvizAttribute.GRADIENTANGLE, 315)
      .set(GraphvizAttribute.MARGIN, 0.2);

    context.renderNodeTags(tensorNode);

    entityContext
      .getLabelTable()
      .add(
        context.asDataKeyValueTr("dtype", tensorNode.getDtype()),
        context.asDataKeyValueTr("range", tensorNode.getRange().toRangeString()),
        context.asDataKeyValueTr("shape", tensorNode.getRange().toShapeString())
      );
  }

  protected static DotGraph.Node exportOperationNode(
    ExportContext context,
    OperationNode operationNode,
    DotGraph.SubGraph opCluster
  ) {
    var opColorScheme = context.colorSchemeForNode(operationNode);

    var entityContext = context.createEntityNode("Operation", operationNode, opCluster);

    entityContext
      .getDotNode()
      .set(GraphvizAttribute.SHAPE, "tab")
      .set(GraphvizAttribute.STYLE, "filled")
      .set(GraphvizAttribute.FILLCOLOR, opColorScheme.getPrimary());

    context.addObjectDataRows(
      entityContext.getLabelTable(),
      (ObjectNode) operationNode.viewBodyAsJsonNode()
    );

    context.renderNodeTags(operationNode, opCluster);

    return entityContext.getDotNode();
  }

  protected static void exportSelectionMap(
    ExportContext context,
    DotGraph.SubGraph dotCluster,
    @Nullable Map<UUID, DotGraph.Node> routeProxies,
    DotGraph.Node selectionParentDotNode,
    Map<String, List<TensorSelection>> selectionMap,
    boolean isInput
  ) {
    var dotGraph = context.getDotGraph();

    String nodeId = selectionParentDotNode.getId();

    var ioDesc = isInput ? "input" : "output";
    var selCluster = dotCluster.createCluster("%s_%s".formatted(nodeId, ioDesc));

    selCluster
      .getAttributes()
      .set(GraphvizAttribute.STYLE, "invis")
      .set(GraphvizAttribute.PERIPHERIES, 0)
      .set(GraphvizAttribute.RANK, "same");

    for (var entry : selectionMap.entrySet()) {
      var key = entry.getKey();
      var slices = entry.getValue();
      for (int idx = 0; idx < slices.size(); idx++) {
        var tensorSelection = slices.get(idx);

        String selDesc = "%s[%d]".formatted(key, idx);

        String selNodeId = nodeId + "_sel_" + key + "_" + idx;

        UUID tensorId = tensorSelection.getTensorId();
        var tensorNode = context.getLoomGraph().assertNode(tensorId, TensorNode.class);

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
              .border(0)
              .cellborder(1)
              .cellspacing(0)
              .add(
                GH.tr(GH.bold(selDesc), GH.bold(" " + context.nodeAlias(tensorId) + " ")),
                context.asDataKeyValueTr("range", tensorSelection.getRange().toRangeString()),
                context.asDataKeyValueTr("shape", tensorSelection.getRange().toShapeString())
              )
          )
        );

        boolean isRouteProxy;
        DotGraph.Node targetTensorRouteNode;
        if (routeProxies != null && routeProxies.containsKey(tensorId)) {
          targetTensorRouteNode = routeProxies.get(tensorId);
          isRouteProxy = true;
        } else {
          targetTensorRouteNode = dotGraph.assertLookup(tensorId.toString(), DotGraph.Node.class);
          isRouteProxy = false;
        }

        DotGraph.Edge routeEdge;
        DotGraph.Edge selEdge;

        if (isInput) {
          routeEdge = dotGraph.createEdge(targetTensorRouteNode, dotSelectionNode);
          selEdge = dotCluster.createEdge(dotSelectionNode, selectionParentDotNode);

          if (isRouteProxy) {
            routeEdge.set(GraphvizAttribute.TAILCLIP, false);
          }
        } else {
          selEdge = dotCluster.createEdge(selectionParentDotNode, dotSelectionNode);
          routeEdge = dotGraph.createEdge(dotSelectionNode, targetTensorRouteNode);

          if (isRouteProxy) {
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
