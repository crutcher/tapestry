package org.tensortapestry.loom.zspace.ops;

import javax.annotation.Nonnull;
import lombok.experimental.UtilityClass;
import org.tensortapestry.loom.zspace.ZTensorWrapper;
import org.tensortapestry.loom.zspace.indexing.BufferMode;

/**
 * Coordinate Dominance Ordering.
 *
 * <p>Two tensors are comparable if they have the same dimension;
 * and all coordinates of the first are less than or equal to the second.
 */
@UtilityClass
public final class DominanceOrderingOps {

  /**
   * Compute the partial ordering of two tensors as coordinates in distance from 0.
   *
   * <p>Only tensors of the same dimension are comparable.
   *
   * <p>Two tensors are equal if they have the same coordinates.
   *
   * <p>A tensor is less than another if it has a coordinate less than the other; and no
   * coordinates
   * greater than the other.
   *
   * <p>A tensor is greater than another if it has a coordinate greater than the other; and no
   * coordinates less than the other.
   *
   * <p>Otherwise, the tensors are unordered.
   *
   * <p>This ordering is defined to be useful for {@code [start, end)} ranges.
   *
   * @param lhs the left-hand side.
   * @param rhs the right-hand side.
   * @return the partial ordering.
   */
  public static PartialOrdering partialOrderByGrid(
    @Nonnull ZTensorWrapper lhs,
    @Nonnull ZTensorWrapper rhs
  ) {
    var zlhs = lhs.unwrap();
    var zrhs = rhs.unwrap();

    zlhs.assertSameShape(zrhs);

    boolean lt = false;
    boolean gt = false;
    for (int[] coords : zlhs.byCoords(BufferMode.REUSED)) {
      int cmp = Integer.compare(zlhs.get(coords), zrhs.get(coords));
      if (cmp < 0) {
        lt = true;
      } else if (cmp > 0) {
        gt = true;
      }
    }
    if (lt && gt) return PartialOrdering.INCOMPARABLE;
    if (lt) return PartialOrdering.LESS_THAN;
    if (gt) return PartialOrdering.GREATER_THAN;
    return PartialOrdering.EQUAL;
  }

  /**
   * Are these points equal under partial ordering?
   *
   * @param lhs the left-hand side.
   * @param rhs the right-hand side.
   * @return true if the points are equal.
   */
  public static boolean eq(@Nonnull ZTensorWrapper lhs, @Nonnull ZTensorWrapper rhs) {
    lhs.unwrap().assertSameShape(rhs);
    return lhs.unwrap().equals(rhs);
  }

  /**
   * Are these points non-equal under partial ordering?
   *
   * @param lhs the left-hand side.
   * @param rhs the right-hand side.
   * @return true if the points are non-equal.
   */
  public static boolean ne(@Nonnull ZTensorWrapper lhs, @Nonnull ZTensorWrapper rhs) {
    return !eq(lhs, rhs);
  }

  /**
   * Is {@code lhs < rhs}?
   *
   * @param lhs the left-hand side.
   * @param rhs the right-hand side.
   * @return true or false.
   */
  public static boolean lt(@Nonnull ZTensorWrapper lhs, @Nonnull ZTensorWrapper rhs) {
    return partialOrderByGrid(lhs, rhs) == PartialOrdering.LESS_THAN;
  }

  /**
   * Is {@code lhs <= rhs}?
   *
   * @param lhs the left-hand side.
   * @param rhs the right-hand side.
   * @return true or false.
   */
  public static boolean le(@Nonnull ZTensorWrapper lhs, @Nonnull ZTensorWrapper rhs) {
    return switch (partialOrderByGrid(lhs, rhs)) {
      case LESS_THAN, EQUAL -> true;
      default -> false;
    };
  }

  /**
   * Is {@code lhs > rhs}?
   *
   * @param lhs the left-hand side.
   * @param rhs the right-hand side.
   * @return true or false.
   */
  public static boolean gt(@Nonnull ZTensorWrapper lhs, @Nonnull ZTensorWrapper rhs) {
    return partialOrderByGrid(lhs, rhs) == PartialOrdering.GREATER_THAN;
  }

  /**
   * Is {@code lhs >= rhs}?
   *
   * @param lhs the left-hand side.
   * @param rhs the right-hand side.
   * @return true or false.
   */
  public static boolean ge(@Nonnull ZTensorWrapper lhs, @Nonnull ZTensorWrapper rhs) {
    return switch (partialOrderByGrid(lhs, rhs)) {
      case GREATER_THAN, EQUAL -> true;
      default -> false;
    };
  }

  /**
   * Partial Ordering Typing.
   */
  public enum PartialOrdering {
    /**
     * The left-hand side is less than the right-hand side.
     */
    LESS_THAN,
    /**
     * The left-hand side is equal to the right-hand side.
     */
    EQUAL,
    /**
     * The left-hand side is greater than the right-hand side.
     */
    GREATER_THAN,
    /**
     * The left-hand side is incomparable to the right-hand side.
     */
    INCOMPARABLE,
  }
}
