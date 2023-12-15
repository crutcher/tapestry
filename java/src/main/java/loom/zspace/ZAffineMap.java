package loom.zspace;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.common.HasToJsonString;
import loom.common.serialization.JsonUtil;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Objects;

/** A linear map from {@code Z^inDim} to {@code Z^outDim}. */
@ThreadSafe
@Immutable
@Jacksonized
@SuperBuilder
public final class ZAffineMap implements HasPermuteInput, HasPermuteOutput, HasToJsonString {
  @Nonnull
  @JsonProperty(value = "A")
  public final ZTensor A;

  @Nonnull public final ZTensor b;

  @JsonCreator
  public ZAffineMap(
      @JsonProperty(value = "A", required = true) ZTensor A,
      @JsonProperty(value = "b", required = true) ZTensor b) {
    this.A = A.asImmutable();
    this.b = b.asImmutable();

    A.assertNDim(2);
    b.assertNDim(1);
    if (b.shape(0) != outputDim()) {
      throw new IllegalArgumentException(
          String.format("A.shape[1] != b.shape[0]: %s != %s", A.shapeAsList(), b.shapeAsList()));
    }
  }

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
   * Creates a reordered view of this tensor along a specified dimension.
   *
   * <p><b>Example:</b> Suppose we have tensor "t" with shape [2,3]:
   *
   * <pre>
   * t = [[0, 1, 2],
   *      [3, 4, 5]]
   * </pre>
   *
   * If we call {@code t.reorderDim([1,0,2], 1)}, the returned tensor will look like:
   *
   * <pre>
   * v = [[1, 0, 2],
   *      [4, 3, 5]]
   * </pre>
   *
   * <p>Supports negative dimension indexing - i.e. -1 represents the last dimension, -2 represents
   * the second last, and so on.
   *
   * @param tensor The tensor.
   * @param permutation An array of unique integers representing the new order of indices along the
   *     specified dimension. Each integer should be a valid index for that dimension.
   * @param dim Index of the dimension to be reordered. Dimensions are zero-indexed. This must be a
   *     valid dimension of this tensor.
   * @return A new ZTensor, with the specified dimension reordered. This view shares data with the
   *     original tensor.
   */
  @Nonnull
  public static ZTensor reorderDim(@Nonnull ZTensor tensor, @Nonnull int[] permutation, int dim) {
    var d = tensor.resolveDim(dim);
    var shape = tensor.shapeAsArray();
    var perm = IndexingFns.resolvePermutation(permutation, shape[d]);
    var res = ZTensor.newZeros(shape);
    for (int i = 0; i < shape[d]; ++i) {
      res.selectDim(d, i).assign(tensor.selectDim(d, perm[i]));
    }
    return res;
  }

  // This seems like a backwards way to represent this;
  // but `Ax + b` is the standard form.
  public int inputDim() {
    return A.shape(1);
  }

  public int outputDim() {
    return A.shape(0);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ZAffineMap that)) return false;
    return A.equals(that.A) && b.equals(that.b);
  }

  @Override
  public int hashCode() {
    return Objects.hash(A, b);
  }

  @Override
  public String toString() {
    return toJsonString();
  }

  @Override
  public ZAffineMap permuteInput(@Nonnull int... permutation) {
    return new ZAffineMap(reorderDim(A, permutation, 1), b);
  }

  @Override
  public ZAffineMap permuteOutput(@Nonnull int... permutation) {
    return new ZAffineMap(reorderDim(A, permutation, 0), reorderDim(b, permutation, 0));
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
   * Apply this affine map to the given ZPoint.
   *
   * @param x a ZPoint of dim `inDim`.
   * @return a ZPoint of dim `outDim`.
   */
  @Nonnull
  public ZPoint apply(@Nonnull ZPoint x) {
    return new ZPoint(apply(x.coords));
  }
}
