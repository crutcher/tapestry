package org.tensortapestry.zspace;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.tensortapestry.common.json.HasToJsonString;
import org.tensortapestry.zspace.indexing.Selector;

/**
 * A function which maps coordinates in a space to ranges in another space.
 */
@Value
@Immutable
@ThreadSafe
@EqualsAndHashCode(cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY)
public class ZRangeProjectionMap implements HasToJsonString {

  @SuppressWarnings("unused")
  public static final class ZRangeProjectionMapBuilder {

    /**
     * Construct the affine map from an identity matrix.
     *
     * @param n the number of dimensions.
     * @return {@code this}
     */
    @Nonnull
    @JsonIgnore
    @CanIgnoreReturnValue
    public ZRangeProjectionMapBuilder identityMap(int n) {
      return affineMap(ZAffineMap.newIdentityMap(n));
    }

    /**
     * Construct the affine map from a diagonal matrix.
     *
     * @param diagonal the diagonal values.
     * @return {@code this}
     */
    @Nonnull
    @JsonIgnore
    @CanIgnoreReturnValue
    public ZRangeProjectionMapBuilder fromDiagonal(@Nonnull int... diagonal) {
      return affineMap(ZAffineMap.newFromDiagonal(diagonal));
    }

    /**
     * Construct the affine map from a diagonal matrix.
     *
     * @param diagonal the diagonal values.
     * @return {@code this}
     */
    @Nonnull
    @JsonIgnore
    @CanIgnoreReturnValue
    public ZRangeProjectionMapBuilder fromDiagonal(@Nonnull List<Integer> diagonal) {
      return affineMap(ZAffineMap.newFromDiagonal(diagonal));
    }

    /**
     * Construct the affine map from a diagonal matrix.
     *
     * @param diagonal the diagonal values.
     * @return {@code this}
     */
    @Nonnull
    @JsonIgnore
    @CanIgnoreReturnValue
    public ZRangeProjectionMapBuilder fromDiagonal(@Nonnull ZTensorWrapper diagonal) {
      return affineMap(ZAffineMap.newFromDiagonal(diagonal));
    }

    /**
     * Build an ZAffineMap from a matrix.
     *
     * @param matrix the matrix.
     * @return {@code this}
     */
    @Nonnull
    @JsonIgnore
    @CanIgnoreReturnValue
    public ZRangeProjectionMapBuilder affineMap(@Nonnull int[][] matrix) {
      return affineMap(ZAffineMap.fromMatrix(matrix));
    }

    /**
     * Build an ZAffineMap from a matrix.
     *
     * @param matrix the matrix.
     * @return {@code this}
     */
    @Nonnull
    @JsonIgnore
    @CanIgnoreReturnValue
    public ZRangeProjectionMapBuilder affineMap(@Nonnull ZTensorWrapper matrix) {
      return affineMap(ZAffineMap.fromMatrix(matrix));
    }

    /**
     * Set a ZAffineMap on the builder.
     *
     * @param affineMap the ZAffineMap.
     * @return {@code this}
     */
    @Nonnull
    @JsonSetter
    @CanIgnoreReturnValue
    public ZRangeProjectionMapBuilder affineMap(@Nonnull ZAffineMap affineMap) {
      this.affineMap = affineMap;
      return this;
    }

    /**
     * Translate the ZAffineMap by the given offset.
     *
     * @param offset the offset.
     * @return {@code this}
     */
    @Nonnull
    @JsonIgnore
    @CanIgnoreReturnValue
    public ZRangeProjectionMapBuilder translate(@Nonnull int... offset) {
      return affineMap(affineMap.translate(offset));
    }

    /**
     * Translate the ZAffineMap by the given offset.
     *
     * @param offset the offset.
     * @return {@code this}
     */
    @Nonnull
    @JsonIgnore
    @CanIgnoreReturnValue
    public ZRangeProjectionMapBuilder translate(@Nonnull ZTensorWrapper offset) {
      return affineMap(affineMap.translate(offset));
    }

    /**
     * Set the shape of the output.
     *
     * @param shape the shape.
     * @return {@code this}
     */
    @Nonnull
    @JsonIgnore
    @CanIgnoreReturnValue
    public ZRangeProjectionMapBuilder shape(@Nonnull int... shape) {
      return shape(ZPoint.of(shape));
    }

    /**
     * Set the shape of the output.
     *
     * @param shape the shape.
     * @return {@code this}
     */
    @Nonnull
    @JsonSetter
    @CanIgnoreReturnValue
    public ZRangeProjectionMapBuilder shape(@Nonnull ZTensorWrapper shape) {
      this.shape = ZPoint.of(shape);
      return this;
    }
  }

  /**
   * Create a new ZRangeProjectionMap.
   *
   * <p>This is a private builder to force the type of the {@link #affineMap} and {@link #shape}
   * used in the builder to be {@link ZAffineMap} and {@link ZPoint}, respectively; and to prevent
   * collision with {@link ZTensorWrapper}.
   *
   * @param affineMap the affine map.
   * @param shape the shape.
   * @return the new ZRangeProjectionMap.
   */
  @JsonCreator
  @Builder(toBuilder = true)
  static ZRangeProjectionMap privateBuilder(
    @Nonnull @JsonProperty(value = "affineMap") ZAffineMap affineMap,
    @Nonnull @JsonProperty(value = "shape") ZPoint shape
  ) {
    return new ZRangeProjectionMap(affineMap, shape);
  }

  @Nonnull
  ZAffineMap affineMap;

  @Nonnull
  ZPoint shape;

  /**
   * Create a new ZRangeProjectionMap.
   *
   * @param affineMap the affine map.
   * @param shape the shape, or {@code null} to use one's in the affine map's output dims.
   */
  public ZRangeProjectionMap(@Nonnull ZAffineMap affineMap, @Nullable ZTensorWrapper shape) {
    this.affineMap = affineMap;
    this.shape = shape == null ? ZPoint.newOnes(affineMap.getOutputNDim()) : ZPoint.of(shape);

    if (this.affineMap.getOutputNDim() != this.shape.getNDim()) {
      throw new IllegalArgumentException(
        String.format(
          "affineMap.outputDim() (%d) != shape.dim() (%d)",
          this.affineMap.getOutputNDim(),
          this.shape.getNDim()
        )
      );
    }
  }

  @Override
  public String toString() {
    return String.format("ipf(affineMap=%s, shape=%s)", affineMap, shape);
  }

  /**
   * Applies the projection function to the given point.
   *
   * @param source The point to project.
   * @return The projected range.
   */
  @Nonnull
  public ZRange apply(@Nonnull ZTensorWrapper source) {
    return ZRange.builder().start(ZPoint.of(affineMap.apply(source))).shape(shape).build();
  }

  @Nonnull
  public ZRange broadcastApply(@Nonnull ZTensorWrapper source) {
    var t = source.unwrap();
    t.assertNDim(1);
    var prefix = t.select(Selector.slice(0, -affineMap.getInputNDim()));
    var ones = ZTensor.newOnes(prefix.getSize());
    var b = affineMap.broadcastApply(source);

    return ZRange.builder().start(b).shape(ZPoint.of(ZTensor.concat(0, ones, shape))).build();
  }

  /**
   * Applies the projection function to the given range.
   *
   * @param source The range to project.
   * @return the union of the projected ranges.
   */
  @Nonnull
  public ZRange apply(@Nonnull ZRange source) {
    // TODO: Does the linear nature of the affine map mean that this is sufficient?
    ZRange r1 = apply(source.getStart());
    if (source.isEmpty()) {
      return ZRange.builder().start(r1.getStart()).shape(ZPoint.newZeros(r1.getNDim())).build();
    }
    return ZRange.boundingRange(r1, apply(source.getInclusiveEnd()));
  }

  /**
   * Translates the projection function by the given offset.
   *
   * @param offset The offset to translate by.
   * @return The translated projection function.
   */
  @Nonnull
  public ZRangeProjectionMap translate(@Nonnull ZTensorWrapper offset) {
    return ZRangeProjectionMap
      .builder()
      .affineMap(affineMap.translate(offset))
      .shape(shape)
      .build();
  }
}
