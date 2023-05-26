package loom.expressions;

import java.util.List;
import loom.testing.CommonAssertions;
import loom.zspace.ZAffineMap;
import loom.zspace.ZPoint;
import loom.zspace.ZTensor;
import org.junit.Test;

public class BlockExpressionTest implements CommonAssertions {
  @Test
  public void test_constructor() {
    var block =
        new BlockExpression(
            NamedZRange.parse("i[x=0:3, y=0:4, z=0:5]"),
            List.of(
                new BoundSlice(
                    "foo",
                    "int32",
                    NamedZRange.parse("i[a=4:9, b=5:17]"),
                    new IndexProjectionFunction(
                        new DimensionMap("x", "y", "z"),
                        new DimensionMap("a", "b"),
                        new ZAffineMap(
                            ZTensor.from(new int[][] {{1, 0, 0}, {1, 0, 2}}), ZTensor.vector(4, 5)),
                        new ZPoint(3, 2)))),
            List.of());

    String json =
        """
                        {
                            "index": {
                                "dimensions": ["x", "y", "z"],
                                "range": {
                                    "start": [0, 0, 0],
                                    "end": [3, 4, 5]
                                }
                            },
                            "inputs": [
                                {
                                    "name": "foo",
                                    "dtype": "int32",
                                    "index": {
                                        "dimensions": ["a", "b"],
                                        "range": {
                                            "start": [4, 5],
                                            "end": [9, 17]
                                        }
                                    },
                                    "projection": {
                                        "input": ["x", "y", "z"],
                                        "output": ["a", "b"],
                                        "map": {
                                            "A": [[1, 0, 0], [1, 0, 2]],
                                            "b": [4, 5]
                                        },
                                        "shape": [3, 2]
                                    }
                                }
                            ],
                            "outputs": []
                        }""";

    assertJsonEquals(block, json);

    var pretty =
        "BlockExpression(index=i[x=0:3, y=0:4, z=0:5], inputs=[b[foo:int32 i[a=4:9, b=5:17]; p[a=x+4:+3, b=x+2z+5:+2]]], outputs=[])";
    assertThat(block).hasToString(pretty);
  }
}
