package org.tensortapestry.loom.zspace;

import javax.annotation.Nonnull;
import lombok.experimental.UtilityClass;

/** Namespace of ZPoint operations. */
@UtilityClass
public final class DominanceOrderingOperations {

  /**
   * Compute the partial ordering of two tensors as coordinates in distance from 0.
   *
   * <p>Only tensors of the same dimension are comparable.
   *
   * <p>Two tensors are equal if they have the same coordinates.
   *
   * <p>A tensor is less than another if it has a coordinate less than the other; and no coordinates
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
    @Nonnull HasZTensor lhs,
    @Nonnull HasZTensor rhs
  ) {
    var zlhs = lhs.getTensor();
    var zrhs = rhs.getTensor();

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
  public static boolean eq(@Nonnull HasZTensor lhs, @Nonnull HasZTensor rhs) {
    lhs.getTensor().assertSameShape(rhs);
    return lhs.getTensor().equals(rhs);
  }

  /**
   * Are these points non-equal under partial ordering?
   *
   * @param lhs the left-hand side.
   * @param rhs the right-hand side.
   * @return true if the points are non-equal.
   */
  public static boolean ne(@Nonnull HasZTensor lhs, @Nonnull HasZTensor rhs) {
    return !eq(lhs, rhs);
  }

  /**
   * Is {@code lhs < rhs}?
   *
   * @param lhs the left-hand side.
   * @param rhs the right-hand side.
   * @return true or false.
   */
  public static boolean lt(@Nonnull HasZTensor lhs, @Nonnull HasZTensor rhs) {
    return partialOrderByGrid(lhs, rhs) == PartialOrdering.LESS_THAN;
  }

  /**
   * Is {@code lhs <= rhs}?
   *
   * @param lhs the left-hand side.
   * @param rhs the right-hand side.
   * @return true or false.
   */
  public static boolean le(@Nonnull HasZTensor lhs, @Nonnull HasZTensor rhs) {
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
  public static boolean gt(@Nonnull HasZTensor lhs, @Nonnull HasZTensor rhs) {
    return partialOrderByGrid(lhs, rhs) == PartialOrdering.GREATER_THAN;
  }

  /**
   * Is {@code lhs >= rhs}?
   *
   * @param lhs the left-hand side.
   * @param rhs the right-hand side.
   * @return true or false.
   */
  public static boolean ge(@Nonnull HasZTensor lhs, @Nonnull HasZTensor rhs) {
    return switch (partialOrderByGrid(lhs, rhs)) {
      case GREATER_THAN, EQUAL -> true;
      default -> false;
    };
  }
}
