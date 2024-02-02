package org.tensortapestry.loom.graph.dialects.tensorops;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.BaseTestClass;
import org.tensortapestry.zspace.ZRange;
import org.tensortapestry.zspace.exceptions.ZDimMissMatchError;

public class TensorSelectionTest extends BaseTestClass {

  @Test
  public void test_from() {
    var tensorNode = TensorNode
      .builder()
      .id(UUID.randomUUID())
      .body(TensorNode.Body.builder().dtype("int32").shape(4, 5).build())
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
      .withMessageContaining("does not contain selection range");

    assertThatExceptionOfType(ZDimMissMatchError.class)
      .isThrownBy(() -> TensorSelection.from(tensorNode, ZRange.newFromShape(2)))
      .withMessageContaining("ZDim shape mismatch: [2] != [1]");
  }
}
