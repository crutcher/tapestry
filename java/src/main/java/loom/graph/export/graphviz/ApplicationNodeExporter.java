package loom.graph.export.graphviz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import guru.nidi.graphviz.attribute.Arrow;
import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.model.Compass;
import guru.nidi.graphviz.model.Factory;
import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.MutableNode;
import loom.common.json.JsonUtil;
import loom.graph.LoomNode;
import loom.graph.nodes.ApplicationNode;
import loom.graph.nodes.IPFIndex;
import loom.graph.nodes.TensorSelection;

import java.util.*;
import java.util.function.Function;

public class ApplicationNodeExporter implements GraphVisualizer.NodeTypeExporter {
  @Override
  public void exportNode(
      GraphVisualizer visualizer, GraphVisualizer.ExportContext context, LoomNode<?, ?> loomNode) {
    var gvNode = context.standardNodePrefix(loomNode);

    var appNode = (ApplicationNode) loomNode;
    var opNode = appNode.getOperationSignatureNode();

    {
      UUID operationId = appNode.getOperationId();
      String operatorAlias = context.getNodeHexAliasMap().get(operationId);
      var link =
          Link.to(
                  Factory.mutNode(loomNode.getId().toString())
                      .port(operatorAlias)
                      .port(Compass.NORTH))
              .with("weight", 0)
              .with("dir", "both")
              .with(Style.DOTTED);
      context.getExportGraph().add(Factory.mutNode(operationId.toString()).addLink(link));
    }

    gvNode.add(Shape.NOTE);
    gvNode.add(Style.FILLED);
    gvNode.add("margin", "0.08,-0.1");
    gvNode.add(Color.named(context.colorForNode(opNode.getId())).fill());

    var table =
        GH.table()
            .border(0)
            .cellspacing(0)
            .cellpadding(2)
            .add(GH.tr(GH.td().add(selectionMapToIOConnectorsTable(context, appNode.getInputs(), true))));

    if (visualizer.isFoldApplicationNodes()) {
      var descTable =
          GH.table()
              .withParent(table)
              .border(0)
              .cellborder(1)
              .cellspacing(0)
              .cellpadding(0)
              .bgcolor("white")
              .add(GH.td().colspan(2).add(GH.bold("\"%s\"".formatted(opNode.getKernel()))));

      if (!opNode.getParams().isEmpty()) {
        descTable.add(
            context.jsonToDataKeyValueTRs(
                (ObjectNode) JsonUtil.valueToJsonNodeTree(opNode.getParams())));
      }

      // Annotations
      {
        var annotationMap = new HashMap<>(appNode.getAnnotations());
        var index = (IPFIndex) annotationMap.remove(IPFIndex.ANNOTATION_TYPE);
        if (index != null) {
          // If the index is present, render it in the node.
          GH.table()
              .withParent(table)
              .border(0)
              .cellborder(1)
              .cellspacing(0)
              .cellpadding(0)
              .bgcolor("white")
              .add(context.jsonToDataKeyValueTRs((ObjectNode) JsonUtil.valueToJsonNodeTree(index)));
        }

        context.maybeRenderAnnotations(loomNode.getId().toString(), annotationMap);
      }

    } else {

      var descTable =
          GH.table()
              .withParent(table)
              .border(0)
              .cellborder(1)
              .cellspacing(0)
              .cellpadding(0)
              .bgcolor("white")
              .add(context.renderDataTypeTitle(loomNode.getTypeAlias()))
              .add(context.asDataKeyValueTR("kernel", opNode.getKernel()));

      if (!opNode.getParams().isEmpty()) {
        descTable.add(
            context.renderDataTypeTitle("params"),
            context.jsonToDataKeyValueTRs(
                (ObjectNode) JsonUtil.convertValue(opNode.getParams(), JsonNode.class)));
      }

      descTable.add(
          selectionMapToDataRows(context, "inputs", appNode.getInputs()),
          selectionMapToDataRows(context, "outputs", appNode.getOutputs()));

      context.maybeRenderAnnotations(loomNode);
    }

    selectionMapToIOConnectorsTable(context, appNode.getOutputs(), false).withParent(table);

    gvNode.add(GraphVisualizer.asHtmlLabel(table));

    tensorSelectionMapEdges(context, appNode, appNode.getInputs(), true);
    tensorSelectionMapEdges(context, appNode, appNode.getOutputs(), false);
  }

  protected static void tensorSelectionMapEdges(
      GraphVisualizer.ExportContext context,
      LoomNode<?, ?> node,
      Map<String, List<TensorSelection>> inputs,
      boolean isInput) {
    UUID nodeId = node.getId();

    var desc = isInput ? "inputs" : "outputs";

    String lastSelNodeId = null;

    for (var entry : inputs.entrySet()) {
      var key = entry.getKey();
      var slices = entry.getValue();
      for (int idx = 0; idx < slices.size(); idx++) {
        var slice = slices.get(idx);

        UUID tensorId = slice.getTensorId();

        LoomNode<?, ?> node1 = context.getGraph().assertNode(tensorId);
        String targetNodeColor = context.colorForNode(node1.getId());
        var targetColor = Color.named(targetNodeColor);
        var color = targetColor.and(targetColor, Color.BLACK);

        Function<Link, Link> config =
            link -> link.with("penwidth", "6").with(color).with(Arrow.NORMAL);

        var selNode =
            Factory.mutNode(node.getId() + "#" + key + "#" + idx)
                .add(Shape.BOX_3D)
                .add(Style.FILLED)
                .add(Color.named(context.colorForNode(tensorId)).fill());
        selNode.add(
            GraphVisualizer.asHtmlLabel(
                GH.table()
                    .border(0)
                    .cellborder(0)
                    .cellspacing(0)
                    .cellpadding(0)
                    .add(
                        context.asDataKeyValueTR("range", slice.getRange().toRangeString()),
                        context.asDataKeyValueTR("shape", slice.getRange().toShapeString()))));
        context.getExportGraph().add(selNode);

        var nodeProxy = Factory.mutNode(nodeId.toString());
        var tensorProxy = Factory.mutNode(tensorId.toString());

        String port = "%s.%s.%d".formatted(desc, key, idx);

        var exportGraph = context.getExportGraph();

        if (isInput) {
          exportGraph.add(
              tensorProxy.addLink(
                  config.apply(
                      tensorProxy.port(Compass.SOUTH).linkTo(selNode.port(Compass.NORTH)))));

          exportGraph.add(
              selNode.addLink(
                  config
                      .apply(
                          selNode
                              .port(Compass.SOUTH)
                              .linkTo(nodeProxy.port(port).port(Compass.NORTH)))
                      .with("weight", 4)));

        } else {
          exportGraph.add(
              nodeProxy.addLink(
                  config
                      .apply(
                          nodeProxy
                              .port(port)
                              .port(Compass.SOUTH)
                              .linkTo(selNode.port(Compass.NORTH)))
                      .with("weight", 4)));

          exportGraph.add(
              selNode.addLink(
                  config.apply(
                      selNode.port(Compass.SOUTH).linkTo(tensorProxy.port(Compass.NORTH)))));
        }

        // Force the selection nodes to cluster and layout in call order.
        if (lastSelNodeId != null) {
          MutableNode fromProxy = Factory.mutNode(lastSelNodeId);
          MutableNode toProxy = Factory.mutNode(selNode.name().toString());

          context.sameRank(fromProxy.name().toString(), toProxy.name().toString());

          context.getExportGraph().add(fromProxy.addLink(Link.to(toProxy).with(Style.INVIS)));
        }
        lastSelNodeId = selNode.name().toString();
      }
    }
  }

  /**
   * Render the I/O pad connectors for the top and bottom of the node.
   *
   * @param context The export context.
   * @param selectionMap The selection map.
   * @param isInput Whether this is an input or output pad.
   * @return The table.
   */
  public GH.TableWrapper selectionMapToIOConnectorsTable(
      GraphVisualizer.ExportContext context,
      Map<String, List<TensorSelection>> selectionMap,
      boolean isInput) {

    var inputTable =
        GH.table()
            .align(GH.HorizontalAlign.LEFT)
            .fixedsize(true)
            .border(0)
            .cellborder(1)
            .cellspacing(0)
            .cellpadding(1);

    var padRow = GH.tr();
    var labelRow = GH.tr();
    var keys = selectionMap.keySet().stream().sorted().toList();
    for (int k = 0; k < keys.size(); ++k) {
      var key = keys.get(k);
      var argSize = selectionMap.get(key).size();

      if (k != 0) {
        // Space between args.
        padRow.add(GH.td().border(0).add(" "));
        labelRow.add(GH.td().border(0).add(" "));
      }

      for (int i = 0; i < argSize; ++i) {
        var sel = selectionMap.get(key).get(i);
        var color = context.colorForNode(sel.getTensorId());

        GH.td(GH.bold(Integer.toString(i)))
            .withParent(padRow)
            .port("%s.%s.%d".formatted(isInput ? "inputs" : "outputs", key, i))
            .bgcolor(color);
      }

      labelRow.add(GH.td(GH.bold(key)).bgcolor("white").colspan(argSize));
    }

    if (isInput) {
      inputTable.add(padRow, labelRow);
    } else {
      inputTable.add(labelRow, padRow);
    }

    return inputTable;
  }

  public List<GH.ElementWrapper<?>> selectionMapToDataRows(
      GraphVisualizer.ExportContext context,
      String selectionDesc,
      Map<String, List<TensorSelection>> selectionMap) {
    List<GH.ElementWrapper<?>> rows = new ArrayList<>();

    rows.add(context.renderDataTypeTitle(selectionDesc));

    var keys = selectionMap.keySet().stream().sorted().toList();
    for (var key : keys) {
      var sels = selectionMap.get(key);

      var tr = GH.tr();
      rows.add(tr);

      tr.add(GH.td().add(GH.bold(key)));

      var selTable = GH.table().cellborder(1).cellspacing(0);
      tr.add(GH.td().cellpadding(0).add(selTable));

      selTable.border(0).cellpadding(2);

      for (int i = 0; i < sels.size(); ++i) {
        var sel = sels.get(i);
        var color = context.colorForNode(sel.getTensorId());

        selTable.add(
            GH.tr(
                GH.td().bgcolor(color).add(GH.bold(Integer.toString(i))),
                GH.td().add(context.nodeAliasTable(sel.getTensorId())),
                GH.td().add(GH.bold(sel.getRange().toRangeString())),
                GH.td().add(GH.bold(sel.getRange().toShapeString()))));
      }
    }
    return rows;
  }
}
