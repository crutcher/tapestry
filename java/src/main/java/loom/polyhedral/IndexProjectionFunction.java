package loom.polyhedral;

import javax.annotation.Nonnull;
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
  @Nonnull ZAffineMap affineMap;
  @Nonnull ZPoint shape;

  @Builder
  public IndexProjectionFunction(ZAffineMap affineMap, ZPoint shape) {
    this.affineMap = affineMap;
    this.shape = shape;

    if (affineMap.outputNDim() != shape.getNDim()) {
      throw new IllegalArgumentException(
          String.format(
              "affineMap.outputDim() (%d) != shape.dim() (%d)",
              affineMap.outputNDim(), shape.getNDim()));
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
