package org.tensortapestry.zspace.ops;

import java.util.Arrays;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.experimental.UtilityClass;
import org.tensortapestry.zspace.*;

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

  /**
   * Return the intersection of two ranges with another.
   *
   * @param lhs the left-hand range.
   * @param rhs the right-hand range.
   * @return the intersection of this range with another, null if there is no intersection.
   */
  @Nullable public static ZRange intersection(@Nonnull ZRange lhs, @Nonnull ZRange rhs) {
    ZTensor zTensor1 = CellWise.maximum(lhs.getStart(), rhs.getStart());
    var iStart = ZPoint.of(zTensor1);
    ZTensor zTensor = CellWise.minimum(lhs.getEnd(), rhs.getEnd());
    var iEnd = ZPoint.of(zTensor);
    if (iStart.le(iEnd)) {
      return new ZRange(iStart, iEnd);
    } else {
      return null;
    }
  }

  /**
   * Compute the minimum bounding range of a set of ranges.
   *
   * @param ranges the ranges to bound.
   * @return the minimum bounding range.
   */
  @Nonnull
  public static ZRange boundingRange(@Nonnull ZRange... ranges) {
    return boundingRange(Arrays.asList(ranges));
  }

  /**
   * Compute the minimum bounding range of a set of ranges.
   *
   * @param ranges the ranges to bound.
   * @return the minimum bounding range.
   */
  @Nonnull
  public static ZRange boundingRange(@Nonnull Iterable<ZRange> ranges) {
    var it = ranges.iterator();
    if (!it.hasNext()) {
      throw new IllegalArgumentException("no ranges");
    }

    var first = it.next();
    var start = first.getStart().unwrap();
    var end = first.getEnd().unwrap();

    while (it.hasNext()) {
      var r = it.next();
      HasDimension.assertSameNDim(first, r);

      start = CellWise.minimum(start, r.getStart());
      end = CellWise.maximum(end, r.getEnd());
    }

    return new ZRange(start, end);
  }
}
