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

    var reshard = true;

    var dtype = "float32";

    var input = TensorNode
      .on(graph)
      .label("input")
      .body(b -> b.dtype(dtype).shape(100, 12))
      .build();

    var weights = TensorNode.on(graph).label("W").body(b -> b.dtype(dtype).shape(12, 32)).build();

    var bias = TensorNode.on(graph).label("bias").body(b -> b.dtype(dtype).shape(32)).build();

    var op = CommonMetaKernels.LINEAR
      .on(graph)
      .input("x", input)
      .input("A", weights)
      .input("b", bias)
      .apply();

    var result = op.getResult().withLabel("Z");

    if (reshard) {
      // Re-shard 2 ways.
      op.getApplicationNodes().toList().forEach(graph::removeNode);
      var index = op
        .getAnnotations()
        .get(TensorOpNodes.IPF_INDEX_ANNOTATION_TYPE)
        .viewAs(ZRange.class);

      OperationUtils.createIpfShards(
        op,
        Arrays.stream(index.split(0, 50)).flatMap(i -> Arrays.stream(i.split(1, 16))).toList()
      );
    }

    assertThat(result.getShape()).isEqualTo(ZPoint.of(100, 32));
    assertThat(result.getDtype()).isEqualTo(dtype);

    graph.validate();

    debugRender(graph);
  }
}
