package org.tensortapestry.loom.zspace;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.tensortapestry.loom.zspace.impl.HasJsonOutput;

/**
 * A linear map from {@code Z^inDim} to {@code Z^outDim}.
 */
@Value
@Immutable
@ThreadSafe
@Jacksonized
@Builder(toBuilder = true)
@JsonPropertyOrder({ "projection", "offset" })
public class ZAffineMap implements HasPermuteIO<ZAffineMap>, HasJsonOutput {

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
  public static ZAffineMap fromMatrix(@Nonnull HasZTensor matrix) {
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
   *         {@code projection.shape[0]}.
   */
  @JsonCreator
  public ZAffineMap(@Nonnull HasZTensor projection, @Nullable HasZTensor offset) {
    this.projection = new ZMatrix(projection);
    if (offset == null) {
      offset = ZTensor.newZeros(this.projection.getOutputNDim());
    }
    var zoffset = offset.getTensor();
    this.offset = zoffset.newZPoint();

    zoffset.assertNDim(1);
    if (zoffset.shape(0) != getOutputNDim()) {
      throw new IllegalArgumentException(
        String.format(
          "projection.shape[1] != offset.shape[0]: %s != %s",
          this.projection.shapeAsList(),
          zoffset.shapeAsList()
        )
      );
    }
  }

  /**
   * Create a new ZAffineMap.
   *
   * @param projection the matrix.
   */
  public ZAffineMap(@Nonnull HasZTensor projection) {
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
  public int hashCode() {
    return Objects.hash(projection, offset);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ZAffineMap that)) return false;
    return projection.equals(that.projection) && offset.equals(that.offset);
  }

  @Override
  public String toString() {
    return ("λx." + projection.toJsonString() + "⋅x + " + offset.toJsonString());
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
  public ZTensor apply(@Nonnull HasZTensor x) {
    return projection.matmul(x).add(offset);
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
  public ZAffineMap translate(@Nonnull HasZTensor x) {
    return new ZAffineMap(projection, offset.add(x));
  }
}
