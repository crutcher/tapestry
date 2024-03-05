package org.tensortapestry;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.CommonAssertions;
import org.tensortapestry.loom.graph.dialects.tensorops.ApplicationExpressionDialect;
import org.tensortapestry.loom.graph.dialects.tensorops.OperationUtils;
import org.tensortapestry.loom.graph.dialects.tensorops.TensorNode;
import org.tensortapestry.weft.metakernels.CommonMetaKernels;
import org.tensortapestry.zspace.ZRange;

public class ScratchTest implements CommonAssertions {

  @Test
  @SuppressWarnings("unused")
  public void test() {
    var graph = ApplicationExpressionDialect.newGraph();

    var t0 = TensorNode.on(graph).body(b -> b.dtype("int32").shape(10, 5)).label("t0").build();
    var t1 = TensorNode
      .on(graph)
      .body(b -> b.dtype("int32").range(ZRange.builder().start(200, 50).shape(10, 5).build()))
      .label("t1")
      .build();

    var op = CommonMetaKernels.ADD.on(graph).input("tensors", t0, t1).apply();

    op.getApplicationNodes().stream().toList().forEach(graph::removeNode);

    var z = op.getResult().withLabel("z");

    OperationUtils.createIpfShards(
      op,
      List.of(
        ZRange.builder().start(0, 0).shape(5, 5).build(),
        ZRange.builder().start(5, 0).shape(5, 5).build()
      )
    );

    var img = ApplicationExpressionDialect.toImage(graph);

    graph.validate();

    System.out.println(graph.toPrettyJsonString());
  }
}
