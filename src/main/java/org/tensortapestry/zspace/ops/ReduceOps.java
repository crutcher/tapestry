package org.tensortapestry.zspace.ops;

import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import javax.annotation.Nonnull;
import lombok.experimental.UtilityClass;
import org.tensortapestry.zspace.ZTensor;
import org.tensortapestry.zspace.ZTensorWrapper;
import org.tensortapestry.zspace.indexing.BufferOwnership;
import org.tensortapestry.zspace.indexing.IndexingFns;

/**
 * ZTensor reduce operations.
 */
@UtilityClass
public class ReduceOps {

  /**
   * Applies the given reduction operation to all values in the given tensor.
   *
   * @param tensor the tensor
   * @param op the reduction operation
   * @param initial the initial value
   * @return the int result of the reduction.
   */
  public int reduceCellsAtomic(
    @Nonnull ZTensorWrapper tensor,
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

    tensor.unwrap().forEachValue(acc);
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
  public ZTensor reduceCells(
    @Nonnull ZTensorWrapper tensor,
    @Nonnull IntBinaryOperator op,
    int initial
  ) {
    return ZTensor.newScalar(reduceCellsAtomic(tensor, op, initial));
  }

  /**
   * Applies the given reduction operation to all values in the given tensor; grouping by the
   * specified dimensions.
   *
   * <p>The shape of the returned tensor is the same as the shape of the input tensor, except
   * that the specified dimensions are removed.
   *
   * @param tensor the tensor
   * @param op the reduction operation
   * @param initial the initial value
   * @param dims the dimensions to group by.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor reduceCells(
    @Nonnull ZTensorWrapper tensor,
    @Nonnull IntBinaryOperator op,
    int initial,
    @Nonnull int... dims
  ) {
    var ztensor = tensor.unwrap();
    int nDim = ztensor.getNDim();
    int[] shape = ztensor.shapeAsArray();
    var sumDims = ztensor.resolveDims(dims);

    int k = nDim - sumDims.length;
    var sliceDims = new int[k];
    var accShape = new int[k];

    for (int sourceIdx = 0, accIdx = 0; sourceIdx < nDim; ++sourceIdx) {
      if (IndexingFns.arrayContains(sumDims, sourceIdx)) {
        continue;
      }
      sliceDims[accIdx] = sourceIdx;
      accShape[accIdx] = shape[sourceIdx];
      accIdx++;
    }

    var acc = ZTensor.newZeros(accShape);
    for (var ks : acc.byCoords(BufferOwnership.REUSED)) {
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
  public int sumAsInt(@Nonnull ZTensorWrapper tensor) {
    return reduceCellsAtomic(tensor, Integer::sum, 0);
  }

  /**
   * Returns the sum of all elements in the tensor.
   *
   * @param tensor the tensor.
   * @return the scalar ZTensor sum of all elements in the tensor.
   */
  @Nonnull
  public ZTensor sum(@Nonnull ZTensorWrapper tensor) {
    return reduceCells(tensor, Integer::sum, 0);
  }

  /**
   * Returns the sum of all elements in the tensor, grouped by the specified dimensions.
   *
   * <p>The shape of the returned tensor is the same as the shape of the input tensor, except
   * that the specified dimensions are removed.
   *
   * @param tensor the tensor.
   * @param dims the dimensions to group by.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor sum(@Nonnull ZTensorWrapper tensor, @Nonnull int... dims) {
    return reduceCells(tensor, Integer::sum, 0, dims);
  }

  /**
   * Returns the product of all elements in the tensor.
   *
   * @param tensor the tensor.
   * @return the int product of all elements in the tensor.
   */
  public int prodAsInt(@Nonnull ZTensorWrapper tensor) {
    return reduceCellsAtomic(tensor, (a, b) -> a * b, 1);
  }

  /**
   * Returns the product of all elements in the tensor.
   *
   * @param tensor the tensor.
   * @return the scalar ZTensor product of all elements in the tensor.
   */
  @Nonnull
  public ZTensor prod(@Nonnull ZTensorWrapper tensor) {
    return reduceCells(tensor, (a, b) -> a * b, 1);
  }

  /**
   * Returns the product of all elements in the tensor, grouped by the specified dimensions.
   *
   * <p>The shape of the returned tensor is the same as the shape of the input tensor, except
   * that the specified dimensions are removed.
   *
   * @param tensor the tensor.
   * @param dims the dimensions to group by.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor prod(@Nonnull ZTensorWrapper tensor, @Nonnull int... dims) {
    return reduceCells(tensor, (a, b) -> a * b, 1, dims);
  }

  /**
   * Returns the min of all elements in the tensor.
   *
   * @param tensor the tensor.
   * @return the int min of all elements in the tensor.
   */
  public int minAsInt(@Nonnull ZTensorWrapper tensor) {
    return reduceCellsAtomic(tensor, Math::min, Integer.MAX_VALUE);
  }

  /**
   * Returns the min of all elements in the tensor.
   *
   * @param tensor the tensor.
   * @return the scalar ZTensor min of all elements in the tensor.
   */
  @Nonnull
  public ZTensor min(@Nonnull ZTensorWrapper tensor) {
    return reduceCells(tensor, Math::min, Integer.MAX_VALUE);
  }

  /**
   * Returns the min of all elements in the tensor, grouped by the specified dimensions.
   *
   * <p>The shape of the returned tensor is the same as the shape of the input tensor, except
   * that the specified dimensions are removed.
   *
   * @param tensor the tensor.
   * @param dims the dimensions to group by.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor min(@Nonnull ZTensorWrapper tensor, @Nonnull int... dims) {
    return reduceCells(tensor, Math::min, Integer.MAX_VALUE, dims);
  }

  /**
   * Returns the int max of all elements in the tensor.
   *
   * @param tensor the tensor.
   * @return the int min of all elements in the tensor.
   */
  public int maxAsInt(@Nonnull ZTensorWrapper tensor) {
    return reduceCellsAtomic(tensor, Math::max, Integer.MIN_VALUE);
  }

  /**
   * Returns the min of all elements in the tensor.
   *
   * @param tensor the tensor.
   * @return the scalar ZTensor max of all elements in the tensor.
   */
  @Nonnull
  public ZTensor max(@Nonnull ZTensorWrapper tensor) {
    return reduceCells(tensor, Math::max, Integer.MIN_VALUE);
  }

  /**
   * Returns the max of all elements in the tensor, grouped by the specified dimensions.
   *
   * <p>The shape of the returned tensor is the same as the shape of the input tensor, except
   * that the specified dimensions are removed.
   *
   * @param tensor the tensor.
   * @param dims the dimensions to group by.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor max(@Nonnull ZTensorWrapper tensor, @Nonnull int... dims) {
    return reduceCells(tensor, Math::max, Integer.MIN_VALUE, dims);
  }
}
