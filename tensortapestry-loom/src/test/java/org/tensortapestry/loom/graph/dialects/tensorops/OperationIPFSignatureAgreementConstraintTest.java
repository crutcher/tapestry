package org.tensortapestry.loom.graph.dialects.tensorops;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.CommonAssertions;
import org.tensortapestry.loom.graph.dialects.tensorops.constraints.OperationIPFSignatureAgreementConstraint;
import org.tensortapestry.zspace.ZPoint;
import org.tensortapestry.zspace.ZRange;
import org.tensortapestry.zspace.ZRangeProjectionMap;

@SuppressWarnings("unused")
public class OperationIPFSignatureAgreementConstraintTest implements CommonAssertions {

  @Test
  @SuppressWarnings("ConstantConditions")
  public void test_valid_short() {
    var env = ApplicationExpressionDialect.ENVIRONMENT;
    env = env.toBuilder().constraint(new OperationIPFSignatureAgreementConstraint()).build();

    var graph = env.newGraph();

    var tensorA = TensorNode
      .on(graph)
      .label("A")
      .body(b -> b.dtype("int32").range(ZRange.builder().start(-10, 4).shape(3, 4).build()))
      .build();

    var tensorB = TensorNode.on(graph).label("B").body(b -> b.dtype("int32").shape(4, 5)).build();

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
      op.viewBodyAs(OperationNode.Body.class).getOutputs().get("z").getFirst().getTensorId(),
      TensorNode::wrap
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
  }
}
