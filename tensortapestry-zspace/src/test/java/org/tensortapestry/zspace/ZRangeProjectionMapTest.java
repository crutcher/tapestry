package org.tensortapestry.zspace;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.tensortapestry.zspace.experimental.ZSpaceTestAssertions;

public class ZRangeProjectionMapTest implements ZSpaceTestAssertions {

  @Test
  public void test_identity() {
    assertThat(ZRangeProjectionMap.builder().identityMap(3).shape(4, 4, 1).build())
      .isEqualTo(
        new ZRangeProjectionMap(
          new ZAffineMap(
            ZTensor.newMatrix(new int[][] { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } }),
            ZTensor.newVector(0, 0, 0)
          ),
          ZPoint.of(4, 4, 1)
        )
      );
  }

  @Test
  public void test_fromDiagonal() {
    assertThat(ZRangeProjectionMap.builder().fromDiagonal(1, 2, 3).shape(4, 4, 1).build())
      .isEqualTo(
        ZRangeProjectionMap.builder().fromDiagonal(List.of(1, 2, 3)).shape(4, 4, 1).build()
      )
      .isEqualTo(
        ZRangeProjectionMap.builder().fromDiagonal(ZPoint.of(1, 2, 3)).shape(4, 4, 1).build()
      )
      .isEqualTo(
        new ZRangeProjectionMap(
          new ZAffineMap(
            ZTensor.newMatrix(new int[][] { { 1, 0, 0 }, { 0, 2, 0 }, { 0, 0, 3 } }),
            ZTensor.newVector(0, 0, 0)
          ),
          ZPoint.of(4, 4, 1)
        )
      );
  }

  @Test
  public void test_affineMap() {
    assertThat(
      ZRangeProjectionMap
        .builder()
        .affineMap(new int[][] { { 1, 0 }, { 0, 1 }, { 1, 1 } })
        .shape(4, 4, 1)
        .build()
    )
      .isEqualTo(
        ZRangeProjectionMap
          .builder()
          .affineMap(ZTensor.newMatrix(new int[][] { { 1, 0 }, { 0, 1 }, { 1, 1 } }))
          .shape(4, 4, 1)
          .build()
      )
      .isEqualTo(
        new ZRangeProjectionMap(
          new ZAffineMap(
            ZTensor.newMatrix(new int[][] { { 1, 0 }, { 0, 1 }, { 1, 1 } }),
            ZTensor.newVector(0, 0, 0)
          ),
          ZPoint.of(4, 4, 1)
        )
      );
  }

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
    int[][] rows = { { 1, 0 }, { 0, 1 }, { 1, 1 } };
    ZAffineMap affineMap = ZAffineMap.fromMatrix(rows);

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
        ZRangeProjectionMap
          .builder()
          .affineMap(rows)
          .shape(ZPoint.of(4, 4, 1))
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
  public void test_apply() {
    var rpm = new ZRangeProjectionMap(
      new ZAffineMap(
        ZTensor.newMatrix(new int[][] { { 1, 0 }, { 0, 1 }, { 1, 1 } }),
        ZTensor.newVector(10, 20, 30)
      ),
      ZPoint.of(4, 4, 1)
    );

    var start3 = ZPoint.of(15, 26, 41);
    var shape3 = ZPoint.of(4, 4, 1);
    assertThat(rpm.apply(ZPoint.of(5, 6)))
      .isEqualTo(ZRange.builder().start(start3).shape(shape3).build());
    var start2 = ZPoint.of(15, 26, 41);
    var shape2 = ZPoint.of(4, 4, 1);
    assertThat(rpm.apply(ZTensor.newVector(5, 6)))
      .isEqualTo(ZRange.builder().start(start2).shape(shape2).build());

    var start1 = ZPoint.of(15, 26, 41);
    var shape1 = ZPoint.of(5, 5, 3);
    assertThat(rpm.apply(ZRange.of(ZPoint.of(5, 6), ZPoint.of(7, 8))))
      .isEqualTo(ZRange.builder().start(start1).shape(shape1).build());

    var start = ZPoint.of(15, 26, 41);
    var shape = ZPoint.newZeros(3);
    assertThat(rpm.apply(ZRange.of(ZPoint.of(5, 6), ZPoint.of(5, 6))))
      .isEqualTo(ZRange.builder().start(start).shape(shape).build());
  }

  @Test
  public void test_broadcastApply() {
    var rpm = new ZRangeProjectionMap(
      new ZAffineMap(
        ZTensor.newMatrix(new int[][] { { 1, 0 }, { 0, 1 }, { 1, 1 } }),
        ZTensor.newVector(10, 20, 30)
      ),
      ZPoint.of(4, 4, 1)
    );

    assertThat(rpm.broadcastApply(ZPoint.of(100, 20, 5, 6)))
      .isEqualTo(
        ZRange
          .builder()
          .start(ZPoint.of(100, 20, 15, 26, 41))
          .shape(ZPoint.of(1, 1, 4, 4, 1))
          .build()
      );
  }
}
