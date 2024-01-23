package org.tensortapestry.loom.graph.nodes;

import java.util.UUID;
import org.junit.Test;
import org.tensortapestry.loom.testing.BaseTestClass;
import org.tensortapestry.loom.zspace.ZDimMissMatchError;
import org.tensortapestry.loom.zspace.ZRange;

public class TensorSelectionTest extends BaseTestClass {

  @Test
  public void test_from() {
    var tensorNode = TensorNode
      .withBody(b -> b.dtype("int32").shape(4, 5))
      .id(UUID.randomUUID())
      .build();

    {
      var tensorSelection = TensorSelection.from(tensorNode);

      assertThat(tensorSelection.getTensorId()).isEqualTo(tensorNode.getId());
      assertThat(tensorSelection.getRange()).isEqualTo(tensorNode.getRange());
    }
    {
      ZRange range = ZRange.builder().start(0, 0).shape(2, 2).build();
      var tensorSelection = TensorSelection.from(tensorNode, range);

      assertThat(tensorSelection.getTensorId()).isEqualTo(tensorNode.getId());
      assertThat(tensorSelection.getRange()).isEqualTo(range);
    }

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() ->
        TensorSelection.from(tensorNode, ZRange.builder().start(-1, 0).shape(2, 2).build())
      )
      .withMessageContaining("does not contain range");

    assertThatExceptionOfType(ZDimMissMatchError.class)
      .isThrownBy(() -> TensorSelection.from(tensorNode, ZRange.newFromShape(2)))
      .withMessageContaining("ZDim shape mismatch: [2] != [1]");
  }
}
