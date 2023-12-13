package loom.zspace;

import loom.testing.CommonAssertions;
import org.junit.Test;

public class ZAffineMapTest implements CommonAssertions {
  @Test
  public void test_constructor() {
    var map =
        new ZAffineMap(
            ZTensor.newFrom(new int[][] {{1, 0}, {0, 2}, {1, 2}}), ZTensor.newVector(4, 5, 6));

    assertThat(map.apply(new ZPoint(1, 1))).isEqualTo(new ZPoint(5, 7, 9));

    assertThat(map.inputDim()).isEqualTo(2);
    assertThat(map.outputDim()).isEqualTo(3);

    assertThat(map.a.isMutable()).isFalse();
    assertThat(map.b.isMutable()).isFalse();

    assertThat(map)
        .hasSameHashCodeAs(
            new ZAffineMap(
                ZTensor.newFrom(new int[][] {{1, 0}, {0, 2}, {1, 2}}), ZTensor.newVector(4, 5, 6)));
    assertThat(map).isEqualTo(map);

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () ->
                new ZAffineMap(
                    ZTensor.newFrom(new int[][] {{1, 0}, {0, 2}}), ZTensor.newVector(4, 5, 6)))
        .withMessageContaining("A.shape[1] != b.shape[0]: [2, 2] != [3]");
  }

  @Test
  public void test_string_parse_json() {
    var map =
        new ZAffineMap(
            ZTensor.newFrom(new int[][] {{1, 0}, {0, 2}, {1, 2}}), ZTensor.newVector(4, 5, 6));

    String json = "{\"a\":[[1,0],[0,2],[1,2]],\"b\":[4,5,6]}";

    assertThat(map).hasToString(json);
    assertThat(map.toJsonString()).isEqualTo(json);

    assertThat(ZAffineMap.parse(json)).isEqualTo(map);
  }

  @Test
  public void test_permute() {
    var map =
        new ZAffineMap(
            ZTensor.newFrom(new int[][] {{1, 0}, {0, 2}, {1, 2}}), ZTensor.newVector(4, 5, 6));

    assertThat(map.permuteInput(1, 0))
        .isEqualTo(
            new ZAffineMap(
                ZTensor.newFrom(new int[][] {{0, 1}, {2, 0}, {2, 1}}), ZTensor.newVector(4, 5, 6)));

    assertThat(map.permuteOutput(1, 0, 2))
        .isEqualTo(
            new ZAffineMap(
                ZTensor.newFrom(new int[][] {{0, 2}, {1, 0}, {1, 2}}), ZTensor.newVector(5, 4, 6)));
  }

  @Test
  public void test_apply() {
    var map =
        new ZAffineMap(
            ZTensor.newFrom(new int[][] {{1, 0}, {0, 2}, {1, 2}}), ZTensor.newVector(4, 5, 6));

    assertThat(map.apply(new ZPoint(1, 1))).isEqualTo(new ZPoint(5, 7, 9));
    assertThat(map.apply(ZTensor.newVector(1, 1))).isEqualTo(ZTensor.newVector(5, 7, 9));

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> map.apply(ZTensor.newVector(1, 1, 1)))
        .withMessageContaining("A.shape[1] != x.shape[0]: [3, 2] != [3]");
  }
}
