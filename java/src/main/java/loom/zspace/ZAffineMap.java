package loom.zspace;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import loom.common.json.HasToJsonString;
import loom.common.json.JsonUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Objects;

/** A linear map from {@code Z^inDim} to {@code Z^outDim}. */
@ThreadSafe
@Immutable
@Jacksonized
@Builder(toBuilder = true)
public final class ZAffineMap
    implements HasPermuteInput<ZAffineMap>, HasPermuteOutput<ZAffineMap>, HasToJsonString {

  /**
   * Parse a ZAffineMap from a string.
   *
   * @param str the string to parse.
   * @return the parsed ZAffineMap.
   */
  @Nonnull
  public static ZAffineMap parse(@Nonnull String str) {
    return JsonUtil.fromJson(str, ZAffineMap.class);
  }

  /**
   * Create a ZAffineMap from a matrix.
   *
   * <p>Will have a zero bias.
   *
   * @param rows the rows of the matrix.
   * @return the ZAffineMap.
   */
  @Nonnull
  public static ZAffineMap fromMatrix(int[]... rows) {
    return fromMatrix(ZTensor.newMatrix(rows));
  }

  /**
   * Create a ZAffineMap from a matrix.
   *
   * <p>Will have a zero bias.
   *
   * @param matrix the matrix.
   * @return the ZAffineMap.
   */
  @Nonnull
  public static ZAffineMap fromMatrix(@Nonnull ZTensor matrix) {
    return new ZAffineMap(matrix);
  }

  @Nonnull
  @JsonProperty(value = "A")
  public final ZTensor A;

  @Nonnull public final ZTensor b;

  /**
   * Create a new ZAffineMap.
   *
   * @param A the matrix.
   * @param b the bias; if null, will be a zero vector of size `A.shape[0]`.
   */
  @JsonCreator
  public ZAffineMap(
      @JsonProperty(value = "A", required = true) ZTensor A,
      @Nullable @JsonProperty(value = "b") ZTensor b) {
    this.A = A.asImmutable();
    if (b == null) {
      b = ZTensor.newZeros(A.shape(0));
    }
    this.b = b.asImmutable();

    A.assertNDim(2);
    b.assertNDim(1);
    if (b.shape(0) != outputNDim()) {
      throw new IllegalArgumentException(
          String.format("A.shape[1] != b.shape[0]: %s != %s", A.shapeAsList(), b.shapeAsList()));
    }
  }

  /**
   * Create a new ZAffineMap.
   *
   * @param A the matrix.
   */
  public ZAffineMap(@Nonnull ZTensor A) {
    this(A, null);
  }

  /**
   * Get the output dimension of this affine map.
   *
   * @return the output dimension.
   */
  public int outputNDim() {
    return A.shape(0);
  }

  /**
   * Get the input dimension of this affine map.
   *
   * @return the input dimension.
   */
  // This seems like a backwards way to represent this;
  // but `Ax + b` is the standard form.
  public int inputNDim() {
    return A.shape(1);
  }

  @Override
  public int hashCode() {
    return Objects.hash(A, b);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ZAffineMap that)) return false;
    return A.equals(that.A) && b.equals(that.b);
  }

  @Override
  public String toString() {
    return toJsonString();
  }

  @Override
  public ZAffineMap permuteInput(@Nonnull int... permutation) {
    return new ZAffineMap(A.reorderedDimCopy(permutation, 1), b);
  }

  @Override
  public ZAffineMap permuteOutput(@Nonnull int... permutation) {
    return new ZAffineMap(A.reorderedDimCopy(permutation, 0), b.reorderedDimCopy(permutation, 0));
  }

  /**
   * Apply this affine map to the given ZPoint.
   *
   * @param x a ZPoint of dim `inDim`.
   * @return a ZPoint of dim `outDim`.
   */
  @Nonnull
  public ZPoint apply(@Nonnull ZPoint x) {
    return new ZPoint(apply(x.coords));
  }

  /**
   * Apply this affine map to the given vector.
   *
   * @param x a 1-dim tensor of length `inDim`.
   * @return a 1-dim tensor of length `outDim`.
   */
  @Nonnull
  public ZTensor apply(@Nonnull ZTensor x) {
    // denoted in the `in` dim.
    x.assertNDim(1);
    return ZTensor.Ops.matmul(A, x).add(b);
  }

  /**
   * Translate this affine map by the given vector.
   *
   * @param x a 1-dim tensor of length `inDim`.
   * @return a new affine map.
   */
  @Nonnull
  public ZAffineMap translate(@Nonnull ZTensor x) {
    return new ZAffineMap(A, b.add(x));
  }

  /**
   * Translate this affine map by the given vector.
   *
   * @param x a ZPoint of dim `inDim`.
   * @return a new affine map.
   */
  @Nonnull
  public ZAffineMap translate(@Nonnull ZPoint x) {
    return translate(x.coords);
  }

  /**
   * Translate this affine map by the given vector.
   *
   * @param x a 1-dim array of length `inDim`.
   * @return a new affine map.
   */
  @Nonnull
  public ZAffineMap translate(@Nonnull int... x) {
    return translate(ZTensor.newVector(x));
  }
}
