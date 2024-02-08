package org.tensortapestry.loom.graph.export.graphviz;

import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.attribute.Style;
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
        var gvNode = context.standardNodePrefix(loomNode);
        context.maybeRenderAnnotations(loomNode);

        gvNode.add(Shape.BOX_3D);
        gvNode.add(Style.FILLED);
        gvNode.add("gradientangle", 315);
        gvNode.add("penwidth", 2);
        gvNode.add("margin", 0.2);

        gvNode.add(context.colorSchemeForNode(loomNode.getId()).fill());

        gvNode.add(
                GraphVisualizer.asHtmlLabel(
                        GH
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
                                )
                )
        );
    }
}
