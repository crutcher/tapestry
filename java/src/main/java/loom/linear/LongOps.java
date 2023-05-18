package loom.linear;

import java.util.Arrays;
import java.util.function.BinaryOperator;
import java.util.function.LongFunction;

/** Tensor-like utility mathematics operations on `long[]` and `long[][]`. */
public final class LongOps {

  private LongOps() {}

  /**
   * Is the given vector empty?
   *
   * @param vec the vector.
   * @return true if the vector is empty.
   */
  public static boolean isEmpty(long[] vec) {
    return vec.length == 0;
  }

  /**
   * Is the given matrix empty?
   *
   * @param mat the matrix.
   * @return true if the matrix is empty.
   */
  public static boolean isEmpty(long[][] mat) {
    return mat.length == 0 || mat[0].length == 0;
  }

  /**
   * Format the given vector as a string.
   *
   * @param vec the vector.
   * @return the formatted string.
   */
  public static String format(long[] vec) {
    return Arrays.toString(vec);
  }

  /**
   * Format the given matrix as a string.
   *
   * @param mat the matrix.
   * @return the formatted string.
   */
  public static String format(long[][] mat) {
    if (isEmpty(mat)) {
      return "[[]]";
    }

    StringBuilder sb = new StringBuilder();
    sb.append("[");
    for (int i = 0; i < mat.length; i++) {
      sb.append(Arrays.toString(mat[i]));
      if (i < mat.length - 1) {
        sb.append(", ");
      }
    }
    sb.append("]");
    return sb.toString();
  }

  /**
   * Format the given matrix as a string.
   *
   * @param mat the matrix.
   * @param initialPrefix the string to prepend.
   * @param joinPrefix the string to add between joins.
   * @return the result.
   */
  public static String formatPretty(long[][] mat, String initialPrefix, String joinPrefix) {
    if (isEmpty(mat)) {
      return initialPrefix + "[[]]";
    }

    StringBuilder sb = new StringBuilder();
    sb.append(initialPrefix + "[");
    for (int i = 0; i < mat.length; i++) {
      sb.append(joinPrefix + "  " + Arrays.toString(mat[i]));
      if (i < mat.length - 1) {
        sb.append(",");
      }
    }
    sb.append(joinPrefix + "]");
    return sb.toString();
  }

  /**
   * Create a new `long[]` with the given shape and fill value.
   *
   * @param ndim the number of dimensions.
   * @param value the fill value.
   * @return the new array.
   */
  public static long[] vec_full(int ndim, long value) {
    long[] result = new long[ndim];
    Arrays.fill(result, value);
    return result;
  }

  /**
   * Create a new `long[]` with a shape matching the `ref`, and the given fill value.
   *
   * @param ref the reference vector.
   * @param value the fill value.
   * @return the new array.
   */
  public static long[] full_like(long[] ref, long value) {
    long[] result = new long[ref.length];
    Arrays.fill(result, value);
    return result;
  }

  /**
   * Create a new `long[]` full of zeros.
   *
   * @param ndim the number of dimensions.
   * @return the new array.
   */
  public static long[] zeros(int ndim) {
    return vec_full(ndim, 0);
  }

  /**
   * Create a new `long[]` with a shape matching the `ref`, `0` as the fill value.
   *
   * @param ref the reference vector.
   * @return the new array.
   */
  public static long[] zeros_like(long[] ref) {
    return full_like(ref, 0);
  }

  /**
   * Create a new `long[]` full of ones.
   *
   * @param ndim the number of dimensions.
   * @return the new array.
   */
  public static long[] vec_ones(int ndim) {
    return vec_full(ndim, 1);
  }

  /**
   * Create a new `long[]` with a shape matching the `ref`, `1` as the fill value.
   *
   * @param ref the reference vector.
   * @return the new array.
   */
  public static long[] ones_like(long[] ref) {
    return full_like(ref, 1);
  }

  /**
   * Create a new `long[][]` full of zeros. Return in row-major order.
   *
   * @param rows the number of rows.
   * @param cols the number of columns.
   * @return the new array.
   */
  public static long[][] mat_zeros(int rows, int cols) {
    long[][] result = new long[rows][cols];
    for (var row : result) {
      Arrays.fill(row, 0);
    }
    return result;
  }

  /**
   * Create a new `long[][]` with a shape matching the `ref`, `0` as the fill value.
   *
   * @param ref the reference matrix.
   * @return a new matrix.
   */
  public static long[][] zeros_like(long[][] ref) {
    int rows = ref.length;
    int cols = rows > 0 ? ref[0].length : 0;
    return mat_zeros(rows, cols);
  }

  /**
   * Construct an identity matrix.
   *
   * @param ndim the number of dimensions.
   * @return a new matrix.
   */
  public static long[][] identity(int ndim) {
    return diaganol(vec_ones(ndim));
  }

  /**
   * Construct a diagonal matrix.
   *
   * @param diag the diagonal values.
   * @return a new matrix.
   */
  public static long[][] diaganol(long[] diag) {
    long[][] result = mat_zeros(diag.length, diag.length);
    for (int i = 0; i < diag.length; i++) {
      result[i][i] = diag[i];
    }
    return result;
  }

  /**
   * Return the shape of a matrix.
   *
   * @param val the matrix.
   * @return the shape of the matrix.
   */
  public static long[] shape(long[][] val) {
    long[] s = new long[2];
    s[0] = val.length;
    if (val.length > 0) {
      s[1] = val[0].length;
    }
    return s;
  }

  /**
   * Verify that a and b have the same shape.
   *
   * @param lhs a vector.
   * @param rhs a vector.
   * @throws LinearDimError on mismatched shape.
   */
  public static void checkDimsMatch(long[] lhs, long[] rhs) {
    if (lhs.length != rhs.length) {
      throw new LinearDimError(
          String.format("dims mismatch: |%d| vs |%d|", lhs.length, rhs.length));
    }
  }

  /**
   * Verify that a and b have the same shape.
   *
   * @param lhs a vector.
   * @param rhs a vector.
   * @throws LinearDimError on mismatched shape.
   */
  public static void checkDimsMatch(long[][] lhs, long[][] rhs) {
    if (lhs.length != rhs.length || (lhs.length > 0 && lhs[0].length != rhs[0].length)) {
      int lhs_rows = lhs.length;
      int lhs_cols = lhs_rows > 0 ? lhs[0].length : 0;

      int rhs_rows = rhs.length;
      int rhs_cols = rhs_rows > 0 ? rhs[0].length : 0;

      throw new LinearDimError(
          String.format(
              "dims mismatch: |%d, %d| vs |%d, %d|", lhs_rows, lhs_cols, rhs_rows, rhs_cols));
    }
  }

  /**
   * Apply a unary operation to a vector.
   *
   * @param op a unary operation.
   * @param val a vector.
   * @return a new vector.
   */
  public static long[] uniOp(LongFunction<Long> op, long[] val) {
    long[] result = new long[val.length];
    for (int i = 0; i < val.length; i++) {
      result[i] = op.apply(val[i]);
    }
    return result;
  }

  /**
   * Negate a vector.
   *
   * @param val a vector.
   * @return a new vector.
   */
  public static long[] neg(long[] val) {
    return uniOp((x) -> -x, val);
  }

  /**
   * Apply a binary operation to two vectors.
   *
   * @param op the binary operation.
   * @param lhs the lhs vector.
   * @param rhs the rhs vector.
   * @return a new vector.
   * @throws LinearDimError on mismatched shape.
   */
  public static long[] binOp(BinaryOperator<Long> op, long[] lhs, long[] rhs) {
    checkDimsMatch(lhs, rhs);
    long[] result = new long[lhs.length];
    for (int i = 0; i < lhs.length; i++) {
      result[i] = op.apply(lhs[i], rhs[i]);
    }
    return result;
  }

  /**
   * Apply a binary operation to a vector and a scalar.
   *
   * @param op the binary operation.
   * @param lhs the lhs vector.
   * @param rhs the rhs scalar.
   * @return a new vector.
   */
  public static long[] binOp(BinaryOperator<Long> op, long[] lhs, long rhs) {
    long[] result = new long[lhs.length];
    for (int i = 0; i < lhs.length; i++) {
      result[i] = op.apply(lhs[i], rhs);
    }
    return result;
  }

  /**
   * Apply a binary operation to a scalar and a vector.
   *
   * @param op the binary operation.
   * @param lhs the lhs scalar.
   * @param rhs the rhs vector.
   * @return a new vector.
   */
  public static long[] binOp(BinaryOperator<Long> op, long lhs, long[] rhs) {
    long[] result = new long[rhs.length];
    for (int i = 0; i < rhs.length; i++) {
      result[i] = op.apply(lhs, rhs[i]);
    }
    return result;
  }

  /**
   * Apply a binary operation to two matrices.
   *
   * @param op the binary operation.
   * @param lhs the lhs matrix.
   * @param rhs the rhs matrix.
   * @return a new vector.
   * @throws LinearDimError on mismatched shape.
   */
  public static long[][] binOp(BinaryOperator<Long> op, long[][] lhs, long[][] rhs) {
    checkDimsMatch(lhs, rhs);
    long[][] result = zeros_like(lhs);
    for (int r = 0; r < lhs.length; r++) {
      for (int c = 0; c < lhs[0].length; c++) {
        result[r][c] = op.apply(lhs[r][c], rhs[r][c]);
      }
    }
    return result;
  }

  /**
   * Apply a binary operation to a matrix and a scalar.
   *
   * @param op the binary operation.
   * @param lhs the lhs matrix.
   * @param rhs the rhs scalar.
   * @return a new vector.
   */
  public static long[][] binOp(BinaryOperator<Long> op, long[][] lhs, long rhs) {
    long[][] result = zeros_like(lhs);
    for (int r = 0; r < lhs.length; r++) {
      for (int c = 0; c < lhs[0].length; c++) {
        result[r][c] = op.apply(lhs[r][c], rhs);
      }
    }
    return result;
  }

  /**
   * Apply a binary operation to a scalar and a matrix.
   *
   * @param op the binary operation.
   * @param lhs the lhs matrix.
   * @param rhs the rhs scalar.
   * @return a new vector.
   */
  public static long[][] binOp(BinaryOperator<Long> op, long lhs, long[][] rhs) {
    long[][] result = zeros_like(rhs);
    for (int r = 0; r < rhs.length; r++) {
      for (int c = 0; c < rhs[0].length; c++) {
        result[r][c] = op.apply(lhs, rhs[r][c]);
      }
    }
    return result;
  }

  /**
   * Add two vectors.
   *
   * @param lhs the lhs vector.
   * @param rhs the rhs vector.
   * @return a new vector.
   * @throws LinearDimError on mismatched shape.
   */
  public static long[] add(long[] lhs, long[] rhs) {
    return binOp(Long::sum, lhs, rhs);
  }

  /**
   * Add a scalar to a vector.
   *
   * @param lhs the lhs vector.
   * @param rhs the rhs scalar.
   * @return a new vector.
   */
  public static long[] add(long[] lhs, long rhs) {
    return binOp(Long::sum, lhs, rhs);
  }

  /**
   * Add a scalar to a vector.
   *
   * @param lhs the lhs scalar.
   * @param rhs the rhs vector.
   * @return a new vector.
   */
  public static long[] add(long lhs, long[] rhs) {
    return binOp(Long::sum, lhs, rhs);
  }

  /**
   * Add two matrices.
   *
   * @param lhs the lhs matrix.
   * @param rhs the rhs matrix.
   * @return a new matrix.
   * @throws LinearDimError on mismatched shape.
   */
  public static long[][] add(long[][] lhs, long[][] rhs) {
    return binOp(Long::sum, lhs, rhs);
  }

  /**
   * Add a scalar to a matrix.
   *
   * @param lhs the lhs matrix.
   * @param rhs the rhs scalar.
   * @return a new matrix.
   */
  public static long[][] add(long[][] lhs, long rhs) {
    return binOp(Long::sum, lhs, rhs);
  }

  /**
   * Add a scalar to a matrix.
   *
   * @param lhs the lhs scalar.
   * @param rhs the rhs matrix.
   * @return a new matrix.
   */
  public static long[][] add(long lhs, long[][] rhs) {
    return binOp(Long::sum, lhs, rhs);
  }

  /**
   * Subtract two vectors.
   *
   * @param lhs the lhs vector.
   * @param rhs the rhs vector.
   * @return a new vector.
   * @throws LinearDimError on mismatched shape.
   */
  public static long[] sub(long[] lhs, long[] rhs) {
    return binOp((x, y) -> x - y, lhs, rhs);
  }

  /**
   * Subtract a scalar from a vector.
   *
   * @param lhs the lhs vector.
   * @param rhs the rhs scalar.
   * @return a new vector.
   */
  public static long[] sub(long[] lhs, long rhs) {
    return binOp((x, y) -> x - y, lhs, rhs);
  }

  /**
   * Subtract a vector from a scalar.
   *
   * @param lhs the lhs scalar.
   * @param rhs the rhs vector.
   * @return a new vector.
   */
  public static long[] sub(long lhs, long[] rhs) {
    return binOp((x, y) -> x - y, lhs, rhs);
  }

  /**
   * Subtract two matrices.
   *
   * @param lhs the lhs matrix.
   * @param rhs the rhs matrix.
   * @return a new matrix.
   * @throws LinearDimError on mismatched shape.
   */
  public static long[][] sub(long[][] lhs, long[][] rhs) {
    return binOp((x, y) -> x - y, lhs, rhs);
  }

  /**
   * Subtract a scalar from a matrix.
   *
   * @param lhs the lhs matrix.
   * @param rhs the rhs scalar.
   * @return a new matrix.
   */
  public static long[][] sub(long[][] lhs, long rhs) {
    return binOp((x, y) -> x - y, lhs, rhs);
  }

  /**
   * Subtract a scalar from a matrix.
   *
   * @param lhs the lhs scalar.
   * @param rhs the rhs matrix.
   * @return a new matrix.
   */
  public static long[][] sub(long lhs, long[][] rhs) {
    return binOp((x, y) -> x - y, lhs, rhs);
  }

  /**
   * Multiply two matrices.
   *
   * @param lhs the lhs matrix.
   * @param rhs the rhs matrix.
   * @return a new matrix.
   * @throws LinearDimError on mismatched shape.
   */
  public static long[] mul(long[] lhs, long[] rhs) {
    return binOp((x, y) -> x * y, lhs, rhs);
  }

  /**
   * Multiply a matrix by a scalar.
   *
   * @param lhs the lhs matrix.
   * @param rhs the rhs scalar.
   * @return a new matrix.
   */
  public static long[] mul(long[] lhs, long rhs) {
    return binOp((x, y) -> x * y, lhs, rhs);
  }

  /**
   * Multiply a scalar by a matrix.
   *
   * @param lhs the rhs scalar.
   * @param rhs the rhs matrix.
   * @return a new matrix.
   */
  public static long[] mul(long lhs, long[] rhs) {
    return binOp((x, y) -> x * y, lhs, rhs);
  }

  /**
   * Multiply two matrices.
   *
   * @param lhs the lhs matrix.
   * @param rhs the rhs matrix.
   * @return a new matrix.
   * @throws LinearDimError on mismatched shape.
   */
  public static long[][] mul(long[][] lhs, long[][] rhs) {
    return binOp((x, y) -> x * y, lhs, rhs);
  }

  /**
   * Multiply a scalar to a matrix.
   *
   * @param lhs the lhs matrix.
   * @param rhs the rhs scalar.
   * @return a new matrix.
   */
  public static long[][] mul(long[][] lhs, long rhs) {
    return binOp((x, y) -> x * y, lhs, rhs);
  }

  /**
   * Multiply a scalar to a matrix.
   *
   * @param lhs the lhs scalar.
   * @param rhs the rhs matrix.
   * @return a new matrix.
   */
  public static long[][] mul(long lhs, long[][] rhs) {
    return binOp((x, y) -> x * y, lhs, rhs);
  }

  /**
   * Divide two vectors.
   *
   * @param lhs the lhs vector.
   * @param rhs the rhs vector.
   * @return a new vector.
   * @throws LinearDimError on mismatched shape.
   * @throws ArithmeticException on division by 0.
   */
  public static long[] div(long[] lhs, long[] rhs) {
    return binOp((x, y) -> x / y, lhs, rhs);
  }

  /**
   * Divide a vector by a scalar.
   *
   * @param lhs the lhs vector.
   * @param rhs the rhs scalar.
   * @return a new vector.
   * @throws ArithmeticException on division by 0.
   */
  public static long[] div(long[] lhs, long rhs) {
    return binOp((x, y) -> x / y, lhs, rhs);
  }

  /**
   * Divide a scalar by a vector.
   *
   * @param lhs the lhs scalar.
   * @param rhs the rhs vector.
   * @return a new vector.
   * @throws ArithmeticException on division by 0.
   */
  public static long[] div(long lhs, long[] rhs) {
    return binOp((x, y) -> x / y, lhs, rhs);
  }

  /**
   * Divide two matrices.
   *
   * @param lhs the lhs matrix.
   * @param rhs the rhs matrix.
   * @return a new matrix.
   * @throws LinearDimError on mismatched shape.
   * @throws ArithmeticException on division by 0.
   */
  public static long[][] div(long[][] lhs, long[][] rhs) {
    return binOp((x, y) -> x / y, lhs, rhs);
  }

  /**
   * Divide a scalar to a matrix.
   *
   * @param lhs the lhs matrix.
   * @param rhs the rhs scalar.
   * @return a new matrix.
   * @throws ArithmeticException on division by 0.
   */
  public static long[][] div(long[][] lhs, long rhs) {
    return binOp((x, y) -> x / y, lhs, rhs);
  }

  /**
   * Divide a scalar to a matrix.
   *
   * @param lhs the lhs scalar.
   * @param rhs the rhs matrix.
   * @return a new matrix.
   * @throws ArithmeticException on division by 0.
   */
  public static long[][] div(long lhs, long[][] rhs) {
    return binOp((x, y) -> x / y, lhs, rhs);
  }

  /**
   * The cell-wise modulo of two vectors.
   *
   * @param lhs the lhs vector.
   * @param rhs the rhs vector.
   * @return a new matrix.
   * @throws LinearDimError on mismatched shape.
   * @throws ArithmeticException on division by 0.
   */
  public static long[] mod(long[] lhs, long[] rhs) {
    return binOp((x, y) -> x % y, lhs, rhs);
  }

  /**
   * The cell-wise modulo of a vector and a scalar.
   *
   * @param lhs the lhs vector.
   * @param rhs the rhs scalar.
   * @return a new matrix.
   * @throws ArithmeticException on division by 0.
   */
  public static long[] mod(long[] lhs, long rhs) {
    return binOp((x, y) -> x % y, lhs, rhs);
  }

  /**
   * The cell-wise modulo of a scalar and a vector.
   *
   * @param lhs the lhs scalar.
   * @param rhs the rhs vector.
   * @return a new matrix.
   * @throws ArithmeticException on division by 0.
   */
  public static long[] mod(long lhs, long[] rhs) {
    return binOp((x, y) -> x % y, lhs, rhs);
  }

  /**
   * The cell-wise modulo of two matrices.
   *
   * @param lhs the lhs matrix.
   * @param rhs the rhs matrix.
   * @return a new matrix.
   * @throws LinearDimError on mismatched shape.
   * @throws ArithmeticException on division by 0.
   */
  public static long[][] mod(long[][] lhs, long[][] rhs) {
    return binOp((x, y) -> x % y, lhs, rhs);
  }

  /**
   * The cell-wise modulo of a matrix and a scalar.
   *
   * @param lhs the lhs matrix.
   * @param rhs the rhs scalar.
   * @return a new matrix.
   * @throws ArithmeticException on division by 0.
   */
  public static long[][] mod(long[][] lhs, long rhs) {
    return binOp((x, y) -> x % y, lhs, rhs);
  }

  /**
   * The cell-wise modulo of a scalar and a matrix.
   *
   * @param lhs the lhs scalar.
   * @param rhs the rhs matrix.
   * @return a new matrix.
   * @throws ArithmeticException on division by 0.
   */
  public static long[][] mod(long lhs, long[][] rhs) {
    return binOp((x, y) -> x % y, lhs, rhs);
  }

  /**
   * Compute the partial ordering of two vectors. They must have the same shape.
   *
   * @param lhs the lhs vector.
   * @param rhs the rhs vector.
   * @return the partial ordering.
   * @throws LinearDimError on mismatched shape.
   */
  public static PartialOrdering partialCompare(long[] lhs, long[] rhs) {
    checkDimsMatch(lhs, rhs);
    boolean lt = false;
    boolean gt = false;
    for (int i = 0; i < lhs.length; i++) {
      if (lhs[i] < rhs[i]) lt = true;
      if (lhs[i] > rhs[i]) gt = true;
    }
    if (lt && gt) return PartialOrdering.INCOMPARABLE;
    if (lt) return PartialOrdering.LESS_THAN;
    if (gt) return PartialOrdering.GREATER_THAN;
    return PartialOrdering.EQUAL;
  }

  /**
   * Is `lhs` equal to `rhs`? They must have the same shape.
   *
   * @param lhs the lhs vector.
   * @param rhs the rhs vector.
   * @return true if equal, false otherwise.
   * @throws LinearDimError on mismatched shape.
   */
  public static boolean eq(long[] lhs, long[] rhs) {
    checkDimsMatch(lhs, rhs);
    for (int i = 0; i < lhs.length; i++) {
      if (lhs[i] != rhs[i]) return false;
    }
    return true;
  }

  /**
   * Is `lhs` not equal to `rhs`? They must have the same shape.
   *
   * @param lhs the lhs vector.
   * @param rhs the rhs vector.
   * @return true if not equal, false otherwise.
   * @throws LinearDimError on mismatched shape.
   */
  public static boolean ne(long[] lhs, long[] rhs) {
    return !eq(lhs, rhs);
  }

  /**
   * Is `lhs` less than `rhs` under partial ordering? They must have the same shape.
   *
   * @param lhs the lhs vector.
   * @param rhs the rhs vector.
   * @return true if less than, false otherwise.
   * @throws LinearDimError on mismatched shape.
   */
  public static boolean lt(long[] lhs, long[] rhs) {
    return partialCompare(lhs, rhs) == PartialOrdering.LESS_THAN;
  }

  /**
   * Is `lhs` less than or equal to `rhs` under partial ordering? They must have the same shape.
   *
   * @param lhs the lhs vector.
   * @param rhs the rhs vector.
   * @return true if less than or equal to, false otherwise.
   * @throws LinearDimError on mismatched shape.
   */
  public static boolean le(long[] lhs, long[] rhs) {
    var ordering = partialCompare(lhs, rhs);
    return (ordering == PartialOrdering.LESS_THAN || ordering == PartialOrdering.EQUAL);
  }

  /**
   * Is `lhs` greater than `rhs` under partial ordering? They must have the same shape.
   *
   * @param lhs the lhs vector.
   * @param rhs the rhs vector.
   * @return true if greater than, false otherwise.
   * @throws LinearDimError on mismatched shape.
   */
  public static boolean gt(long[] lhs, long[] rhs) {
    return partialCompare(lhs, rhs) == PartialOrdering.GREATER_THAN;
  }

  /**
   * Is `lhs` greater than or equal to `rhs` under partial ordering? They must have the same shape.
   *
   * @param lhs the lhs vector.
   * @param rhs the rhs vector.
   * @return true if greater than or equal to, false otherwise.
   * @throws LinearDimError on mismatched shape.
   */
  public static boolean ge(long[] lhs, long[] rhs) {
    var ordering = partialCompare(lhs, rhs);
    return (ordering == PartialOrdering.GREATER_THAN || ordering == PartialOrdering.EQUAL);
  }
}
