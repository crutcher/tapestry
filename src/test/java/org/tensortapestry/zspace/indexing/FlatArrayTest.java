package org.tensortapestry.zspace.indexing;

import org.junit.jupiter.api.Test;
import org.tensortapestry.zspace.experimental.ZSpaceTestAssertions;

public class FlatArrayTest implements ZSpaceTestAssertions {

  @Test
  public void test() {
    var shape = new int[] { 1, 2, 3 };
    var data = new int[] { 1, 2, 3, 4, 5, 6 };

    var arrayData = new FlatArray(shape, data);

    assertThat(arrayData.getShape()).isEqualTo(arrayData.getShape()).isSameAs(shape);

    assertThat(arrayData.getData()).isEqualTo(arrayData.getData()).isSameAs(data);

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> new FlatArray(new int[] { -2, 8 }, new int[0]))
      .withMessage("shape must be non-negative: [-2, 8]");

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> new FlatArray(new int[] { 2, 8 }, new int[0]))
      .withMessage("Shape size (16) != data length (0): [2, 8]");
  }
}
