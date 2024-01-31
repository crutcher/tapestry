package org.tensortapestry.loom.graph.dialects.tensorops;

import guru.nidi.graphviz.engine.Format;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.tensortapestry.common.testing.BaseTestClass;
import org.tensortapestry.loom.graph.CommonEnvironments;
import org.tensortapestry.loom.graph.export.graphviz.GraphVisualizer;
import org.tensortapestry.zspace.ZPoint;
import org.tensortapestry.zspace.ZRange;
import org.tensortapestry.zspace.ZRangeProjectionMap;

@SuppressWarnings("unused")
public class IPFSignatureAgreementConstraintTest extends BaseTestClass {

  @Test
  @SuppressWarnings("ConstantConditions")
  public void test_valid_short() {
    var env = CommonEnvironments.expressionEnvironment();
    env.addConstraint(new IPFSignatureAgreementConstraint());

    var graph = env.newGraph();

    var tensorA = TensorNode
      .builder(graph)
      .label("A")
      .configure(b -> b.dtype("int32").range(ZRange.builder().start(-10, 4).shape(3, 4).build()))
      .build();

    var tensorB = TensorNode
      .builder(graph)
      .label("B")
      .configure(b -> b.dtype("int32").shape(4, 5))
      .build();

    var op = OperationUtils.applyRelativeSignature(
      graph,
      "matmul",
      IPFSignature
        .builder()
        .input(
          "x",
          ZRangeProjectionMap
            .builder()
            .affineMap(new int[][] { { 1, 0 }, { 0, 0 } })
            .shape(ZPoint.of(1, tensorA.getShape().get(1)))
        )
        .input(
          "y",
          ZRangeProjectionMap
            .builder()
            .affineMap(new int[][] { { 0, 0 }, { 0, 1 } })
            .shape(ZPoint.of(tensorB.getShape().get(0), 1))
        )
        .output("z", ZRangeProjectionMap.builder().affineMap(new int[][] { { 1, 0 }, { 0, 1 } }))
        .build(),
      inputs -> {
        var x = inputs.get("x").getFirst().getRange().getShape().get(0);
        var y = inputs.get("y").getFirst().getRange().getShape().get(1);
        return ZRange.newFromShape(x, y);
      },
      index -> List.of(index.split(1, 2)),
      Map.of(
        "x",
        List.of(TensorSelection.from(tensorA)),
        "y",
        List.of(TensorSelection.from(tensorB))
      ),
      Map.of("z", List.of("int32")),
      null
    );

    assertThat(graph.byType(OperationNode.TYPE).stream().count()).isEqualTo(1);

    var tensorC = graph.assertNode(
      TensorNode::wrap,
      op.viewBodyAs(OperationNode.Body.class).getOutputs().get("z").getFirst().getTensorId()
    );

    assertThat(tensorC.getDtype()).isEqualTo("int32");
    assertThat(tensorC.getRange()).isEqualTo(ZRange.newFromShape(3, 5));
    assertThat(tensorC.getLabel()).isEqualTo("matmul/z[0]");

    var matmulResult = TensorNode.wrap(
      graph.assertNode(
        op.viewBodyAs(OperationNode.Body.class).getOutputs().get("z").getFirst().getTensorId()
      )
    );

    OperationUtils.applyRelativeSignature(
      graph,
      "row_sum",
      IPFSignature
        .builder()
        .input(
          "tensor",
          ZRangeProjectionMap
            .builder()
            .affineMap(new int[][] { { 1 }, { 0 } })
            .shape(ZPoint.of(1, matmulResult.getShape().get(1)))
            .build()
        )
        .output("result", ZRangeProjectionMap.builder().affineMap(new int[][] { { 1 } }).build())
        .build(),
      inputs -> {
        var x = inputs.get("tensor").getFirst().getRange().getShape().get(0);
        return ZRange.newFromShape(x);
      },
      index -> List.of(index.split(0, 2)),
      Map.of("tensor", List.of(TensorSelection.from(matmulResult))),
      Map.of("result", List.of("int32")),
      Map.of("axis", 1)
    );
    graph.validate();

    if (false) {
      // This is for dev on the graphviz stuff; it should be moved.
      var exporter = GraphVisualizer.buildDefault();
      var export = exporter.export(graph);
      var gv = export.getGraphviz();

      // System.out.println(export.getExportGraph());

      var img = gv.render(Format.PNG).toImage();

      System.out.println("here");
    }
  }
}
