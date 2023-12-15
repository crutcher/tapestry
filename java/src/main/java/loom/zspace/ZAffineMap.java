package loom.zspace;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.common.HasToJsonString;
import loom.common.serialization.JsonUtil;

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
    return new ZAffineMap(A.reorderDim(permutation, 1), b);
  }

  @Override
  public ZAffineMap permuteOutput(@Nonnull int... permutation) {
    return new ZAffineMap(A.reorderDim(permutation, 0), b.reorderDim(permutation, 0));
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
    if (x.shape(0) != inputDim()) {
      throw new IllegalArgumentException(
          String.format("A.shape[1] != x.shape[0]: %s != %s", A.shapeAsList(), x.shapeAsList()));
    }

    // denoted in the `out` dim.
    var res = b.clone(true);

    for (int j = 0; j < inputDim(); j++) {
      for (int i = 0; i < outputDim(); i++) {
        res.set(new int[] {i}, res.get(i) + A.get(i, j) * x.get(j));
      }
    }
    return res;
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
