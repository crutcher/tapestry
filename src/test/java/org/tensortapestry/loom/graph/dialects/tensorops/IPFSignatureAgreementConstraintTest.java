package org.tensortapestry.loom.graph.dialects.tensorops;

import guru.nidi.graphviz.engine.Format;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.tensortapestry.loom.graph.CommonEnvironments;
import org.tensortapestry.loom.graph.export.graphviz.GraphVisualizer;
import org.tensortapestry.loom.testing.BaseTestClass;
import org.tensortapestry.loom.zspace.IndexProjectionFunction;
import org.tensortapestry.loom.zspace.ZPoint;
import org.tensortapestry.loom.zspace.ZRange;

@SuppressWarnings("unused")
public class IPFSignatureAgreementConstraintTest extends BaseTestClass {

  @Test
  public void test_valid_short() {
    var env = CommonEnvironments.expressionEnvironment();
    env.addConstraint(new IPFSignatureAgreementConstraint());

    var graph = env.newGraph();

    var tensorA = TensorNode
      .withBody(b ->
        b
          .dtype("int32")
          .range(ZRange.builder().start(ZPoint.of(-10, 4)).shape(ZPoint.of(3, 4)).build())
      )
      .label("A")
      .addTo(graph);

    var tensorB = TensorNode.withBody(b -> b.dtype("int32").shape(4, 5)).label("B").addTo(graph);

    var op = OperationUtils.applyRelativeSignature(
      graph,
      "matmul",
      IPFSignature
        .builder()
        .input(
          "x",
          IndexProjectionFunction
            .builder()
            .affineMap(new int[][] { { 1, 0 }, { 0, 0 } })
            .shape(ZPoint.of(1, tensorA.getShape().get(1)))
            .build()
        )
        .input(
          "y",
          IndexProjectionFunction
            .builder()
            .affineMap(new int[][] { { 0, 0 }, { 0, 1 } })
            .shape(ZPoint.of(tensorB.getShape().get(0), 1))
            .build()
        )
        .output(
          "z",
          IndexProjectionFunction.builder().affineMap(new int[][] { { 1, 0 }, { 0, 1 } }).build()
        )
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

    assertThat(graph.nodeScan().nodeClass(OperationSignatureNode.class).asStream().count())
      .isEqualTo(1);

    TensorNode tensorC = graph.assertNode(
      op.getOutputs().get("z").getFirst().getTensorId(),
      TensorOpNodes.TENSOR_NODE_TYPE,
      TensorNode.class
    );

    assertThat(tensorC.getDtype()).isEqualTo("int32");
    assertThat(tensorC.getRange()).isEqualTo(ZRange.newFromShape(3, 5));
    assertThat(tensorC.getLabel()).isEqualTo("matmul/z[0]");

    var matmulResult = graph.assertNode(
      op.getOutputs().get("z").getFirst().getTensorId(),
      TensorOpNodes.TENSOR_NODE_TYPE,
      TensorNode.class
    );

    OperationUtils.applyRelativeSignature(
      graph,
      "row_sum",
      IPFSignature
        .builder()
        .input(
          "tensor",
          IndexProjectionFunction
            .builder()
            .affineMap(new int[][] { { 1 }, { 0 } })
            .shape(ZPoint.of(1, matmulResult.getShape().get(1)))
            .build()
        )
        .output(
          "result",
          IndexProjectionFunction.builder().affineMap(new int[][] { { 1 } }).build()
        )
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
