package org.tensortapestry.weft.metakernels;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.CommonAssertions;
import org.tensortapestry.loom.graph.dialects.tensorops.*;
import org.tensortapestry.loom.graph.tools.GraphViewer;
import org.tensortapestry.zspace.ZPoint;
import org.tensortapestry.zspace.ZRange;

public class MKTest implements CommonAssertions {

  @Test
  public void test_add() {
    var graph = ApplicationExpressionDialect.newGraph();

    String dtype = "float32";

    var tensorA = TensorNode
      .on(graph)
      .label("A")
      .body(b -> b.dtype(dtype).shape(10, 10, 10))
      .build();

    var tensorB = TensorNode.on(graph).label("B").body(b -> b.dtype(dtype).shape(1, 10)).build();

    var tensorC = CommonMetaKernels.ADD
      .on(graph)
      .input("tensors", tensorA, tensorB)
      .apply()
      .getResult()
      .withLabel("C");

    assertThat(tensorC.getShape()).isEqualTo(ZPoint.of(10, 10, 10));
    assertThat(tensorC.getDtype()).isEqualTo(dtype);

    graph.validate();
  }

  @Test
  @SuppressWarnings({ "unused", "ConstantConditions" })
  public void test_Linear() {
    var graph = ApplicationExpressionDialect.newGraph();

    var dtype = "float32";

    var input = TensorNode
      .on(graph)
      .label("input")
      .body(b -> b.dtype(dtype).range(ZRange.builder().start(-40, 20).shape(100, 12).build()))
      .build();

    var weights = TensorNode.on(graph).label("W").body(b -> b.dtype(dtype).shape(12, 32)).build();

    var bias = TensorNode.on(graph).label("bias").body(b -> b.dtype(dtype).shape(32)).build();

    var linearOp = CommonMetaKernels.LINEAR
      .on(graph)
      .input("x", input)
      .input("A", weights)
      .input("b", bias)
      .apply();

    var z = linearOp.getResult().withLabel("Z");

    var reluOp = CommonMetaKernels.RELU.on(graph).input("tensor", z).apply();

    var y = reluOp.getResult().withLabel("Y");

    var reshard = true;
    if (reshard) {
      // Re-shard 2 ways.
      linearOp.getApplicationNodes().toList().forEach(graph::removeNode);
      reluOp.getApplicationNodes().toList().forEach(graph::removeNode);
      var linearIndex = linearOp
        .getTags()
        .get(TensorOpNodes.IPF_INDEX_ANNOTATION_TYPE)
        .viewAs(ZRange.class);
      var reluIndex = reluOp
        .getTags()
        .get(TensorOpNodes.IPF_INDEX_ANNOTATION_TYPE)
        .viewAs(ZRange.class);

      assertThat(linearIndex).isEqualTo(reluIndex);
      var indexShards = Arrays
        .stream(linearIndex.split(0, 50))
        .flatMap(i -> Arrays.stream(i.split(1, 16)))
        .toList();

      OperationUtils.createIpfShards(linearOp, indexShards);
      OperationUtils.createIpfShards(reluOp, indexShards);
    }

    assertThat(z.getShape()).isEqualTo(ZPoint.of(100, 32));
    assertThat(z.getDtype()).isEqualTo(dtype);

    graph.validate();
  }

  @SuppressWarnings({ "unused", "ConstantConditions" })
  public static void main(String[] args) {
    var graph = ApplicationExpressionDialect.newGraph();

    var dtype = "float32";

    var input = TensorNode
      .on(graph)
      .label("input")
      .body(b -> b.dtype(dtype).range(ZRange.builder().start(-40, 20).shape(100, 12).build()))
      .build();
    CommonMetaKernels.importTensor(graph, input);

    var w1 = TensorNode.on(graph).label("W1").body(b -> b.dtype(dtype).shape(12, 32)).build();
    CommonMetaKernels.importTensor(graph, w1);

    var b1 = TensorNode.on(graph).label("bias1").body(b -> b.dtype(dtype).shape(32)).build();
    CommonMetaKernels.importTensor(graph, b1);

    var l1 = CommonMetaKernels.LINEAR
      .on(graph)
      .input("x", input)
      .input("A", w1)
      .input("b", b1)
      .apply();

    var z1 = l1.getResult().withLabel("Z1");

    var relu1 = CommonMetaKernels.RELU.on(graph).input("tensor", z1).apply();
    var y1 = relu1.getResult().withLabel("Y1");
    assert y1.getRange().equals(ZRange.builder().shape(100, 12).build());

    var w2 = TensorNode.on(graph).label("W2").body(b -> b.dtype(dtype).shape(32, 8)).build();
    CommonMetaKernels.importTensor(graph, w2);

    var b2 = TensorNode.on(graph).label("bias2").body(b -> b.dtype(dtype).shape(8)).build();
    CommonMetaKernels.importTensor(graph, b2);

    var l2 = CommonMetaKernels.LINEAR
      .on(graph)
      .input("x", relu1.getResult())
      .input("A", w2)
      .input("b", b2)
      .apply();

    var z2 = l2.getResult().withLabel("Z");

    var relu2 = CommonMetaKernels.RELU.on(graph).input("tensor", z2).apply();

    var y = relu2.getResult().withLabel("Y");
    CommonMetaKernels.exportTensor(graph, y);

    var reshard = true;
    if (reshard) {
      {
        l1.getApplicationNodes().toList().forEach(graph::removeNode);
        relu1.getApplicationNodes().toList().forEach(graph::removeNode);
        var linearIndex = l1
          .getTags()
          .get(TensorOpNodes.IPF_INDEX_ANNOTATION_TYPE)
          .viewAs(ZRange.class);
        var reluIndex = relu1
          .getTags()
          .get(TensorOpNodes.IPF_INDEX_ANNOTATION_TYPE)
          .viewAs(ZRange.class);
        assert linearIndex.equals(reluIndex);

        var indexShards = Arrays
          .stream(linearIndex.split(0, 50))
          .flatMap(i -> Arrays.stream(i.split(1, 16)))
          .toList();

        OperationUtils.createIpfShards(l1, indexShards);
        OperationUtils.createIpfShards(relu1, indexShards);
      }
      {
        l2.getApplicationNodes().toList().forEach(graph::removeNode);
        relu2.getApplicationNodes().toList().forEach(graph::removeNode);
        var linearIndex = l2
          .getTags()
          .get(TensorOpNodes.IPF_INDEX_ANNOTATION_TYPE)
          .viewAs(ZRange.class);
        var reluIndex = relu2
          .getTags()
          .get(TensorOpNodes.IPF_INDEX_ANNOTATION_TYPE)
          .viewAs(ZRange.class);
        assert linearIndex.equals(reluIndex);

        var indexShards = Arrays
          .stream(linearIndex.split(0, 50))
          .flatMap(i -> Arrays.stream(i.split(1, 4)))
          .toList();

        OperationUtils.createIpfShards(l2, indexShards);
        OperationUtils.createIpfShards(relu2, indexShards);
      }
    }

    graph.validate();

    GraphViewer.graphViewer(graph);
  }
}
