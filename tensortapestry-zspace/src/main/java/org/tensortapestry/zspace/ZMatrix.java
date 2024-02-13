package org.tensortapestry.zspace;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.errorprone.annotations.Immutable;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import org.tensortapestry.common.runtime.ExcludeFromJacocoGeneratedReport;
import org.tensortapestry.zspace.ops.MatrixOps;

/**
 * An immutable 2D matrix.
 *
 * <p>Internally, represented by an immutable ZTensor.
 *
 * <p>Serializes as the JSON representation of a ZTensor.
 */
@ThreadSafe
@Immutable
@SuppressWarnings("Immutable")
public final class ZMatrix
  extends ImmutableZTensorWrapper<ZMatrix>
  implements HasPermuteIO<ZMatrix> {

  /**
   * Private constructor for Jackson.
   *
   * <p>This permits the public constructor to use {@link ZTensorWrapper}.
   *
   * @param tensor the tensor.
   * @return a new ZMatrix.
   */
  @JsonCreator
  @Nonnull
  @ExcludeFromJacocoGeneratedReport
  static ZMatrix privateCreator(@Nonnull ZTensor tensor) {
    return new ZMatrix(tensor);
  }

  /**
   * Create a ZMatrix from a matrix.
   *
   * <p>Will have a zero bias.
   *
   * @param rows the rows of the matrix.
   * @return the ZMatrix.
   */
  @Nonnull
  public static ZMatrix newMatrix(@Nonnull int[]... rows) {
    return newMatrix(ZTensor.newMatrix(rows));
  }

  /**
   * Create a ZMatrix from a matrix.
   *
   * <p>Will have a zero bias.
   *
   * @param matrix the matrix.
   * @return the ZMatrix.
   */
  @Nonnull
  public static ZMatrix newMatrix(@Nonnull ZTensorWrapper matrix) {
    return new ZMatrix(matrix);
  }

  /**
   * Create a ZMatrix of all zeros.
   *
   * @param rows the number of rows.
   * @param cols the number of columns.
   * @return a new ZMatrix.
   */
  @Nonnull
  public static ZMatrix newZeros(int rows, int cols) {
    return new ZMatrix(ZTensor.newZeros(rows, cols));
  }

  /**
   * Create a ZMatrix of all zeros in the same ZSpace as a reference ZMatrix.
   *
   * @param ref a reference ZMatrix.
   * @return a new ZMatrix.
   */
  @Nonnull
  public static ZMatrix newZerosLike(@Nonnull ZTensorWrapper ref) {
    return new ZMatrix(ZTensor.newZerosLike(ref));
  }

  /**
   * Create a ZMatrix of all ones in the given ZSpace.
   *
   * @param rows the number of rows.
   * @param cols the number of columns.
   * @return a new ZMatrix.
   */
  @Nonnull
  public static ZMatrix newOnes(int rows, int cols) {
    return new ZMatrix(ZTensor.newOnes(rows, cols));
  }

  /**
   * Create a ZMatrix of all ones in the same ZSpace as a reference ZMatrix.
   *
   * @param ref a reference ZMatrix.
   * @return a new ZMatrix.
   */
  @Nonnull
  public static ZMatrix newOnesLike(@Nonnull ZTensorWrapper ref) {
    return new ZMatrix(ZTensor.newOnesLike(ref));
  }

  /**
   * Create a ZMatrix of all the given value.
   *
   * @param rows the number of rows.
   * @param cols the number of columns.
   * @param value the value.
   * @return a new ZMatrix.
   */
  @Nonnull
  public static ZMatrix newFilled(int rows, int cols, int value) {
    return new ZMatrix(ZTensor.newFilled(new int[] { rows, cols }, value));
  }

  /**
   * Create a ZMatrix of all the given value in the same ZSpace as a reference value.
   *
   * @param ref a reference ZMatrix.
   * @param value the value.
   * @return a new ZMatrix.
   */
  @Nonnull
  public static ZMatrix newFilledLike(@Nonnull ZTensorWrapper ref, int value) {
    return new ZMatrix(ZTensor.newFilledLike(ref, value));
  }

  /**
   * Construct an identity matrix of size nxn.
   *
   * @param n the size of a side of the matrix.
   * @return the ZMatrix.
   */
  @Nonnull
  public static ZMatrix newIdentityMatrix(int n) {
    return new ZMatrix(ZTensor.newIdentityMatrix(n));
  }

  /**
   * Construct a diagonal matrix from a list of values.
   *
   * @param diag the values to put on the diagonal.
   * @return a new ZTensor.
   */
  @Nonnull
  public static ZMatrix newDiagonalMatrix(@Nonnull int... diag) {
    return new ZMatrix(ZTensor.newDiagonalMatrix(diag));
  }

  /**
   * Construct a diagonal matrix from a list of values.
   *
   * @param diag the values to put on the diagonal.
   * @return a new ZTensor.
   */
  @Nonnull
  public static ZMatrix newDiagonalMatrix(@Nonnull List<Integer> diag) {
    return new ZMatrix(ZTensor.newDiagonalMatrix(diag));
  }

  /**
   * Construct a diagonal matrix from a list of values.
   *
   * @param diag the values to put on the diagonal.
   * @return a new ZTensor.
   */
  @Nonnull
  public static ZMatrix newDiagonalMatrix(@Nonnull ZTensorWrapper diag) {
    return newDiagonalMatrix(diag.unwrap().toT1());
  }

  /**
   * Parse a ZMatrix from a string.
   *
   * @param source the string to parse.
   * @return the parsed ZMatrix.
   * @throws IllegalArgumentException if the string is not a valid ZMatrix.
   */
  @Nonnull
  public static ZMatrix parse(@Nonnull String source) {
    return new ZMatrix(ZTensor.parse(source));
  }

  /**
   * Create a ZMatrix.
   *
   * @param tensor the tensor.
   */
  public ZMatrix(@Nonnull ZTensorWrapper tensor) {
    super(tensor);
    this.tensor.assertNDim(2);
  }

  @Override
  @Nonnull
  protected ZMatrix create(@Nonnull ZTensorWrapper tensor) {
    return new ZMatrix(tensor);
  }

  /**
   * Convert this matrix to a 2D array.
   *
   * @return the 2D array.
   */
  public int[][] toArray() {
    return tensor.toT2();
  }

  /**
   * Get the value at the given row and column.
   *
   * @param row the row.
   * @param col the column.
   * @return the value.
   */
  public int get(int row, int col) {
    return tensor.get(row, col);
  }

  /**
   * Get the shape of this matrix.
   *
   * @return the shape.
   */
  public List<Integer> shapeAsList() {
    return tensor.shapeAsList();
  }

  /**
   * Get the number of rows in this matrix.
   *
   * @return the number of rows.
   */
  public int rows() {
    return tensor.shape(0);
  }

  /**
   * Get the number of columns in this matrix.
   *
   * @return the number of columns.
   */
  public int cols() {
    return tensor.shape(1);
  }

  @Override
  public int getOutputNDim() {
    return rows();
  }

  @Override
  public int getInputNDim() {
    return cols();
  }

  @Override
  @Nonnull
  public ZMatrix permuteInput(@Nonnull int... permutation) {
    return new ZMatrix(tensor.reorderedDimCopy(permutation, 1));
  }

  @Override
  @Nonnull
  public ZMatrix permuteOutput(@Nonnull int... permutation) {
    return new ZMatrix(tensor.reorderedDimCopy(permutation, 0));
  }

  @Nonnull
  public ZTensor matmul(@Nonnull ZTensorWrapper x) {
    return MatrixOps.matmul(this, x);
  }
}
