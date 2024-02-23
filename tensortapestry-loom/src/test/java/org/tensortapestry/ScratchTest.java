package org.tensortapestry;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.CommonAssertions;
import org.tensortapestry.loom.graph.dialects.tensorops.OperationExpressionDialect;
import org.tensortapestry.loom.graph.dialects.tensorops.OperationNode;
import org.tensortapestry.loom.graph.dialects.tensorops.TensorNode;
import org.tensortapestry.zspace.ZRange;

public class ScratchTest implements CommonAssertions {

  @Test
  public void test() {
    var graph = OperationExpressionDialect.newGraph();

    var t0 = TensorNode.on(graph).body(b -> b.dtype("int32").shape(10, 5)).label("t0").build();
    var t1 = TensorNode
      .on(graph)
      .body(b -> b.dtype("int32").range(ZRange.builder().start(200, 50).shape(10, 5).build()))
      .label("t1")
      .build();
    var mask = TensorNode
      .on(graph)
      .body(b -> b.dtype("boolean").shape(10, 5))
      .label("mask")
      .build();

    var z = TensorNode.on(graph).body(b -> b.dtype("int32").shape(10, 5)).label("z0").build();

    OperationNode
      .on(graph)
      .body(b ->
        b
          .kernel("builtins:add")
          .param("mask_value", 12)
          .input("tensor", List.of(t0.getTensorSelection(), t1.getTensorSelection()))
          .input("mask", List.of(mask.getTensorSelection()))
          .output("result", List.of(z.getTensorSelection()))
      )
      .label("op0")
      .build();

    System.out.println(graph.toPrettyJsonString());
  }
}
