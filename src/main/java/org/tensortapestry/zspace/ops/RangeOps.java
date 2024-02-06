package org.tensortapestry.zspace.ops;

import javax.annotation.Nonnull;
import lombok.experimental.UtilityClass;
import org.tensortapestry.zspace.ZPoint;
import org.tensortapestry.zspace.ZRange;
import org.tensortapestry.zspace.ZTensorWrapper;

@UtilityClass
public class RangeOps {

  /**
   * Compute the cartesian product of two ranges.
   *
   * @param lhs the left-hand side.
   * @param rhs the right-hand side.
   * @return the cartesian product.
   */
  @Nonnull
  public static ZRange cartesianProduct(@Nonnull ZRange lhs, @Nonnull ZRange rhs) {
    int baseNDim = rhs.getNDim();
    int embedNDim = lhs.getNDim();
    ZPoint rStart = rhs.getStart().addDims(0, embedNDim).add(lhs.getStart().addDims(-1, baseNDim));
    ZPoint rEnd = rhs.getEnd().addDims(0, embedNDim).add(lhs.getEnd().addDims(-1, baseNDim));
    return new ZRange(rStart, rEnd);
  }

  /**
   * Compute the cartesian product of two ranges.
   *
   * @param lhs the left-hand side.
   * @param rhs the right-hand side.
   * @return the cartesian product.
   */
  @Nonnull
  public static ZRange cartesianProduct(@Nonnull ZRange lhs, @Nonnull ZTensorWrapper rhs) {
    return cartesianProduct(lhs, ZRange.newFromShape(rhs));
  }

  /**
   * Compute the cartesian product of two ranges.
   *
   * @param lhs the left-hand side.
   * @param rhs the right-hand side.
   * @return the cartesian product.
   */
  @Nonnull
  public static ZRange cartesianProduct(@Nonnull ZTensorWrapper lhs, @Nonnull ZTensorWrapper rhs) {
    return cartesianProduct(ZRange.newFromShape(lhs), ZRange.newFromShape(rhs));
  }
}
