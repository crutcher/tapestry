package org.tensortapestry.zspace;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.tensortapestry.zspace.impl.HasJsonOutput;
import org.tensortapestry.zspace.indexing.Selector;

/**
 * A linear map from {@code Z^inDim} to {@code Z^outDim}.
 */
@Value
@Immutable
@ThreadSafe
@Jacksonized
@Builder(toBuilder = true)
@JsonPropertyOrder({ "projection", "offset" })
@EqualsAndHashCode(cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY)
public class ZAffineMap implements HasPermuteIO<ZAffineMap>, HasJsonOutput {

  /**
   * Create a new ZAffineMap which is an identity projection.
   *
   * @param n the number of dimensions.
   * @return the new matrix.
   */
  @Nonnull
  public static ZAffineMap newIdentityMap(int n) {
    return fromMatrix(ZMatrix.newIdentityMatrix(n));
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
  public static ZAffineMap fromMatrix(@Nonnull int[]... rows) {
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
  public static ZAffineMap fromMatrix(@Nonnull ZTensorWrapper matrix) {
    return new ZAffineMap(matrix);
  }

  @Nonnull
  public ZMatrix projection;

  @Nonnull
  public ZPoint offset;

  /**
   * Create a new ZAffineMap.
   *
   * @param projection the matrix.
   * @param offset the bias; if null, will be a zero vector of size
   *     {@code projection.shape[0]}.
   */
  @JsonCreator
  public ZAffineMap(@Nonnull ZTensorWrapper projection, @Nullable ZTensorWrapper offset) {
    this.projection = new ZMatrix(projection);
    if (offset == null) {
      offset = ZTensor.newZeros(this.projection.getOutputNDim());
    }
    this.offset = ZPoint.of(offset);

    if (this.offset.getNDim() != getOutputNDim()) {
      throw new IllegalArgumentException(
        String.format(
          "projection.shape[1] != offset.shape[0]: %s != %s",
          this.projection.shapeAsList(),
          this.offset.unwrap().shapeAsList()
        )
      );
    }
  }

  /**
   * Create a new ZAffineMap.
   *
   * @param projection the matrix.
   */
  public ZAffineMap(@Nonnull ZTensorWrapper projection) {
    this(projection, null);
  }

  @Override
  public int getInputNDim() {
    return projection.getInputNDim();
  }

  @Override
  public int getOutputNDim() {
    return projection.getOutputNDim();
  }

  @Override
  public String toString() {
    String res = "λx." + projection.toJsonString() + "⋅x";
    if (!offset.allZero()) {
      res += " + " + offset.toJsonString();
    }
    return res;
  }

  @Override
  @Nonnull
  public ZAffineMap permuteInput(@Nonnull int... permutation) {
    return new ZAffineMap(projection.permuteInput(permutation), offset);
  }

  @Override
  @Nonnull
  public ZAffineMap permuteOutput(@Nonnull int... permutation) {
    return new ZAffineMap(projection.permuteOutput(permutation), offset.permute(permutation));
  }

  /**
   * Apply this affine map to the given vector.
   *
   * @param x a 1-dim tensor of length `inDim`.
   * @return a 1-dim tensor of length `outDim`.
   */
  @Nonnull
  public ZTensor apply(@Nonnull ZTensorWrapper x) {
    return projection.matmul(x).add(offset);
  }

  /**
   * Broadcast apply this affine map to the given tensor.
   * <p>
   * If the expected input dimension is 2, and the provided input size is 3, then the first
   * dimension is carried over, and the affine map is applied to the last two dimensions.
   *
   * @param x input tensor
   * @return output tensor
   */
  @Nonnull
  public ZTensor broadcastApply(@Nonnull ZTensorWrapper x) {
    var t = x.unwrap();
    t.assertNDim(1);
    return ZTensor.concat(
      0,
      t.select(Selector.slice(0, -getInputNDim())),
      apply(t.select(Selector.slice(-getInputNDim(), null)))
    );
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

  /**
   * Translate this affine map by the given vector.
   *
   * @param x a 1-dim tensor of length `inDim`.
   * @return a new affine map.
   */
  @Nonnull
  public ZAffineMap translate(@Nonnull ZTensorWrapper x) {
    return new ZAffineMap(projection, offset.add(x));
  }
}
