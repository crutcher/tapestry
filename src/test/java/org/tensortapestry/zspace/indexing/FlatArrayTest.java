package org.tensortapestry.zspace.indexing;

import org.junit.jupiter.api.Test;
import org.tensortapestry.zspace.experimental.ZSpaceTestAssertions;
import org.tensortapestry.zspace.impl.ZSpaceJsonUtil;

public class FlatArrayTest implements ZSpaceTestAssertions {

  @Test
  public void test_json() {
    var shape = new int[] { 1, 2, 3 };
    var data = new int[] { 1, 2, 3, 4, 5, 6 };
    var arrayData = new FlatArray(shape, data);

    String json =
      """
                [
                   [1, 2, 3],
                   [1, 2, 3, 4, 5, 6]
                ]
                """;

    assertObjectJsonEquivalence(arrayData, json);

    assertThat(arrayData).isEqualTo(ZSpaceJsonUtil.fromJson(json, FlatArray.class));
  }

  @Test
  public void test() {
    var shape = new int[] { 1, 2, 3 };
    var data = new int[] { 1, 2, 3, 4, 5, 6 };
    var flatArray = new FlatArray(shape, data);

    assertThat(flatArray.get(0, 1, 2)).isEqualTo(6);

    assertThat(flatArray.getShape()).isEqualTo(flatArray.getShape()).isSameAs(shape);

    assertThat(flatArray.getData()).isEqualTo(flatArray.getData()).isSameAs(data);

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> new FlatArray(new int[] { -2, 8 }, new int[0]))
      .withMessage("shape must be non-negative: [-2, 8]");

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> new FlatArray(new int[] { 2, 8 }, new int[0]))
      .withMessage("Shape size (16) != data length (0): [2, 8]");
  }
}
