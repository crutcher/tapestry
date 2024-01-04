package loom.polyhedral;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import loom.common.json.HasToJsonString;
import loom.zspace.ZAffineMap;
import loom.zspace.ZPoint;
import loom.zspace.ZRange;
import loom.zspace.ZTensor;

/** A function which maps coordinates in a space to ranges in another space. */
@Value
@Jacksonized
@Builder
public class IndexProjectionFunction implements HasToJsonString {
  @SuppressWarnings("unused")
  public static class IndexProjectionFunctionBuilder {
    public IndexProjectionFunctionBuilder affineMap(@Nonnull ZAffineMap affineMap) {
      this.affineMap = affineMap;
      return this;
    }

    public IndexProjectionFunctionBuilder affineMap(@Nonnull ZAffineMap.ZAffineMapBuilder builder) {
      return affineMap(builder.build());
    }

    public IndexProjectionFunctionBuilder shape(@Nonnull ZPoint shape) {
      this.shape = shape;
      return this;
    }

    public IndexProjectionFunctionBuilder shape(@Nonnull ZTensor shape) {
      return shape(shape.newZPoint());
    }

    public IndexProjectionFunctionBuilder shape(int... shape) {
      return shape(ZPoint.of(shape));
    }
  }

  @Nonnull ZAffineMap affineMap;
  @Nonnull ZPoint shape;

  @Builder
  public IndexProjectionFunction(@Nonnull ZAffineMap affineMap, @Nullable ZPoint shape) {
    this.shape = shape == null ? ZPoint.newOnes(affineMap.outputNDim()) : shape;
    this.affineMap = affineMap;

    if (this.affineMap.outputNDim() != this.shape.getNDim()) {
      throw new IllegalArgumentException(
          String.format(
              "affineMap.outputDim() (%d) != shape.dim() (%d)",
              this.affineMap.outputNDim(), this.shape.getNDim()));
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
  public ZRange apply(@Nonnull ZPoint source) {
    return apply(source.coords);
  }

  /**
   * Applies the projection function to the given point.
   *
   * @param source The point to project.
   * @return The projected range.
   */
  @Nonnull
  public ZRange apply(@Nonnull ZTensor source) {
    return ZRange.fromStartWithShape(affineMap.apply(source).newZPoint(), shape);
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
      return ZRange.fromStartWithShape(r1.getStart(), ZPoint.newZeros(r1.getNDim()));
    }
    return ZRange.boundingRange(r1, apply(source.getInclusiveEnd()));
  }
}
