package loom.zspace;

import com.google.errorprone.annotations.CheckReturnValue;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;
import javax.annotation.Nonnull;
import lombok.NoArgsConstructor;

/** ZTensor math operations. */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class ZTensorOperations {
  /**
   * Matrix multiplication of {@code lhs * rhs}.
   *
   * @param lhs the left-hand side tensor.
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public static ZTensor matmul(@Nonnull HasZTensor lhs, @Nonnull HasZTensor rhs) {
    var zlhs = lhs.asZTensor();
    var zrhs = rhs.asZTensor();

    zlhs.assertNDim(2);
    if (zlhs.shape(1) != zrhs.shape(0)) {
      throw new IllegalArgumentException(
          "lhs shape %s not compatible with rhs shape %s"
              .formatted(zlhs.shapeAsList(), zrhs.shapeAsList()));
    }

    if (zrhs.getNDim() > 2 || zrhs.getNDim() == 0) {
      throw new IllegalArgumentException(
          "rhs must be a 1D or 2D tensor, got %dD: %s"
              .formatted(zrhs.getNDim(), zrhs.shapeAsList()));
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
  @CheckReturnValue
  @Nonnull
  public static ZTensor map(@Nonnull IntUnaryOperator op, @Nonnull HasZTensor tensor) {
    var result = ZTensor.newZerosLike(tensor);
    result.assignFromMap(op, tensor);
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
      @Nonnull IntBinaryOperator op, @Nonnull HasZTensor lhs, @Nonnull HasZTensor rhs) {
    var zlhs = lhs.asZTensor();
    var zrhs = rhs.asZTensor();
    var result =
        ZTensor.newZeros(
            IndexingFns.commonBroadcastShape(zlhs._unsafeGetShape(), zrhs._unsafeGetShape()));
    result.assignFromZipWith(op, zlhs, zrhs);
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
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public static ZTensor mod(@Nonnull HasZTensor lhs, @Nonnull HasZTensor rhs) {
    return zipWith((l, r) -> l % r, lhs, rhs);
  }

  /**
   * Element-wise broadcast mod.
   *
   * @param lhs the left-hand side scalar.
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public static ZTensor mod(@Nonnull HasZTensor lhs, int rhs) {
    return zipWith((l, r) -> l % r, lhs, rhs);
  }

  /**
   * Element-wise broadcast mod.
   *
   * @param lhs the left-hand side scalar.
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public static ZTensor mod(int lhs, @Nonnull HasZTensor rhs) {
    return zipWith((l, r) -> l % r, lhs, rhs);
  }

  /**
   * Element-wise broadcast in-place mod on the lhs.
   *
   * @param lhs the left-hand side tensor, modified in-place; must be mutable.
   * @param rhs the right-hand side tensor.
   */
  public static void mod_(@Nonnull ZTensor lhs, @Nonnull HasZTensor rhs) {
    lhs.zipWith_((l, r) -> l % r, rhs);
  }

  /**
   * Element-wise broadcast in-place mod on the lhs.
   *
   * @param lhs the left-hand side tensor, modified in-place; must be mutable.
   * @param rhs the right-hand side tensor.
   */
  public static void mod_(@Nonnull ZTensor lhs, int rhs) {
    lhs.zipWith_((l, r) -> l % r, rhs);
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
   * Applies the given reduction operation to all values in the given tensor.
   *
   * @param tensor the tensor
   * @param op the reduction operation
   * @param initial the initial value
   * @return the int result of the reduction.
   */
  public static int reduceCellsAtomic(
      @Nonnull HasZTensor tensor, @Nonnull IntBinaryOperator op, int initial) {
    var acc =
        new IntConsumer() {
          int value = initial;

          @Override
          public void accept(int value) {
            this.value = op.applyAsInt(this.value, value);
          }
        };

    tensor.asZTensor().forEachValue(acc);
    return acc.value;
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
   * Applies the given reduction operation to all values in the given tensor.
   *
   * @param tensor the tensor
   * @param op the reduction operation
   * @param initial the initial value
   * @return a new tensor.
   */
  @Nonnull
  public static ZTensor reduceCells(
      @Nonnull HasZTensor tensor, @Nonnull IntBinaryOperator op, int initial) {
    return ZTensor.newScalar(reduceCellsAtomic(tensor, op, initial));
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
      @Nonnull int... dims) {
    var ztensor = tensor.asZTensor();
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
