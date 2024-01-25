package org.tensortapestry.loom.zspace;

import java.util.List;
import org.junit.Test;
import org.tensortapestry.loom.zspace.experimental.ZSpaceTestAssertions;

public class ZMatrixTest implements ZSpaceTestAssertions {

  @Test
  public void test_json() {
    var matrix = ZMatrix.newMatrix(new int[][] { { 1, 2 }, { 3, 4 } });
    assertObjectJsonEquivalence(matrix, "[[1,2],[3,4]]");
  }

  @Test
  public void test_parse() {
    var matrix = ZMatrix.newMatrix(new int[][] { { 1, 2 }, { 3, 4 } });
    assertThat(ZMatrix.parse("[[1,2],[3,4]]")).isEqualTo(matrix);
  }

  @Test
  public void test_create() {
    var matrix = ZMatrix.newMatrix(new int[][] { { 1, 2 }, { 3, 4 } });
    assertThat(matrix.create(ZTensor.newMatrix(new int[][] { { 1, 2, 5 }, { 7, 3, 4 } })))
      .isInstanceOf(ZMatrix.class)
      .isEqualTo(ZMatrix.newMatrix(new int[][] { { 1, 2, 5 }, { 7, 3, 4 } }));
  }

  @Test
  public void test_clone() {
    var matrix = ZMatrix.newMatrix(new int[][] { { 1, 2 }, { 3, 4 } });
    assertThat(matrix.clone()).isEqualTo(matrix);
  }

  @Test
  public void test_toArray() {
    var matrix = ZMatrix.newMatrix(new int[][] { { 1, 2 }, { 3, 4 } });
    assertThat(matrix.toArray()).isEqualTo(new int[][] { { 1, 2 }, { 3, 4 } });
  }

  @Test
  public void test_fromMatrix() {
    var matrix = ZMatrix.newMatrix(new int[][] { { 1, 2 }, { 3, 4 } });
    ZTensor tensor = ZTensor.newMatrix(new int[][] { { 1, 2 }, { 3, 4 } });
    assertThat(matrix).isEqualTo(new ZMatrix(tensor)).isEqualTo(ZMatrix.newMatrix(tensor));
  }

  @Test
  public void test_newZeros() {
    assertThat(ZMatrix.newZeros(2, 3))
      .isEqualTo(ZMatrix.newMatrix(new int[][] { { 0, 0, 0 }, { 0, 0, 0 } }));

    assertThat(ZMatrix.newZerosLike(ZTensor.newOnes(2, 4)))
      .isEqualTo(ZMatrix.newMatrix(new int[][] { { 0, 0, 0, 0 }, { 0, 0, 0, 0 } }));
  }

  @Test
  public void test_newOnes() {
    assertThat(ZMatrix.newOnes(2, 3))
      .isEqualTo(ZMatrix.newMatrix(new int[][] { { 1, 1, 1 }, { 1, 1, 1 } }));

    assertThat(ZMatrix.newOnesLike(ZTensor.newOnes(2, 4)))
      .isEqualTo(ZMatrix.newMatrix(new int[][] { { 1, 1, 1, 1 }, { 1, 1, 1, 1 } }));
  }

  @Test
  public void test_newFilled() {
    assertThat(ZMatrix.newFilled(2, 3, 5))
      .isEqualTo(ZMatrix.newMatrix(new int[][] { { 5, 5, 5 }, { 5, 5, 5 } }));

    assertThat(ZMatrix.newFilledLike(ZTensor.newOnes(2, 4), 5))
      .isEqualTo(ZMatrix.newMatrix(new int[][] { { 5, 5, 5, 5 }, { 5, 5, 5, 5 } }));
  }

  @Test
  public void test_newIdentityMatrix() {
    assertThat(ZMatrix.newIdentityMatrix(3))
      .isEqualTo(ZMatrix.newMatrix(new int[][] { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } }));
  }

  @Test
  public void test_newDiagonalMatrix() {
    assertThat(ZMatrix.newDiagonalMatrix(1, 2, 3))
      .isEqualTo(ZMatrix.newDiagonalMatrix(List.of(1, 2, 3)))
      .isEqualTo(ZMatrix.newMatrix(new int[][] { { 1, 0, 0 }, { 0, 2, 0 }, { 0, 0, 3 } }));
  }

  @Test
  public void test_get() {
    var matrix = ZMatrix.newMatrix(new int[][] { { 1, 2 }, { 3, 4 } });
    assertThat(matrix.get(0, 0)).isEqualTo(1);
    assertThat(matrix.get(0, 1)).isEqualTo(2);
    assertThat(matrix.get(1, 0)).isEqualTo(3);
    assertThat(matrix.get(1, 1)).isEqualTo(4);
  }

  @Test
  public void test_shapeAsList() {
    var matrix = ZMatrix.newMatrix(new int[][] { { 1, 2 }, { 3, 4 } });
    assertThat(matrix.shapeAsList()).isEqualTo(List.of(2, 2));
  }

  @Test
  public void test_ndim() {
    var matrix = ZMatrix.newMatrix(new int[][] { { 1, 2, 3 }, { 3, 4, 5 } });
    assertThat(matrix.getInputNDim()).isEqualTo(3);
    assertThat(matrix.getOutputNDim()).isEqualTo(2);
  }

  @Test
  public void test_permute() {
    var matrix = ZMatrix.newMatrix(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
    assertThat(matrix.permuteInput(1, 0, 2))
      .isEqualTo(ZMatrix.newMatrix(new int[][] { { 2, 1, 3 }, { 5, 4, 6 } }));

    assertThat(matrix.permuteOutput(1, 0))
      .isEqualTo(ZMatrix.newMatrix(new int[][] { { 4, 5, 6 }, { 1, 2, 3 } }));
  }
}
