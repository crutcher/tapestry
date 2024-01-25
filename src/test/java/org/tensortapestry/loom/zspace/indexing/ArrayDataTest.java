package org.tensortapestry.loom.zspace.indexing;

import org.junit.Test;
import org.tensortapestry.loom.zspace.experimental.ZSpaceTestAssertions;

public class ArrayDataTest implements ZSpaceTestAssertions {

  @Test
  public void test() {
    var shape = new int[] { 1, 2, 3 };
    var data = new int[] { 1, 2, 3, 4, 5, 6 };

    var arrayData = new ArrayData(shape, data);

    assertThat(arrayData.getShape()).isEqualTo(arrayData.getKey()).isSameAs(shape);

    assertThat(arrayData.getData()).isEqualTo(arrayData.getValue()).isSameAs(data);

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> new ArrayData(new int[] { -2, 8 }, new int[0]))
      .withMessage("shape must be non-negative: [-2, 8]");

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> new ArrayData(new int[] { 2, 8 }, new int[0]))
      .withMessage("Shape size (16) != data length (0): [2, 8]");

    assertThatExceptionOfType(UnsupportedOperationException.class)
      .isThrownBy(() -> arrayData.setValue(new int[0]));
  }
}
