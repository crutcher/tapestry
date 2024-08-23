package org.tensortapestry.loom.graph.export.graphviz;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.tensortapestry.common.collections.EnumerationUtils;
import org.tensortapestry.graphviz.DotGraph;
import org.tensortapestry.graphviz.FormatUtils;
import org.tensortapestry.graphviz.GraphvizAttribute;
import org.tensortapestry.graphviz.HtmlLabel;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.dialects.tensorops.*;

@SuperBuilder
public class OperationExpressionLoomGraphvizExporter extends LoomGraphvizExporter {

  @Builder.Default
  private final boolean labelOperationGroups = true;

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

    if (labelOperationGroups) {
      var clusterGroupDescription = "Operation";
      if (isIoOp) {
        List<String> mods = new ArrayList<>();
        if (!op.getInputs().isEmpty()) {
          mods.add("Sink");
        }
        if (!op.getOutputs().isEmpty()) {
          mods.add("Source");
        }
        clusterGroupDescription = String.join(" / ", mods) + " " + clusterGroupDescription;
      }
      clusterGroupDescription += " [%s]".formatted(op.getKernel());
      {
        var label = op.getLabel();
        if (label != null && !label.isEmpty() && !label.equals(op.getKernel())) {
          clusterGroupDescription += " : \"%s\"".formatted(label);
        }
      }

      var labelCluster = outer.createCluster(op.getId() + "_op_label_cluster");
      labelCluster
        .getAttributes()
        .set(GraphvizAttribute.LABELJUST, "l")
        .set(GraphvizAttribute.LABELLOC, "t")
        .set(GraphvizAttribute.PERIPHERIES, 0)
        .set(
          GraphvizAttribute.LABEL,
          HtmlLabel.from(
            GH.font().color("black").pointSize(48).add(GH.bold(clusterGroupDescription))
          )
        );

      outer = labelCluster;
    }

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

  protected DotGraph.Node exportTensorNode(ExportContext context, TensorNode tensorNode) {
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

    return entityContext.getDotNode();
  }

  protected DotGraph.Node exportOperationNode(
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

    var labelTable = entityContext.getLabelTable();

    labelTable.add(context.asDataKeyValueTr("Kernel", operationNode.getKernel()));

    var selTable = ioSelectionTable(context, operationNode.getInputs(), operationNode.getOutputs());
    if (selTable != null) {
      labelTable.add(GH.td().colspan(2).add(selTable));
    }

    context.renderNodeTags(operationNode, opCluster);

    return entityContext.getDotNode();
  }

  protected String selectionDotNodeId(String nodeId, String desc, String key, int idx) {
    return nodeId + "_" + desc + "_sel_" + key + "_" + idx;
  }

  protected void exportSelectionMap(
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

        String selNodeId = selectionDotNodeId(nodeId, ioDesc, key, idx);

        UUID tensorId = tensorSelection.getTensorId();
        var tensorNode = context.getLoomGraph().assertNode(tensorId, TensorNode.class);

        var isTotalSelection = tensorNode.getRange().equals(tensorSelection.getRange());

        var tensorColor = context.colorSchemeForNode(tensorNode).getPrimary();

        var dotSelectionNode = selCluster.createNode(selNodeId);
        dotSelectionNode
          .set(GraphvizAttribute.SHAPE, "box3d")
          .set(GraphvizAttribute.PENWIDTH, 2)
          .set(GraphvizAttribute.FILLCOLOR, tensorColor)
          .set(GraphvizAttribute.STYLE, "filled")
          .set(GraphvizAttribute.GRADIENTANGLE, 315)
          .set(GraphvizAttribute.MARGIN, 0.15);

        if (isTotalSelection) {
          dotSelectionNode.set(
            GraphvizAttribute.LABEL,
            HtmlLabel.from(
              GH
                .table()
                .bgcolor("white")
                .border(0)
                .cellborder(1)
                .cellspacing(0)
                .cellpadding(2)
                .add(GH.bold(selDesc + " "))
            )
          );
        } else {
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
        }

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

          if (!isRouteProxy) {
            routeEdge.set(GraphvizAttribute.WEIGHT, 10);
          }

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

  @Nullable protected GH.TableWrapper ioSelectionTable(
    ExportContext context,
    Map<String, List<TensorSelection>> inputs,
    Map<String, List<TensorSelection>> outputs
  ) {
    if (inputs.size() == 0 && outputs.size() == 0) {
      return null;
    }

    var table = GH.table();

    table.border(0).cellborder(1).cellpadding(2).cellspacing(0);

    if (inputs.size() > 0) {
      table.add(GH.td().colspan(3).add(GH.bold("Inputs")));

      for (var entry : inputs.entrySet()) {
        var key = entry.getKey();
        var selections = entry.getValue();

        for (var iv : EnumerationUtils.enumerate(selections)) {
          var idx = iv.getKey();
          var sel = iv.getValue();

          var tr = GH.tr();
          table.add(tr);

          if (idx == 0) {
            tr.add(GH.td().rowspan(selections.size()).add(GH.bold(key)));
          }

          tr.add(
            GH.td(GH.bold(context.nodeAlias(sel.getTensorId()))),
            GH.td(GH.bold(sel.getRange().toString()))
          );
        }
      }
    }

    if (outputs.size() > 0) {
      table.add(GH.td().colspan(3).add(GH.bold("Outputs")));

      for (var entry : outputs.entrySet()) {
        var key = entry.getKey();
        var selections = entry.getValue();

        for (var iv : EnumerationUtils.enumerate(selections)) {
          var idx = iv.getKey();
          var sel = iv.getValue();

          var tr = GH.tr();
          table.add(tr);

          if (idx == 0) {
            tr.add(GH.td().rowspan(selections.size()).add(GH.bold(key)));
          }

          tr.add(
            GH.td(GH.bold(context.nodeAlias(sel.getTensorId()))),
            GH.td(GH.bold(sel.getRange().toString()))
          );
        }
      }
    }

    return table;
  }
}
