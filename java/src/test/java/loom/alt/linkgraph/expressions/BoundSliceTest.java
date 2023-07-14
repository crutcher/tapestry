package loom.alt.linkgraph.expressions;

import loom.testing.CommonAssertions;
import loom.zspace.*;
import org.junit.Test;

public class BoundSliceTest implements CommonAssertions {
  @Test
  public void test_string_json() {
    var slice =
        BoundSlice.builder()
            .name("foo")
            .dtype("int32")
            .index(
                NamedZRange.builder()
                    .dimensions(new DimensionMap("a", "b", "c"))
                    .range(ZRange.fromShape(ZTensor.vector(3, 4, 5)))
                    .build())
            .projection(
                new IndexProjectionFunction(
                    new DimensionMap("x", "y"),
                    new DimensionMap("a", "b", "c"),
                    new ZAffineMap(
                        ZTensor.from(new int[][] {{1, 0}, {0, 2}, {-1, 2}}),
                        ZTensor.vector(4, 5, 6)),
                    new ZPoint(3, 2, 1)))
            .build();

    String json =
        """
                        {
                          "name": "foo",
                          "dtype": "int32",
                          "index": {
                            "dimensions": ["a", "b", "c"],
                            "range": {
                              "start": [0, 0, 0],
                              "end": [3, 4, 5]
                            }
                          },
                          "projection": {
                            "input": ["x", "y"],
                            "output": ["a", "b", "c"],
                            "map": {
                              "A": [[1, 0], [0, 2], [-1, 2]],
                              "b": [4, 5, 6]
                            },
                            "shape": [3, 2, 1]
                          }
                        }
                        """;

    assertJsonEquals(slice, json);

    assertThat(slice)
        .hasToString("b[foo:int32 i[a=0:3, b=0:4, c=0:5]; p[a=x+4:+3, b=2y+5:+2, c=-x+2y+6:+1]]");
  }
}
