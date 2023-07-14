package loom.alt.linkgraph.expressions;

import loom.testing.CommonAssertions;
import loom.zspace.ZAffineMap;
import loom.zspace.ZPoint;
import loom.zspace.ZTensor;
import org.junit.Test;

public class IndexProjectionFunctionTest implements CommonAssertions {
  @Test
  public void test_string_parse_json() {
    var map =
        new IndexProjectionFunction(
            new DimensionMap("x", "y"),
            new DimensionMap("a", "b", "c"),
            new ZAffineMap(
                ZTensor.from(new int[][] {{1, 0}, {0, 2}, {-1, 2}}), ZTensor.vector(4, 5, 6)),
            new ZPoint(2, 1, 4));

    String json =
        """
                        {
                                "input": ["x", "y"],
                                "output": ["a", "b", "c"],
                                "map": {
                                  "A": [[1, 0], [0, 2], [-1, 2]],
                                  "b": [4, 5, 6]
                                },
                              "shape": [2, 1, 4]
                            }
                        """;

    assertJsonEquals(map, json);

    assertThat(IndexProjectionFunction.parse(json)).isEqualTo(map);

    String pretty = "p[a=x+4:+2, b=2y+5:+1, c=-x+2y+6:+4]";
    assertThat(map).hasToString(pretty);
  }

  @Test
  public void test_permute() {
    var map =
        IndexProjectionFunction.builder()
            .input(new DimensionMap("x", "y"))
            .output(new DimensionMap("a", "b", "c"))
            .map(
                new ZAffineMap(
                    ZTensor.from(new int[][] {{1, 0}, {0, 2}, {1, 2}}), ZTensor.vector(4, 5, 6)))
            .shape(new ZPoint(3, 4, 5))
            .build();

    assertThat(map.permuteInput(1, 0))
        .isEqualTo(
            new IndexProjectionFunction(
                new DimensionMap("y", "x"),
                new DimensionMap("a", "b", "c"),
                new ZAffineMap(
                    ZTensor.from(new int[][] {{0, 1}, {2, 0}, {2, 1}}), ZTensor.vector(4, 5, 6)),
                new ZPoint(3, 4, 5)));

    assertThat(map.permuteInput("x", "y")).isEqualTo(map);
    assertThat(map.permuteInput("y", "x"))
        .isEqualTo(
            new IndexProjectionFunction(
                new DimensionMap("y", "x"),
                new DimensionMap("a", "b", "c"),
                new ZAffineMap(
                    ZTensor.from(new int[][] {{0, 1}, {2, 0}, {2, 1}}), ZTensor.vector(4, 5, 6)),
                new ZPoint(3, 4, 5)));

    assertThat(map.permuteOutput("a", "b", "c")).isEqualTo(map);

    assertThat(map.permuteOutput("c", "a", "b"))
        .isEqualTo(
            new IndexProjectionFunction(
                new DimensionMap("x", "y"),
                new DimensionMap("c", "a", "b"),
                new ZAffineMap(
                    ZTensor.from(new int[][] {{1, 2}, {1, 0}, {0, 2}}), ZTensor.vector(6, 4, 5)),
                new ZPoint(5, 3, 4)));
  }

  @Test
  public void test_apply() {
    var map =
        new IndexProjectionFunction(
            new DimensionMap("x", "y"),
            new DimensionMap("a", "b", "c"),
            new ZAffineMap(
                ZTensor.from(new int[][] {{1, 0}, {0, 2}, {1, 2}}), ZTensor.vector(4, 5, 6)),
            new ZPoint(1, 2, 3));

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
