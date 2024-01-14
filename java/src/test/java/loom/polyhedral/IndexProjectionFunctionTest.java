package loom.polyhedral;

import loom.testing.BaseTestClass;
import loom.zspace.ZAffineMap;
import loom.zspace.ZPoint;
import loom.zspace.ZRange;
import loom.zspace.ZTensor;
import org.junit.Test;

public class IndexProjectionFunctionTest extends BaseTestClass {
  @Test
  public void test_json() {
    var ipf =
        new IndexProjectionFunction(
            new ZAffineMap(
                ZTensor.newMatrix(
                    new int[][] {
                      {1, 0},
                      {0, 1},
                      {1, 1}
                    }),
                ZTensor.newVector(10, 20, 30)),
            ZPoint.of(4, 4, 1));

    assertJsonEquals(
        ipf,
        """
            {
              "affineMap": {
                "projection": [
                  [1, 0],
                  [0, 1],
                  [1, 1]
                ],
                "offset": [10, 20, 30]
              },
              "shape": [4, 4, 1]
            }
            """);
  }

  @Test
  public void test_builder() {
    ZAffineMap affineMap = ZAffineMap.fromMatrix(new int[][] {{1, 0}, {0, 1}, {1, 1}});

    assertThat(new IndexProjectionFunction(affineMap, ZPoint.of(4, 4, 1)))
        .isEqualTo(
            IndexProjectionFunction.builder()
                .affineMap(affineMap)
                .shape(ZTensor.newVector(4, 4, 1))
                .translate(ZPoint.of(1, 2, 3))
                .translate(-1, -2, -3)
                .build())
        .isEqualTo(
            IndexProjectionFunction.builder()
                .affineMap(affineMap.toBuilder())
                .shape(ZTensor.newVector(4, 4, 1))
                .build())
        .isEqualTo(
            IndexProjectionFunction.builder()
                .affineMap(affineMap)
                .shape(ZTensor.newVector(4, 4, 1))
                .build())
        .isEqualTo(
            IndexProjectionFunction.builder()
                .affineMap(affineMap)
                .shape(ZPoint.of(4, 4, 1))
                .build())
        .isEqualTo(IndexProjectionFunction.builder().affineMap(affineMap).shape(4, 4, 1).build());
  }

  @Test
  public void test_toString() {
    var ipf =
        new IndexProjectionFunction(
            new ZAffineMap(
                ZTensor.newMatrix(
                    new int[][] {
                      {1, 0},
                      {0, 1},
                      {1, 1}
                    }),
                ZTensor.newVector(10, 20, 30)),
            ZPoint.of(4, 4, 1));

    assertThat(ipf)
        .hasToString("ipf(affineMap=λx.[[1,0],[0,1],[1,1]]⋅x + [10,20,30], shape=[4, 4, 1])");
  }

  @Test
  public void test_mismatch() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () ->
                new IndexProjectionFunction(
                    new ZAffineMap(
                        ZTensor.newMatrix(
                            new int[][] {
                              {1, 0},
                              {0, 1},
                              {1, 1}
                            }),
                        ZTensor.newVector(10, 20, 30)),
                    ZPoint.of(4, 1)))
        .withMessageContaining("affineMap.outputDim() (3) != shape.dim() (2)");
  }

  @Test
  public void test_translate() {
    var ipf =
        new IndexProjectionFunction(
            new ZAffineMap(
                ZTensor.newMatrix(
                    new int[][] {
                      {1, 0},
                      {0, 1},
                      {1, 1}
                    }),
                ZTensor.newVector(10, 20, 30)),
            ZPoint.of(4, 4, 1));

    assertThat(ipf.translate(ZPoint.of(1, 2, 3)))
        .isEqualTo(
            new IndexProjectionFunction(
                new ZAffineMap(
                    ZTensor.newMatrix(
                        new int[][] {
                          {1, 0},
                          {0, 1},
                          {1, 1}
                        }),
                    ZTensor.newVector(11, 22, 33)),
                ZPoint.of(4, 4, 1)));
  }

  @Test
  public void test() {
    var ipf =
        new IndexProjectionFunction(
            new ZAffineMap(
                ZTensor.newMatrix(
                    new int[][] {
                      {1, 0},
                      {0, 1},
                      {1, 1}
                    }),
                ZTensor.newVector(10, 20, 30)),
            ZPoint.of(4, 4, 1));

    assertThat(ipf.apply(ZPoint.of(5, 6)))
        .isEqualTo(ZRange.fromStartWithShape(ZPoint.of(15, 26, 41), ZPoint.of(4, 4, 1)));
    assertThat(ipf.apply(ZTensor.newVector(5, 6)))
        .isEqualTo(ZRange.fromStartWithShape(ZPoint.of(15, 26, 41), ZPoint.of(4, 4, 1)));

    assertThat(ipf.apply(ZRange.of(ZPoint.of(5, 6), ZPoint.of(7, 8))))
        .isEqualTo(ZRange.fromStartWithShape(ZPoint.of(15, 26, 41), ZPoint.of(5, 5, 3)));

    assertThat(ipf.apply(ZRange.of(ZPoint.of(5, 6), ZPoint.of(5, 6))))
        .isEqualTo(ZRange.fromStartWithShape(ZPoint.of(15, 26, 41), ZPoint.newZeros(3)));
  }
}
