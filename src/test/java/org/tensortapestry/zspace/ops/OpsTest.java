package org.tensortapestry.zspace.ops;

import org.junit.jupiter.api.Test;
import org.tensortapestry.zspace.ZTensor;
import org.tensortapestry.zspace.experimental.ZSpaceTestAssertions;

public class OpsTest implements ZSpaceTestAssertions {

  @Test
  @SuppressWarnings("unused")
  public void test_intPow() {
    assertThat(CellWiseOps.intPow(2, 0)).isEqualTo(1);
    assertThat(CellWiseOps.intPow(2, 1)).isEqualTo(2);
    assertThat(CellWiseOps.intPow(2, 2)).isEqualTo(4);

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> {
        var _ignored = CellWiseOps.intPow(2, -1);
      });
  }

  @Test
  public void test_intLog() {
    assertThat(CellWiseOps.intLog(1, 2)).isEqualTo(0);
    assertThat(CellWiseOps.intLog(2, 2)).isEqualTo(1);
    assertThat(CellWiseOps.intLog(4, 2)).isEqualTo(2);

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> CellWiseOps.intLog(0, 2));
    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> CellWiseOps.intLog(-1, 2));
  }

  @Test
  public void test_matmul() {
    var lhs = ZTensor.newFromArray(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
    var rhs = ZTensor.newFromArray(new int[][] { { 10, 11 }, { 20, 21 }, { 30, 31 } });

    assertThat(MatrixOps.matmul(lhs, rhs))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 140, 146 }, { 320, 335 } }));

    assertThat(MatrixOps.matmul(lhs, ZTensor.newVector(10, 20, 30)))
      .isEqualTo(ZTensor.newFromArray(new int[] { 140, 320 }));

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> MatrixOps.matmul(lhs, ZTensor.newVector(10, 20, 30, 40)))
      .withMessageContaining("lhs shape [2, 3] not compatible with rhs shape [4]");

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> MatrixOps.matmul(lhs, ZTensor.newZeros(3, 4, 5)))
      .withMessageContaining("rhs must be a 1D or 2D tensor, got 3D: [3, 4, 5]");
  }
}
