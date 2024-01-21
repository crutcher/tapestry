package loom.zspace;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import loom.common.json.HasToJsonString;

/** A linear map from {@code Z^inDim} to {@code Z^outDim}. */
@Value
@ThreadSafe
@Immutable
@Jacksonized
@Builder(toBuilder = true)
@JsonPropertyOrder({ "projection", "offset" })
public class ZAffineMap
  implements HasPermuteInput<ZAffineMap>, HasPermuteOutput<ZAffineMap>, HasToJsonString {

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
   * @param offset the bias; if null, will be a zero vector of size `A.shape[0]`.
   */
  @JsonCreator
  public ZAffineMap(
    @JsonProperty(value = "projection", required = true) HasZTensor projection,
    @Nullable @JsonProperty(value = "offset") HasZTensor offset
  ) {
    this.projection = new ZMatrix(projection);
    if (offset == null) {
      offset = ZTensor.newZeros(this.projection.outputNDim());
    }
    var zoffset = offset.getTensor();
    this.offset = zoffset.newZPoint();

    zoffset.assertNDim(1);
    if (zoffset.shape(0) != outputNDim()) {
      throw new IllegalArgumentException(
        String.format(
          "A.shape[1] != b.shape[0]: %s != %s",
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

  /**
   * Get the input dimension of this affine map.
   *
   * @return the input dimension.
   */
  public int inputNDim() {
    return projection.inputNDim();
  }

  /**
   * Get the output dimension of this affine map.
   *
   * @return the output dimension.
   */
  public int outputNDim() {
    return projection.outputNDim();
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
  public ZAffineMap permuteInput(@Nonnull int... permutation) {
    return new ZAffineMap(projection.permuteInput(permutation), offset);
  }

  @Override
  public ZAffineMap permuteOutput(@Nonnull int... permutation) {
    return new ZAffineMap(
      projection.permuteOutput(permutation),
      offset.tensor.reorderedDimCopy(permutation, 0)
    );
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
   * @param x a 1-dim tensor of length `inDim`.
   * @return a new affine map.
   */
  @Nonnull
  public ZAffineMap translate(@Nonnull HasZTensor x) {
    return new ZAffineMap(projection, offset.add(x));
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
