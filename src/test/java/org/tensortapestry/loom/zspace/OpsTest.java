package org.tensortapestry.loom.zspace;

import org.junit.Test;
import org.tensortapestry.loom.testing.CommonAssertions;

public class OpsTest implements CommonAssertions {

  @Test
  @SuppressWarnings("unused")
  public void test_intPow() {
    assertThat(Ops.CellWise.intPow(2, 0)).isEqualTo(1);
    assertThat(Ops.CellWise.intPow(2, 1)).isEqualTo(2);
    assertThat(Ops.CellWise.intPow(2, 2)).isEqualTo(4);

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> {
        var _ignored = Ops.CellWise.intPow(2, -1);
      });
  }

  @Test
  public void test_intLog() {
    assertThat(Ops.CellWise.intLog(1, 2)).isEqualTo(0);
    assertThat(Ops.CellWise.intLog(2, 2)).isEqualTo(1);
    assertThat(Ops.CellWise.intLog(4, 2)).isEqualTo(2);

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> Ops.CellWise.intLog(0, 2));
    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> Ops.CellWise.intLog(-1, 2));
  }
}
