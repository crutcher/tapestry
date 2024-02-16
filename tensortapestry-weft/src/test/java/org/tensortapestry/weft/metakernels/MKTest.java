package org.tensortapestry.weft.metakernels;

import guru.nidi.graphviz.engine.Format;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.CommonAssertions;
import org.tensortapestry.loom.graph.CommonEnvironments;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.dialects.tensorops.*;
import org.tensortapestry.loom.graph.export.graphviz.GraphVisualizer;
import org.tensortapestry.zspace.ZPoint;
import org.tensortapestry.zspace.ZRange;

public class MKTest implements CommonAssertions {

  @SuppressWarnings("unused")
  public void debugRender(LoomGraph graph) {
    var exporter = GraphVisualizer.buildDefault();
    var export = exporter.export(graph);
    var gv = export.getGraphviz();
    var img = gv.render(Format.PNG).toImage();
  }

  @Test
  public void test_add() {
    var env = CommonEnvironments.expressionEnvironment();
    var graph = env.newGraph();

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
  @SuppressWarnings("ConstantConditions")
  public void test_Linear() {
    var env = CommonEnvironments.expressionEnvironment();
    var graph = env.newGraph();

    var reshard = false;

    var dtype = "float32";

    var input = TensorNode
      .on(graph)
      .label("input")
      .body(b -> b.dtype(dtype).shape(100, 12))
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

    if (reshard) {
      // Re-shard 2 ways.
      linearOp.getApplicationNodes().toList().forEach(graph::removeNode);
      reluOp.getApplicationNodes().toList().forEach(graph::removeNode);
      var linearIndex = linearOp
        .getAnnotations()
        .get(TensorOpNodes.IPF_INDEX_ANNOTATION_TYPE)
        .viewAs(ZRange.class);
      var reluIndex = reluOp
        .getAnnotations()
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

    debugRender(graph);
  }
}
