package loom.zspace;

import static org.junit.Assert.*;

import loom.testing.CommonAssertions;
import org.junit.Test;

public class NamedZAffineMapTest implements CommonAssertions {
  @Test
  public void test_string_parse_json() {
    var map =
        new NamedZAffineMap(
            new DimensionMap("x", "y"),
            new DimensionMap("a", "b", "c"),
            new ZAffineMap(
                ZTensor.from(new int[][] {{1, 0}, {0, 2}, {1, 2}}), ZTensor.vector(4, 5, 6)));

    String json =
        "{\"input\":[\"x\",\"y\"],\"output\":[\"a\",\"b\",\"c\"],\"map\":{\"A\":[[1,0],[0,2],[1,2]],\"b\":[4,5,6]}}";

    assertThat(map).hasToString(json);
    assertJsonEquals(map, json);

    assertThat(NamedZAffineMap.parse(json)).isEqualTo(map);
  }

  @Test
  public void test_permute() {
    var map =
        new NamedZAffineMap(
            new DimensionMap("x", "y"),
            new DimensionMap("a", "b", "c"),
            new ZAffineMap(
                ZTensor.from(new int[][] {{1, 0}, {0, 2}, {1, 2}}), ZTensor.vector(4, 5, 6)));

    assertThat(map.permuteInput(1, 0))
        .isEqualTo(
            new NamedZAffineMap(
                new DimensionMap("y", "x"),
                new DimensionMap("a", "b", "c"),
                new ZAffineMap(
                    ZTensor.from(new int[][] {{0, 1}, {2, 0}, {2, 1}}), ZTensor.vector(4, 5, 6))));

    assertThat(map.permuteInput("x", "y")).isEqualTo(map);
    assertThat(map.permuteInput("y", "x"))
        .isEqualTo(
            new NamedZAffineMap(
                new DimensionMap("y", "x"),
                new DimensionMap("a", "b", "c"),
                new ZAffineMap(
                    ZTensor.from(new int[][] {{0, 1}, {2, 0}, {2, 1}}), ZTensor.vector(4, 5, 6))));

    assertThat(map.permuteOutput("a", "b", "c")).isEqualTo(map);

    assertThat(map.permuteOutput("c", "a", "b"))
        .isEqualTo(
            new NamedZAffineMap(
                new DimensionMap("x", "y"),
                new DimensionMap("c", "a", "b"),
                new ZAffineMap(
                    ZTensor.from(new int[][] {{1, 2}, {1, 0}, {0, 2}}), ZTensor.vector(6, 4, 5))));
  }

  @Test
  public void test_apply() {
    var map =
        new NamedZAffineMap(
            new DimensionMap("x", "y"),
            new DimensionMap("a", "b", "c"),
            new ZAffineMap(
                ZTensor.from(new int[][] {{1, 0}, {0, 2}, {1, 2}}), ZTensor.vector(4, 5, 6)));

    assertThat(
            map.apply(
                new DimensionMap("y", "x"), ZTensor.vector(1, 2), new DimensionMap("c", "a", "b")))
        .isEqualTo(ZTensor.vector(10, 6, 7));
    assertThat(
            map.apply(
                new DimensionMap("y", "x"), new ZPoint(1, 2), new DimensionMap("c", "a", "b")))
        .isEqualTo(new ZPoint(10, 6, 7));
  }
}
