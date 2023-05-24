package loom.zspace;

import loom.testing.CommonAssertions;
import org.junit.Test;

public class ZAffineMapTest implements CommonAssertions {
  @Test
  public void test_constructor() {
    var map =
        new ZAffineMap(ZTensor.from(new int[][] {{1, 0}, {0, 2}, {1, 2}}), ZTensor.vector(4, 5, 6));

    assertThat(map.apply(new ZPoint(1, 1))).isEqualTo(new ZPoint(5, 7, 9));

    assertThat(map.inputDim).isEqualTo(2);
    assertThat(map.outputDim).isEqualTo(3);

    assertThat(map.A.isMutable()).isFalse();
    assertThat(map.b.isMutable()).isFalse();

    assertThat(map)
        .hasSameHashCodeAs(
            new ZAffineMap(
                ZTensor.from(new int[][] {{1, 0}, {0, 2}, {1, 2}}), ZTensor.vector(4, 5, 6)));
    assertThat(map).isEqualTo(map);
  }

  @Test
  public void test_string_parse_json() {
    var map =
        new ZAffineMap(ZTensor.from(new int[][] {{1, 0}, {0, 2}, {1, 2}}), ZTensor.vector(4, 5, 6));

    String json = "{\"A\":[[1,0],[0,2],[1,2]],\"b\":[4,5,6]}";

    assertThat(map).hasToString(json);
    assertThat(map.toJsonString()).isEqualTo(json);

    assertThat(ZAffineMap.parse(json)).isEqualTo(map);
  }

  @Test
  public void test_permute() {
    var map =
        new ZAffineMap(ZTensor.from(new int[][] {{1, 0}, {0, 2}, {1, 2}}), ZTensor.vector(4, 5, 6));

    assertThat(map.permuteInput(1, 0))
        .isEqualTo(
            new ZAffineMap(
                ZTensor.from(new int[][] {{0, 1}, {2, 0}, {2, 1}}), ZTensor.vector(4, 5, 6)));

    assertThat(map.permuteOutput(1, 0, 2))
        .isEqualTo(
            new ZAffineMap(
                ZTensor.from(new int[][] {{0, 2}, {1, 0}, {1, 2}}), ZTensor.vector(5, 4, 6)));
  }

  @Test
  public void test_apply() {
    var map =
        new ZAffineMap(ZTensor.from(new int[][] {{1, 0}, {0, 2}, {1, 2}}), ZTensor.vector(4, 5, 6));

    assertThat(map.apply(new ZPoint(1, 1))).isEqualTo(new ZPoint(5, 7, 9));
    assertThat(map.apply(ZTensor.vector(1, 1))).isEqualTo(ZTensor.vector(5, 7, 9));
  }
}
