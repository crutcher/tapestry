package org.tensortapestry.zspace;

import org.junit.jupiter.api.Test;
import org.tensortapestry.zspace.experimental.ZSpaceTestAssertions;

public class ZRangeProjectionMapTest implements ZSpaceTestAssertions {

  @Test
  public void test_json() {
    var ipf = new ZRangeProjectionMap(
      new ZAffineMap(
        ZTensor.newMatrix(new int[][] { { 1, 0 }, { 0, 1 }, { 1, 1 } }),
        ZTensor.newVector(10, 20, 30)
      ),
      ZPoint.of(4, 4, 1)
    );

    assertObjectJsonEquivalence(
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
            """
    );
  }

  @Test
  public void test_builder() {
    ZAffineMap affineMap = ZAffineMap.fromMatrix(new int[][] { { 1, 0 }, { 0, 1 }, { 1, 1 } });

    assertThat(new ZRangeProjectionMap(affineMap, ZPoint.of(4, 4, 1)))
      .isEqualTo(
        ZRangeProjectionMap
          .builder()
          .affineMap(affineMap)
          .shape(ZTensor.newVector(4, 4, 1))
          .translate(ZPoint.of(1, 2, 3))
          .translate(-1, -2, -3)
          .build()
      )
      .isEqualTo(
        ZRangeProjectionMap.builder().affineMap(affineMap).shape(ZTensor.newVector(4, 4, 1)).build()
      )
      .isEqualTo(
        ZRangeProjectionMap.builder().affineMap(affineMap).shape(ZPoint.of(4, 4, 1)).build()
      )
      .isEqualTo(ZRangeProjectionMap.builder().affineMap(affineMap).shape(4, 4, 1).build());
  }

  @Test
  public void test_toString() {
    var ipf = new ZRangeProjectionMap(
      new ZAffineMap(
        ZTensor.newMatrix(new int[][] { { 1, 0 }, { 0, 1 }, { 1, 1 } }),
        ZTensor.newVector(10, 20, 30)
      ),
      ZPoint.of(4, 4, 1)
    );

    assertThat(ipf)
      .hasToString("ipf(affineMap=λx.[[1,0],[0,1],[1,1]]⋅x + [10,20,30], shape=[4, 4, 1])");
  }

  @Test
  public void test_mismatch() {
    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() ->
        new ZRangeProjectionMap(
          new ZAffineMap(
            ZTensor.newMatrix(new int[][] { { 1, 0 }, { 0, 1 }, { 1, 1 } }),
            ZTensor.newVector(10, 20, 30)
          ),
          ZPoint.of(4, 1)
        )
      )
      .withMessageContaining("affineMap.outputDim() (3) != shape.dim() (2)");
  }

  @Test
  public void test_translate() {
    var ipf = new ZRangeProjectionMap(
      new ZAffineMap(
        ZTensor.newMatrix(new int[][] { { 1, 0 }, { 0, 1 }, { 1, 1 } }),
        ZTensor.newVector(10, 20, 30)
      ),
      ZPoint.of(4, 4, 1)
    );

    assertThat(ipf.translate(ZPoint.of(1, 2, 3)))
      .isEqualTo(
        new ZRangeProjectionMap(
          new ZAffineMap(
            ZTensor.newMatrix(new int[][] { { 1, 0 }, { 0, 1 }, { 1, 1 } }),
            ZTensor.newVector(11, 22, 33)
          ),
          ZPoint.of(4, 4, 1)
        )
      );
  }

  @Test
  public void test() {
    var ipf = new ZRangeProjectionMap(
      new ZAffineMap(
        ZTensor.newMatrix(new int[][] { { 1, 0 }, { 0, 1 }, { 1, 1 } }),
        ZTensor.newVector(10, 20, 30)
      ),
      ZPoint.of(4, 4, 1)
    );

    ZTensorWrapper start3 = ZPoint.of(15, 26, 41);
    ZTensorWrapper shape3 = ZPoint.of(4, 4, 1);
    assertThat(ipf.apply(ZPoint.of(5, 6)))
      .isEqualTo(ZRange.builder().start(start3).shape(shape3).build());
    ZTensorWrapper start2 = ZPoint.of(15, 26, 41);
    ZTensorWrapper shape2 = ZPoint.of(4, 4, 1);
    assertThat(ipf.apply(ZTensor.newVector(5, 6)))
      .isEqualTo(ZRange.builder().start(start2).shape(shape2).build());

    ZTensorWrapper start1 = ZPoint.of(15, 26, 41);
    ZTensorWrapper shape1 = ZPoint.of(5, 5, 3);
    assertThat(ipf.apply(ZRange.of(ZPoint.of(5, 6), ZPoint.of(7, 8))))
      .isEqualTo(ZRange.builder().start(start1).shape(shape1).build());

    ZTensorWrapper start = ZPoint.of(15, 26, 41);
    ZTensorWrapper shape = ZPoint.newZeros(3);
    assertThat(ipf.apply(ZRange.of(ZPoint.of(5, 6), ZPoint.of(5, 6))))
      .isEqualTo(ZRange.builder().start(start).shape(shape).build());
  }
}
