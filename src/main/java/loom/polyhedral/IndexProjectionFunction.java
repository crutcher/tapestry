package loom.polyhedral;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import lombok.Builder;
import lombok.Value;
import loom.common.json.HasToJsonString;
import loom.zspace.HasZTensor;
import loom.zspace.ZAffineMap;
import loom.zspace.ZPoint;
import loom.zspace.ZRange;

/** A function which maps coordinates in a space to ranges in another space. */
@ThreadSafe
@Immutable
@Value
public class IndexProjectionFunction implements HasToJsonString {

  @SuppressWarnings("unused")
  public static class IndexProjectionFunctionBuilder {

    /**
     * Build an ZAffineMap from a matrix.
     *
     * @param matrix the matrix.
     * @return {@code this}
     */
    @Nonnull
    @JsonIgnore
    public IndexProjectionFunctionBuilder affineMap(@Nonnull int[][] matrix) {
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
    public IndexProjectionFunctionBuilder affineMap(@Nonnull ZAffineMap affineMap) {
      this.affineMap = affineMap;
      return this;
    }

    /**
     * Build an ZAffineMap from a builder.
     *
     * @param builder the builder.
     * @return {@code this}
     */
    @Nonnull
    @JsonIgnore
    public IndexProjectionFunctionBuilder affineMap(@Nonnull ZAffineMap.ZAffineMapBuilder builder) {
      return affineMap(builder.build());
    }

    /**
     * Translate the ZAffineMap by the given offset.
     *
     * @param offset the offset.
     * @return {@code this}
     */
    @Nonnull
    @JsonIgnore
    public IndexProjectionFunctionBuilder translate(@Nonnull HasZTensor offset) {
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
    public IndexProjectionFunctionBuilder translate(int... offset) {
      return affineMap(affineMap.translate(offset));
    }

    /**
     * Set the shape of the output.
     *
     * @param shape the shape.
     * @return {@code this}
     */
    @Nonnull
    @JsonSetter
    public IndexProjectionFunctionBuilder shape(@Nonnull HasZTensor shape) {
      this.shape = shape.getTensor().newZPoint();
      return this;
    }

    /**
     * Set the shape of the output.
     *
     * @param shape the shape.
     * @return {@code this}
     */
    @Nonnull
    @JsonIgnore
    public IndexProjectionFunctionBuilder shape(int... shape) {
      return shape(ZPoint.of(shape));
    }
  }

  @JsonCreator
  @Builder
  static IndexProjectionFunction privateBuilder(
    @Nonnull @JsonProperty(value = "affineMap") ZAffineMap affineMap,
    @Nonnull @JsonProperty(value = "shape") ZPoint shape
  ) {
    return new IndexProjectionFunction(affineMap, shape);
  }

  @Nonnull
  ZAffineMap affineMap;

  @Nonnull
  ZPoint shape;

  /**
   * Create a new IndexProjectionFunction.
   *
   * @param affineMap the affine map.
   * @param shape the shape, or {@code null} to use one's in the affine map's output dims.
   */
  public IndexProjectionFunction(@Nonnull ZAffineMap affineMap, @Nullable HasZTensor shape) {
    this.affineMap = affineMap;
    this.shape =
      shape == null ? ZPoint.newOnes(affineMap.outputNDim()) : shape.getTensor().newZPoint();

    if (this.affineMap.outputNDim() != this.shape.getNDim()) {
      throw new IllegalArgumentException(
        String.format(
          "affineMap.outputDim() (%d) != shape.dim() (%d)",
          this.affineMap.outputNDim(),
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
  public ZRange apply(@Nonnull HasZTensor source) {
    HasZTensor start = affineMap.apply(source).newZPoint();
    return ZRange.builder().start(start).shape(shape).build();
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
      HasZTensor shape1 = ZPoint.newZeros(r1.getNDim());
      return ZRange.builder().start(r1.getStart()).shape(shape1).build();
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
  public IndexProjectionFunction translate(@Nonnull HasZTensor offset) {
    return IndexProjectionFunction
      .builder()
      .affineMap(affineMap.translate(offset))
      .shape(shape)
      .build();
  }
}
