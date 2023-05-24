package loom.zspace;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import loom.common.HasToJsonString;
import loom.common.JsonUtil;

/** A linear map from {@code Z^inDim} to {@code Z^outDim}. */
@ThreadSafe
@Immutable
public class ZAffineMap implements HasPermuteInput, HasPermuteOutput, HasToJsonString {
  public final ZTensor A;
  public final ZTensor b;

  @JsonIgnore public final int inputDim;
  @JsonIgnore public final int outputDim;

  @JsonCreator
  public ZAffineMap(@JsonProperty("A") ZTensor A, @JsonProperty("b") ZTensor b) {
    A.assertNdim(2);
    // This seems like a backwards way to represent this;
    // but `Ax + b` is the standard form.
    outputDim = A.shapeAsList().get(0);
    inputDim = A.shapeAsList().get(1);

    b.assertNdim(1);
    if (b.shapeAsList().get(0) != outputDim) {
      throw new IllegalArgumentException(
          String.format("A.shape[1] != b.shape[0]: %s != %s", A.shapeAsList(), b.shapeAsList()));
    }

    this.A = A.immutable();
    this.b = b.immutable();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ZAffineMap)) return false;
    ZAffineMap that = (ZAffineMap) o;
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

  /**
   * Parse a ZAffineMap from a string.
   *
   * @param str the string to parse.
   * @return the parsed ZAffineMap.
   */
  public static ZAffineMap parse(String str) {
    return JsonUtil.fromJson(str, ZAffineMap.class);
  }

  @Override
  public ZAffineMap permuteInput(int... permutation) {
    return new ZAffineMap(A.reorderDim(permutation, 1), b);
  }

  @Override
  public ZAffineMap permuteOutput(int... permutation) {
    return new ZAffineMap(A.reorderDim(permutation, 0), b.reorderDim(permutation, 0));
  }

  /**
   * Apply this affine map to the given vector.
   *
   * @param x a 1-dim tensor of length `inDim`.
   * @return a 1-dim tensor of length `outDim`.
   */
  public ZTensor apply(ZTensor x) {
    // denoted in the `in` dim.
    x.assertNdim(1);
    if (x.shapeAsList().get(0) != inputDim) {
      throw new IllegalArgumentException(
          String.format("A.shape[1] != x.shape[0]: %s != %s", A.shapeAsList(), x.shapeAsList()));
    }

    // denoted in the `out` dim.
    var res = b.clone(true);

    for (int j = 0; j < inputDim; j++) {
      for (int i = 0; i < outputDim; i++) {
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
  public ZPoint apply(ZPoint x) {
    return new ZPoint(apply(x.coords));
  }
}
