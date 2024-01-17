package loom.graph.export.graphviz;

import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.attribute.Style;
import loom.graph.LoomNode;
import loom.graph.nodes.TensorNode;

public class TensorNodeExporter implements GraphVisualizer.NodeTypeExporter {
  @Override
  public void exportNode(
      GraphVisualizer visualizer, GraphVisualizer.ExportContext context, LoomNode<?, ?> loomNode) {
    var tensorNode = (TensorNode) loomNode;
    var gvNode = context.standardNodePrefix(loomNode);
    context.maybeRenderAnnotations(loomNode);

    gvNode.add(Shape.BOX_3D);
    gvNode.add(Style.FILLED);
    gvNode.add("penwidth", 2);

    String tensorColor = context.colorForNode(loomNode.getId());
    gvNode.add(Color.named(tensorColor).fill());
    // gvNode.add(Color.rgb("#74CFFF").fill());

    gvNode.add(
        GraphVisualizer.asHtmlLabel(
            GH.table()
                .border(0)
                .cellborder(0)
                .cellspacing(0)
                .tr(context.renderDataTypeTitle(loomNode.getTypeAlias()))
                .add(
                    context.asDataKeyValueTR("dtype", tensorNode.getDtype()),
                    context.asDataKeyValueTR("range", tensorNode.getRange().toRangeString()),
                    context.asDataKeyValueTR("shape", tensorNode.getRange().toShapeString()))));
  }
}
