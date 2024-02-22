package org.tensortapestry.loom.graph.export.graphviz;

import org.tensortapestry.graphviz.GraphvizAttribute;
import org.tensortapestry.graphviz.HtmlLabel;
import org.tensortapestry.loom.graph.LoomNode;
import org.tensortapestry.loom.graph.dialects.tensorops.TensorNode;

public class TensorNodeExporter implements GraphVisualizer.NodeTypeExporter {

  @Override
  public void exportNode(
    GraphVisualizer visualizer,
    GraphVisualizer.ExportContext context,
    LoomNode loomNode
  ) {
    loomNode.assertType(TensorNode.TYPE);
    var tensorData = loomNode.viewBodyAs(TensorNode.Body.class);

    var colorScheme = context.colorSchemeForNode(loomNode.getId());

    var dotNode = context.createPrimaryNode(loomNode);
    dotNode
      .set(GraphvizAttribute.SHAPE, "box3d")
      .set(GraphvizAttribute.STYLE, "filled")
      .set(GraphvizAttribute.FILLCOLOR, colorScheme.getKey())
      .set(GraphvizAttribute.GRADIENTANGLE, 315)
      .set(GraphvizAttribute.MARGIN, 0.2);

    context.renderTags(loomNode);

    GH.TableWrapper labelTable = GH
      .table()
      .bgcolor("white")
      .border(1)
      .cellborder(0)
      .cellspacing(0)
      .add(
        GH
          .td()
          .colspan(2)
          .align(GH.TableDataAlign.LEFT)
          .add(GH.font().add(GH.bold(" %s ".formatted(loomNode.getTypeAlias())))),
        context.asDataKeyValueTr("dtype", tensorData.getDtype()),
        context.asDataKeyValueTr("range", tensorData.getRange().toRangeString()),
        context.asDataKeyValueTr("shape", tensorData.getRange().toShapeString())
      );

    dotNode.set(GraphvizAttribute.LABEL, HtmlLabel.from(labelTable));
  }
}
