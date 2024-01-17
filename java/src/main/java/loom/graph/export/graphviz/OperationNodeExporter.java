package loom.graph.export.graphviz;

import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.attribute.Style;
import loom.graph.LoomNode;

public class OperationNodeExporter implements GraphVisualizer.NodeTypeExporter {
  @Override
  public void exportNode(
      GraphVisualizer visualizer, GraphVisualizer.ExportContext context, LoomNode<?, ?> loomNode) {
    var gvNode = context.standardNodePrefix(loomNode);
    context.maybeRenderAnnotations(loomNode);

    gvNode.add(Shape.TAB);
    gvNode.add(Style.FILLED);
    gvNode.add(Color.named(context.colorForNode(loomNode.getId())).fill());

    gvNode.add(
        GraphVisualizer.asHtmlLabel(
            GH.table()
                .bgcolor("white")
                .border(0)
                .cellborder(0)
                .cellspacing(0)
                .add(
                    context.renderDataTable(
                        loomNode.getTypeAlias(), loomNode.getBodyAsJsonNode()))));
  }
}
