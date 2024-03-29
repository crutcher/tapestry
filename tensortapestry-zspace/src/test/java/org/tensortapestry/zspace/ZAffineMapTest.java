package org.tensortapestry.zspace;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.tensortapestry.zspace.experimental.ZSpaceTestAssertions;

public class ZAffineMapTest implements ZSpaceTestAssertions {

  @Test
  public void test_newIdentityMap() {
    assertThat(ZAffineMap.newIdentityMap(0)).isEqualTo(new ZAffineMap(ZTensor.newDiagonalMatrix()));
    assertThat(ZAffineMap.newIdentityMap(2))
      .isEqualTo(new ZAffineMap(ZTensor.newDiagonalMatrix(1, 1)));
  }

  @Test
  public void test_newFromDiagonal() {
    assertThat(ZAffineMap.newFromDiagonal(1, 2, 3))
      .isEqualTo(ZAffineMap.newFromDiagonal(List.of(1, 2, 3)))
      .isEqualTo(ZAffineMap.newFromDiagonal(ZTensor.newVector(1, 2, 3)))
      .isEqualTo(new ZAffineMap(ZTensor.newDiagonalMatrix(1, 2, 3)));
  }

  @Test
  public void test_constructor() {
    var map = new ZAffineMap(
      ZTensor.newFromArray(new int[][] { { 1, 0 }, { 0, 2 }, { 1, 2 } }),
      ZTensor.newVector(4, 5, 6)
    );

    assertThat(map.apply(new ZPoint(1, 1))).isEqualTo(new ZPoint(5, 7, 9));

    assertThat(map.getInputNDim()).isEqualTo(2);
    assertThat(map.getOutputNDim()).isEqualTo(3);

    assertThat(map).hasToString("λx.[[1,0],[0,2],[1,2]]⋅x + [4,5,6]");

    assertThat(ZAffineMap.fromMatrix(ZMatrix.newDiagonalMatrix(1, 2)))
      .hasToString("λx.[[1,0],[0,2]]⋅x");

    assertThat(map)
      .hasSameHashCodeAs(
        new ZAffineMap(
          ZTensor.newFromArray(new int[][] { { 1, 0 }, { 0, 2 }, { 1, 2 } }),
          ZTensor.newVector(4, 5, 6)
        )
      );
    assertThat(map).isEqualTo(map);

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() ->
        new ZAffineMap(
          ZTensor.newFromArray(new int[][] { { 1, 0 }, { 0, 2 } }),
          ZTensor.newVector(4, 5, 6)
        )
      )
      .withMessageContaining("projection.shape[1] != offset.shape[0]: [2, 2] != [3]");
  }

  @Test
  public void test_auto_bias() {
    int[][] mat = { { 1, 0 }, { 0, 2 }, { 1, 2 } };
    var A = ZTensor.newFromArray(mat);
    var b = ZTensor.newZeros(3);

    assertThat(new ZAffineMap(A, b))
      .isEqualTo(new ZAffineMap(A))
      .isEqualTo(new ZAffineMap(A, null))
      .isEqualTo(ZAffineMap.fromMatrix(new int[] { 1, 0 }, new int[] { 0, 2 }, new int[] { 1, 2 }))
      .isEqualTo(ZAffineMap.fromMatrix(A))
      .isEqualTo(ZAffineMap.fromMatrix(mat));
  }

  @Test
  public void test_translate() {
    var map = new ZAffineMap(
      ZTensor.newFromArray(new int[][] { { 1, 0 }, { 0, 2 }, { 1, 2 } }),
      ZTensor.newVector(4, 5, 6)
    );

    assertThat(map.translate(ZTensor.newVector(1, 2, 3)))
      .isEqualTo(
        new ZAffineMap(
          ZTensor.newFromArray(new int[][] { { 1, 0 }, { 0, 2 }, { 1, 2 } }),
          ZTensor.newVector(5, 7, 9)
        )
      );
    assertThat(map.translate(ZPoint.of(1, 2, 3)))
      .isEqualTo(
        new ZAffineMap(
          ZTensor.newFromArray(new int[][] { { 1, 0 }, { 0, 2 }, { 1, 2 } }),
          ZTensor.newVector(5, 7, 9)
        )
      );
    assertThat(map.translate(1, 2, 3))
      .isEqualTo(
        new ZAffineMap(
          ZTensor.newFromArray(new int[][] { { 1, 0 }, { 0, 2 }, { 1, 2 } }),
          ZTensor.newVector(5, 7, 9)
        )
      );
  }

  @Test
  public void test_string_parse_json() {
    var map = new ZAffineMap(
      ZTensor.newFromArray(new int[][] { { 1, 0 }, { 0, 2 }, { 1, 2 } }),
      ZTensor.newVector(4, 5, 6)
    );

    String json = "{\"projection\":[[1,0],[0,2],[1,2]],\"offset\":[4,5,6]}";

    assertThat(map.toJsonString()).isEqualTo(json);
  }

  @Test
  public void test_permute() {
    var map = new ZAffineMap(
      ZTensor.newFromArray(new int[][] { { 1, 0 }, { 0, 2 }, { 1, 2 } }),
      ZTensor.newVector(4, 5, 6)
    );

    assertThat(map.permuteInput(1, 0))
      .isEqualTo(
        new ZAffineMap(
          ZTensor.newFromArray(new int[][] { { 0, 1 }, { 2, 0 }, { 2, 1 } }),
          ZTensor.newVector(4, 5, 6)
        )
      );

    assertThat(map.permuteOutput(1, 0, 2))
      .isEqualTo(
        new ZAffineMap(
          ZTensor.newFromArray(new int[][] { { 0, 2 }, { 1, 0 }, { 1, 2 } }),
          ZTensor.newVector(5, 4, 6)
        )
      );
  }

  @Test
  public void test_apply() {
    var map = new ZAffineMap(
      ZTensor.newFromArray(new int[][] { { 1, 0 }, { 0, 2 }, { 1, 2 } }),
      ZTensor.newVector(4, 5, 6)
    );

    assertThat(map.apply(new ZPoint(1, 1))).isEqualTo(new ZPoint(5, 7, 9));
    assertThat(map.apply(ZTensor.newVector(1, 1))).isEqualTo(ZTensor.newVector(5, 7, 9));

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> map.apply(ZTensor.newVector(1, 1, 1)))
      .withMessageContaining("lhs shape [3, 2] not compatible with rhs shape [3]");
  }

  @Test
  public void test_broadcastApply() {
    var map = new ZAffineMap(
      ZTensor.newFromArray(new int[][] { { 1, 0 }, { 0, 2 }, { 1, 2 } }),
      ZTensor.newVector(4, 5, 6)
    );

    assertThat(map.broadcastApply(ZTensor.newVector(100, 20, 1, 1)))
      .isEqualTo(ZTensor.newVector(100, 20, 5, 7, 9));
  }
}
