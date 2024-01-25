package org.tensortapestry.loom.zspace;

import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;
import javax.annotation.Nonnull;
import lombok.experimental.UtilityClass;
import org.tensortapestry.loom.zspace.indexing.BufferMode;
import org.tensortapestry.loom.zspace.indexing.IndexingFns;

/**
 * Various operations on ZTensors.
 */
@UtilityClass
public class Ops {

  /**
   * Coordinate Dominance Ordering.
   *
   * <p>Two tensors are comparable if they have the same dimension;
   * and all coordinates of the first are less than or equal to the second.
   */
  @UtilityClass
  public static final class DominanceOrdering {

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

  /** ZTensor cell wise operations. */
  @UtilityClass
  public static class CellWise {

    /**
     * Elementwise negation of a tensor.
     *
     * @param tensor the input tensor.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor neg(@Nonnull HasZTensor tensor) {
      return map(x -> -x, tensor);
    }

    /**
     * An element-wise unary operation.
     *
     * @param op the operation.
     * @param tensor the input tensor.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor map(@Nonnull IntUnaryOperator op, @Nonnull HasZTensor tensor) {
      var result = ZTensor.newZerosLike(tensor);
      result.assignFromMap_(op, tensor);
      return result;
    }

    /**
     * Elementwise absolute value of a tensor.
     *
     * @param tensor the input tensor.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor abs(@Nonnull HasZTensor tensor) {
      return map(Math::abs, tensor);
    }

    /**
     * Element-wise broadcast minimum.
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor minimum(@Nonnull HasZTensor lhs, @Nonnull HasZTensor rhs) {
      return zipWith(Math::min, lhs, rhs);
    }

    /**
     * An element-wise broadcast binary operation.
     *
     * @param op the operation.
     * @param lhs the left-hand side tensor.
     * @param rhs the right-hand side tensor.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor zipWith(
      @Nonnull IntBinaryOperator op,
      @Nonnull HasZTensor lhs,
      @Nonnull HasZTensor rhs
    ) {
      var zlhs = lhs.getTensor();
      var zrhs = rhs.getTensor();
      var result = ZTensor.newZeros(
        IndexingFns.commonBroadcastShape(zlhs._unsafeGetShape(), zrhs._unsafeGetShape())
      );
      result.assignFromZipWith_(op, zlhs, zrhs);
      return result;
    }

    /**
     * Element-wise broadcast minimum.
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor minimum(@Nonnull HasZTensor lhs, int rhs) {
      return zipWith(Math::min, lhs, rhs);
    }

    /**
     * An element-wise broadcast binary operation.
     *
     * @param op the operation.
     * @param lhs the left-hand side tensor.
     * @param rhs the right-hand side scalar.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor zipWith(@Nonnull IntBinaryOperator op, @Nonnull HasZTensor lhs, int rhs) {
      return zipWith(op, lhs, ZTensor.newScalar(rhs));
    }

    /**
     * Element-wise broadcast minimum.
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor minimum(int lhs, @Nonnull HasZTensor rhs) {
      return zipWith(Math::min, lhs, rhs);
    }

    /**
     * An element-wise broadcast binary operation.
     *
     * @param op the operation.
     * @param lhs the left-hand side scalar.
     * @param rhs the right-hand side tensor.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor zipWith(@Nonnull IntBinaryOperator op, int lhs, @Nonnull HasZTensor rhs) {
      return zipWith(op, ZTensor.newScalar(lhs), rhs);
    }

    /**
     * Element-wise broadcast maximum.
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor maximum(@Nonnull HasZTensor lhs, @Nonnull HasZTensor rhs) {
      return zipWith(Math::max, lhs, rhs);
    }

    /**
     * Element-wise broadcast maximum.
     *
     * @param lhs the left-hand side tensor.
     * @param rhs the right-hand side scalar.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor maximum(@Nonnull HasZTensor lhs, int rhs) {
      return zipWith(Math::max, lhs, rhs);
    }

    /**
     * Element-wise broadcast maximum.
     *
     * @param lhs the left-hand side tensor.
     * @param rhs the right-hand side scalar.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor maximum(int lhs, @Nonnull HasZTensor rhs) {
      return zipWith(Math::max, lhs, rhs);
    }

    /**
     * Element-wise broadcast addition.
     *
     * @param lhs the left-hand side tensor.
     * @param rhs the right-hand side scalar.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor add(@Nonnull HasZTensor lhs, @Nonnull HasZTensor rhs) {
      return zipWith(Integer::sum, lhs, rhs);
    }

    /**
     * Element-wise broadcast addition.
     *
     * @param lhs the left-hand side tensor.
     * @param rhs the right-hand side scalar.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor add(@Nonnull HasZTensor lhs, int rhs) {
      return zipWith(Integer::sum, lhs, rhs);
    }

    /**
     * Element-wise broadcast addition.
     *
     * @param lhs the left-hand side scalar.
     * @param rhs the right-hand side tensor.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor add(int lhs, @Nonnull HasZTensor rhs) {
      return zipWith(Integer::sum, lhs, rhs);
    }

    /**
     * Element-wise broadcast in-place addition on the lhs.
     *
     * @param lhs the left-hand side tensor, modified in-place; must be mutable.
     * @param rhs the right-hand side tensor.
     */
    public static void add_(@Nonnull ZTensor lhs, @Nonnull HasZTensor rhs) {
      lhs.zipWith_(Integer::sum, rhs);
    }

    /**
     * Element-wise broadcast in-place addition on the lhs.
     *
     * @param lhs the left-hand side tensor, modified in-place; must be mutable.
     * @param rhs the right-hand side tensor.
     */
    public static void add_(@Nonnull ZTensor lhs, int rhs) {
      lhs.zipWith_(Integer::sum, rhs);
    }

    /**
     * Element-wise broadcast subtraction.
     *
     * @param lhs the left-hand side scalar.
     * @param rhs the right-hand side tensor.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor sub(@Nonnull HasZTensor lhs, @Nonnull HasZTensor rhs) {
      return zipWith((l, r) -> l - r, lhs, rhs);
    }

    /**
     * Element-wise broadcast subtraction.
     *
     * @param lhs the left-hand side scalar.
     * @param rhs the right-hand side tensor.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor sub(@Nonnull HasZTensor lhs, int rhs) {
      return zipWith((l, r) -> l - r, lhs, rhs);
    }

    /**
     * Element-wise broadcast subtraction.
     *
     * @param lhs the left-hand side scalar.
     * @param rhs the right-hand side tensor.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor sub(int lhs, @Nonnull HasZTensor rhs) {
      return zipWith((l, r) -> l - r, lhs, rhs);
    }

    /**
     * Element-wise broadcast in-place subtraction on the lhs.
     *
     * @param lhs the left-hand side tensor, modified in-place; must be mutable.
     * @param rhs the right-hand side tensor.
     */
    public static void sub_(@Nonnull ZTensor lhs, @Nonnull HasZTensor rhs) {
      lhs.zipWith_((l, r) -> l - r, rhs);
    }

    /**
     * Element-wise broadcast in-place subtraction on the lhs.
     *
     * @param lhs the left-hand side tensor, modified in-place; must be mutable.
     * @param rhs the right-hand side tensor.
     */
    public static void sub_(@Nonnull ZTensor lhs, int rhs) {
      lhs.zipWith_((l, r) -> l - r, rhs);
    }

    /**
     * Element-wise broadcast multiplication.
     *
     * @param lhs the left-hand side scalar.
     * @param rhs the right-hand side tensor.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor mul(@Nonnull HasZTensor lhs, @Nonnull HasZTensor rhs) {
      return zipWith((l, r) -> l * r, lhs, rhs);
    }

    /**
     * Element-wise broadcast multiplication.
     *
     * @param lhs the left-hand side scalar.
     * @param rhs the right-hand side tensor.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor mul(@Nonnull HasZTensor lhs, int rhs) {
      return zipWith((l, r) -> l * r, lhs, rhs);
    }

    /**
     * Element-wise broadcast multiplication.
     *
     * @param lhs the left-hand side scalar.
     * @param rhs the right-hand side tensor.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor mul(int lhs, @Nonnull HasZTensor rhs) {
      return zipWith((l, r) -> l * r, lhs, rhs);
    }

    /**
     * Element-wise broadcast in-place multiplication on the lhs.
     *
     * @param lhs the left-hand side tensor, modified in-place; must be mutable.
     * @param rhs the right-hand side tensor.
     */
    public static void mul_(@Nonnull ZTensor lhs, @Nonnull HasZTensor rhs) {
      lhs.zipWith_((l, r) -> l * r, rhs);
    }

    /**
     * Element-wise broadcast in-place multiplication on the lhs.
     *
     * @param lhs the left-hand side tensor, modified in-place; must be mutable.
     * @param rhs the right-hand side tensor.
     */
    public static void mul_(@Nonnull ZTensor lhs, int rhs) {
      lhs.zipWith_((l, r) -> l * r, rhs);
    }

    /**
     * Element-wise broadcast division.
     *
     * @param lhs the left-hand side scalar.
     * @param rhs the right-hand side tensor.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor div(@Nonnull HasZTensor lhs, @Nonnull HasZTensor rhs) {
      return zipWith((l, r) -> l / r, lhs, rhs);
    }

    /**
     * Element-wise broadcast division.
     *
     * @param lhs the left-hand side scalar.
     * @param rhs the right-hand side tensor.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor div(@Nonnull HasZTensor lhs, int rhs) {
      return zipWith((l, r) -> l / r, lhs, rhs);
    }

    /**
     * Element-wise broadcast division.
     *
     * @param lhs the left-hand side scalar.
     * @param rhs the right-hand side tensor.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor div(int lhs, @Nonnull HasZTensor rhs) {
      return zipWith((l, r) -> l / r, lhs, rhs);
    }

    /**
     * Element-wise broadcast in-place division on the lhs.
     *
     * @param lhs the left-hand side tensor, modified in-place; must be mutable.
     * @param rhs the right-hand side tensor.
     */
    public static void div_(@Nonnull ZTensor lhs, @Nonnull HasZTensor rhs) {
      lhs.zipWith_((l, r) -> l / r, rhs);
    }

    /**
     * Element-wise broadcast in-place division on the lhs.
     *
     * @param lhs the left-hand side tensor, modified in-place; must be mutable.
     * @param rhs the right-hand side tensor.
     */
    public static void div_(@Nonnull ZTensor lhs, int rhs) {
      lhs.zipWith_((l, r) -> l / r, rhs);
    }

    /**
     * Element-wise broadcast mod.
     *
     * @param lhs the left-hand side scalar.
     * @param base the right-hand side tensor.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor mod(@Nonnull HasZTensor lhs, @Nonnull HasZTensor base) {
      return zipWith((l, r) -> l % r, lhs, base);
    }

    /**
     * Element-wise broadcast mod.
     *
     * @param lhs the left-hand side scalar.
     * @param base the right-hand side tensor.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor mod(@Nonnull HasZTensor lhs, int base) {
      return zipWith((l, r) -> l % r, lhs, base);
    }

    /**
     * Element-wise broadcast mod.
     *
     * @param lhs the left-hand side scalar.
     * @param base the right-hand side tensor.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor mod(int lhs, @Nonnull HasZTensor base) {
      return zipWith((l, r) -> l % r, lhs, base);
    }

    /**
     * Element-wise broadcast in-place mod on the lhs.
     *
     * @param lhs the left-hand side tensor, modified in-place; must be mutable.
     * @param base the right-hand side tensor.
     */
    public static void mod_(@Nonnull ZTensor lhs, @Nonnull HasZTensor base) {
      lhs.zipWith_((l, r) -> l % r, base);
    }

    /**
     * Element-wise broadcast in-place mod on the lhs.
     *
     * @param lhs the left-hand side tensor, modified in-place; must be mutable.
     * @param base the right-hand side tensor.
     */
    public static void mod_(@Nonnull ZTensor lhs, int base) {
      lhs.zipWith_((l, r) -> l % r, base);
    }

    /**
     * Compute the integer power of a number.
     *
     * @param base the base.
     * @param exp the exponent.
     * @return the integer power.
     */
    public int intPow(int base, int exp) {
      if (exp < 0) {
        throw new IllegalArgumentException("exponent must be non-negative");
      }
      int result = 1;
      while (exp > 0) {
        if (exp % 2 == 1) {
          result *= base;
        }
        base *= base;
        exp /= 2;
      }
      return result;
    }

    /**
     * Element-wise broadcast power.
     *
     * @param lhs the left-hand side scalar.
     * @param exp the right-hand side tensor.
     * @return a new tensor.
     */
    public static ZTensor pow(@Nonnull HasZTensor lhs, @Nonnull HasZTensor exp) {
      return zipWith(CellWise::intPow, lhs, exp);
    }

    /**
     * Element-wise broadcast power.
     *
     * @param lhs the left-hand side tensor.
     * @param exp the right-hand side scalar.
     * @return a new tensor.
     */
    public static ZTensor pow(int lhs, @Nonnull HasZTensor exp) {
      return zipWith(CellWise::intPow, lhs, exp);
    }

    /**
     * Element-wise broadcast power.
     *
     * @param lhs the left-hand side tensor.
     * @param exp the right-hand side scalar.
     * @return a new tensor.
     */
    public static ZTensor pow(@Nonnull HasZTensor lhs, int exp) {
      return zipWith(CellWise::intPow, lhs, exp);
    }

    /**
     * Element-wise broadcast in-place power on the lhs.
     *
     * @param lhs the left-hand side tensor, modified in-place; must be mutable.
     * @param exp the right-hand side tensor.
     */
    public static void pow_(@Nonnull ZTensor lhs, @Nonnull HasZTensor exp) {
      lhs.zipWith_(CellWise::intPow, exp);
    }

    /**
     * Element-wise broadcast in-place power on the lhs.
     *
     * @param lhs the left-hand side tensor, modified in-place; must be mutable.
     * @param exp the right-hand side tensor.
     */
    public static void pow_(@Nonnull ZTensor lhs, int exp) {
      lhs.zipWith_(CellWise::intPow, exp);
    }

    /**
     * Compute the integer logarithm of a number.
     *
     * @param value the value.
     * @param base the base.
     * @return the integer logarithm.
     */
    public int intLog(int value, int base) {
      if (base <= 1) {
        throw new IllegalArgumentException("Base must be greater than 1");
      }
      if (value <= 0) {
        throw new IllegalArgumentException("Value must be positive");
      }

      int low = 0;
      int high = value;
      while (low < high) {
        int mid = (low + high) / 2;
        int pow = intPow(base, mid);

        if (pow == value) {
          return mid;
        } else if (pow < value) {
          low = mid + 1;
        } else {
          high = mid;
        }
      }

      return low - 1;
    }

    /**
     * Element-wise broadcast log.
     *
     * @param lhs the left-hand side tensor.
     * @param base the right-hand side tensor.
     * @return a new tensor.
     */
    public static ZTensor log(@Nonnull HasZTensor lhs, @Nonnull HasZTensor base) {
      return zipWith(CellWise::intLog, lhs, base);
    }

    /**
     * Element-wise broadcast log.
     *
     * @param lhs the left-hand side scalar.
     * @param base the right-hand side tensor.
     * @return a new tensor.
     */
    public static ZTensor log(int lhs, @Nonnull HasZTensor base) {
      return zipWith(CellWise::intLog, lhs, base);
    }

    /**
     * Element-wise broadcast log.
     *
     * @param lhs the left-hand side tensor.
     * @param base the right-hand side scalar.
     * @return a new tensor.
     */
    public static ZTensor log(@Nonnull HasZTensor lhs, int base) {
      return zipWith(CellWise::intLog, lhs, base);
    }

    /**
     * Element-wise broadcast in-place log on the lhs.
     *
     * @param lhs the left-hand side tensor, modified in-place; must be mutable.
     * @param base the right-hand side tensor.
     */
    public static void log_(@Nonnull ZTensor lhs, @Nonnull HasZTensor base) {
      lhs.zipWith_(CellWise::intLog, base);
    }

    /**
     * Element-wise broadcast in-place log on the lhs.
     *
     * @param lhs the left-hand side tensor, modified in-place; must be mutable.
     * @param base the right-hand side tensor.
     */
    public static void log_(@Nonnull ZTensor lhs, int base) {
      lhs.zipWith_(CellWise::intLog, base);
    }
  }

  /** ZTensor reduce operations. */
  @UtilityClass
  public static class Reduce {

    /**
     * Applies the given reduction operation to all values in the given tensor.
     *
     * @param tensor the tensor
     * @param op the reduction operation
     * @param initial the initial value
     * @return the int result of the reduction.
     */
    public static int reduceCellsAtomic(
      @Nonnull HasZTensor tensor,
      @Nonnull IntBinaryOperator op,
      int initial
    ) {
      var acc = new IntConsumer() {
        int value = initial;

        @Override
        public void accept(int value) {
          this.value = op.applyAsInt(this.value, value);
        }
      };

      tensor.getTensor().forEachValue(acc);
      return acc.value;
    }

    /**
     * Applies the given reduction operation to all values in the given tensor.
     *
     * @param tensor the tensor
     * @param op the reduction operation
     * @param initial the initial value
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor reduceCells(
      @Nonnull HasZTensor tensor,
      @Nonnull IntBinaryOperator op,
      int initial
    ) {
      return ZTensor.newScalar(reduceCellsAtomic(tensor, op, initial));
    }

    /**
     * Applies the given reduction operation to all values in the given tensor; grouping by the
     * specified dimensions.
     *
     * <p>The shape of the returned tensor is the same as the shape of the input tensor, except that
     * the specified dimensions are removed.
     *
     * @param tensor the tensor
     * @param op the reduction operation
     * @param initial the initial value
     * @param dims the dimensions to group by.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor reduceCells(
      @Nonnull HasZTensor tensor,
      @Nonnull IntBinaryOperator op,
      int initial,
      @Nonnull int... dims
    ) {
      var ztensor = tensor.getTensor();
      int[] ztensorShape = ztensor._unsafeGetShape();

      var sumDims = ztensor.resolveDims(dims);

      var sliceDims = new int[ztensor.getNDim() - sumDims.length];

      var accShape = new int[ztensor.getNDim() - sumDims.length];
      for (int sourceIdx = 0, accIdx = 0; sourceIdx < ztensor.getNDim(); ++sourceIdx) {
        if (IndexingFns.arrayContains(sumDims, sourceIdx)) {
          continue;
        }

        sliceDims[accIdx] = sourceIdx;
        accShape[accIdx] = ztensorShape[sourceIdx];
        accIdx++;
      }

      var acc = ZTensor.newZeros(accShape);
      for (var ks : acc.byCoords(BufferMode.REUSED)) {
        ZTensor slice = ztensor.selectDims(sliceDims, ks);
        acc.set(ks, reduceCellsAtomic(slice, op, initial));
      }
      return acc;
    }

    /**
     * Returns the sum of all elements in the tensor.
     *
     * @param tensor the tensor.
     * @return the int sum of all elements in the tensor.
     */
    public static int sumAsInt(@Nonnull HasZTensor tensor) {
      return reduceCellsAtomic(tensor, Integer::sum, 0);
    }

    /**
     * Returns the sum of all elements in the tensor.
     *
     * @param tensor the tensor.
     * @return the scalar ZTensor sum of all elements in the tensor.
     */
    @Nonnull
    public static ZTensor sum(@Nonnull HasZTensor tensor) {
      return reduceCells(tensor, Integer::sum, 0);
    }

    /**
     * Returns the sum of all elements in the tensor, grouped by the specified dimensions.
     *
     * <p>The shape of the returned tensor is the same as the shape of the input tensor, except that
     * the specified dimensions are removed.
     *
     * @param tensor the tensor.
     * @param dims the dimensions to group by.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor sum(@Nonnull HasZTensor tensor, @Nonnull int... dims) {
      return reduceCells(tensor, Integer::sum, 0, dims);
    }

    /**
     * Returns the product of all elements in the tensor.
     *
     * @param tensor the tensor.
     * @return the int product of all elements in the tensor.
     */
    public static int prodAsInt(@Nonnull HasZTensor tensor) {
      return reduceCellsAtomic(tensor, (a, b) -> a * b, 1);
    }

    /**
     * Returns the product of all elements in the tensor.
     *
     * @param tensor the tensor.
     * @return the scalar ZTensor product of all elements in the tensor.
     */
    @Nonnull
    public static ZTensor prod(@Nonnull HasZTensor tensor) {
      return reduceCells(tensor, (a, b) -> a * b, 1);
    }

    /**
     * Returns the product of all elements in the tensor, grouped by the specified dimensions.
     *
     * <p>The shape of the returned tensor is the same as the shape of the input tensor, except that
     * the specified dimensions are removed.
     *
     * @param tensor the tensor.
     * @param dims the dimensions to group by.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor prod(@Nonnull HasZTensor tensor, @Nonnull int... dims) {
      return reduceCells(tensor, (a, b) -> a * b, 1, dims);
    }

    /**
     * Returns the min of all elements in the tensor.
     *
     * @param tensor the tensor.
     * @return the int min of all elements in the tensor.
     */
    public static int minAsInt(@Nonnull HasZTensor tensor) {
      return reduceCellsAtomic(tensor, Math::min, Integer.MAX_VALUE);
    }

    /**
     * Returns the min of all elements in the tensor.
     *
     * @param tensor the tensor.
     * @return the scalar ZTensor min of all elements in the tensor.
     */
    @Nonnull
    public static ZTensor min(@Nonnull HasZTensor tensor) {
      return reduceCells(tensor, Math::min, Integer.MAX_VALUE);
    }

    /**
     * Returns the min of all elements in the tensor, grouped by the specified dimensions.
     *
     * <p>The shape of the returned tensor is the same as the shape of the input tensor, except that
     * the specified dimensions are removed.
     *
     * @param tensor the tensor.
     * @param dims the dimensions to group by.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor min(@Nonnull HasZTensor tensor, @Nonnull int... dims) {
      return reduceCells(tensor, Math::min, Integer.MAX_VALUE, dims);
    }

    /**
     * Returns the int max of all elements in the tensor.
     *
     * @param tensor the tensor.
     * @return the int min of all elements in the tensor.
     */
    public static int maxAsInt(@Nonnull HasZTensor tensor) {
      return reduceCellsAtomic(tensor, Math::max, Integer.MIN_VALUE);
    }

    /**
     * Returns the min of all elements in the tensor.
     *
     * @param tensor the tensor.
     * @return the scalar ZTensor max of all elements in the tensor.
     */
    @Nonnull
    public static ZTensor max(@Nonnull HasZTensor tensor) {
      return reduceCells(tensor, Math::max, Integer.MIN_VALUE);
    }

    /**
     * Returns the max of all elements in the tensor, grouped by the specified dimensions.
     *
     * <p>The shape of the returned tensor is the same as the shape of the input tensor, except that
     * the specified dimensions are removed.
     *
     * @param tensor the tensor.
     * @param dims the dimensions to group by.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor max(@Nonnull HasZTensor tensor, @Nonnull int... dims) {
      return reduceCells(tensor, Math::max, Integer.MIN_VALUE, dims);
    }
  }

  /**
   * ZTensor matrix operations.
   */
  @UtilityClass
  public final class Matrix {

    /**
     * Matrix multiplication of {@code lhs * rhs}.
     *
     * @param lhs the left-hand side tensor.
     * @param rhs the right-hand side tensor.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor matmul(@Nonnull HasZTensor lhs, @Nonnull HasZTensor rhs) {
      var zlhs = lhs.getTensor();
      var zrhs = rhs.getTensor();

      zlhs.assertNDim(2);
      if (zlhs.shape(1) != zrhs.shape(0)) {
        throw new IllegalArgumentException(
          "lhs shape %s not compatible with rhs shape %s".formatted(
              zlhs.shapeAsList(),
              zrhs.shapeAsList()
            )
        );
      }

      if (zrhs.getNDim() > 2 || zrhs.getNDim() == 0) {
        throw new IllegalArgumentException(
          "rhs must be a 1D or 2D tensor, got %dD: %s".formatted(zrhs.getNDim(), zrhs.shapeAsList())
        );
      }

      boolean rhsIsVector = zrhs.getNDim() == 1;
      if (rhsIsVector) {
        zrhs = zrhs.unsqueeze(1);
      }

      var res = ZTensor.newZeros(zlhs.shape(0), zrhs.shape(1));
      var coords = new int[2];
      for (int i = 0; i < zlhs.shape(0); ++i) {
        coords[0] = i;
        for (int j = 0; j < zrhs.shape(1); ++j) {
          coords[1] = j;
          int sum = 0;
          for (int k = 0; k < zlhs.shape(1); ++k) {
            sum += zlhs.get(i, k) * zrhs.get(k, j);
          }
          res.set(coords, sum);
        }
      }

      if (rhsIsVector) {
        res = res.squeeze(1);
      }

      return res;
    }
  }
}
