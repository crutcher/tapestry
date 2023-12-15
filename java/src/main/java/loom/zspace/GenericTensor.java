package loom.zspace;

import com.google.errorprone.annotations.CheckReturnValue;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.function.*;

@ApiStatus.Experimental
public final class GenericTensor<T> extends AbstractTensor<GenericTensor<T>, T[]> {
  /**
   * Constructs a Tensor from parts; takes ownership of the arrays.
   *
   * @param mutable whether the ZTensor is mutable.
   * @param shape the shape.
   * @param stride the strides.
   * @param data the data.
   * @param data_offset the offset in the source data.
   */
  GenericTensor(
      boolean mutable,
      @Nonnull int[] shape,
      @Nonnull int[] stride,
      @Nonnull T[] data,
      int data_offset) {
    super(mutable, shape, stride, data, data_offset);
  }

  @Override
  @Nonnull
  public GenericTensor<T> clone(boolean mutable) {
    if (isReadOnly() && isCompact() && !mutable) {
      return this;
    }

    var res = new GenericTensor<>(getArrayClass(), shape);
    forEachEntry(res::set, BufferMode.REUSED);
    if (!mutable) {
      return new GenericTensor<>(false, res.shape, res.stride, res.data, 0);
    }
    return res;
  }

  @Override
  protected int _dataHashCode() {
    return reduceCellsAtomic((a, b) -> 31 * a + b.hashCode(), Arrays.hashCode(shape));
  }

  /**
   * Create a new tensor by mapping a function over the values of the tensor.
   *
   * @param op the function to apply.
   * @return a new tensor.
   */
  @Nonnull
  public GenericTensor<T> map(@Nonnull UnaryOperator<T> op) {
    return Ops.map(op, this);
  }

  /**
   * An in-place element-wise unary operation.
   *
   * @param op the operation.
   */
  public void map_(UnaryOperator<T> op) {
    assignFromMap(op, this);
  }

  /**
   * Does every cell in this tensor match the given predicate?
   *
   * <p>Trivially true for an empty tensor.
   *
   * @param predicate the predicate.
   * @return true if every cell matches the predicate.
   */
  public boolean allMatch(@Nonnull Predicate<T> predicate) {
    return byCoords(BufferMode.REUSED).stream().allMatch(c -> predicate.test(get(c)));
  }

  /**
   * Does any cell in this tensor match the given predicate?
   *
   * <p>Trivially false for an empty tensor.
   *
   * @param predicate the predicate.
   * @return true if any cell matches the predicate.
   */
  public boolean anyMatch(@Nonnull Predicate<T> predicate) {
    return byCoords(BufferMode.REUSED).stream().anyMatch(c -> predicate.test(get(c)));
  }

  /**
   * Iterate over the coordinates and values of this tensor.
   *
   * <p>When the buffer mode is {@link BufferMode#REUSED}, the buffer is shared between subsequent
   * calls to {@link TensorEntryConsumer#accept(int[], int)}. When the buffer mode is {@link
   * BufferMode#SAFE}, the buffer is not shared between subsequent calls to {@link
   * TensorEntryConsumer#accept(int[], int)}.
   *
   * @param consumer the consumer.
   * @param bufferMode the buffer mode.
   */
  public void forEachEntry(@Nonnull BiConsumer<int[], T> consumer, @Nonnull BufferMode bufferMode) {
    for (int[] coords : byCoords(bufferMode)) {
      consumer.accept(coords, get(coords));
    }
  }

  /**
   * Iterate over the values of this tensor.
   *
   * @param consumer the consumer.
   */
  public void forEachValue(@Nonnull Consumer<T> consumer) {
    for (int[] coords : byCoords(BufferMode.REUSED)) {
      consumer.accept(get(coords));
    }
  }

  private static <A> A[] newArray(Class<A[]> arrType, int size) {
    return arrType.cast(Array.newInstance(arrType.getComponentType(), size));
  }

  /**
   * Construct a mutable 0-filled ZTensor of the given shape; takes ownership of the shape.
   *
   * @param arrType the array type.
   * @param shape the shape.
   */
  GenericTensor(Class<T[]> arrType, @Nonnull int[] shape) {
    super(true, shape, newArray(arrType, IndexingFns.shapeToSize(shape)));
  }

  /**
   * Construct a new mutable tensor filled with zeros.
   *
   * @param shape the shape of the tensor.
   * @return a new mutable ZTensor.
   */
  @Nonnull
  public static <T> GenericTensor<T> newZeros(Class<T[]> arrType, @Nonnull int... shape) {
    return new GenericTensor<>(arrType, shape.clone());
  }

  /**
   * Construct a new mutable ZTensor filled with zeros with a shape like the given ZTensor.
   *
   * @param ref the ZTensor to copy the shape from.
   * @return a new mutable ZTensor.
   */
  @Nonnull
  public static <T> GenericTensor<T> newZerosLike(@Nonnull GenericTensor<T> ref) {
    return new GenericTensor<>(ref.getArrayClass(), ref.shapeAsArray());
  }

  /**
   * Construct a new mutable scalar (0-dim) tensor.
   *
   * @param value the scalar value.
   * @return the new tensor.
   */
  @Nonnull
  public static <A> GenericTensor<A> newScalar(Class<A[]> arrType, A value) {
    var data = newArray(arrType, 1);
    data[0] = value;
    return new GenericTensor<>(true, new int[] {}, new int[] {}, data, 0);
  }

  @Nonnull
  public static <A> GenericTensor<A> newScalar(A value) {
    @SuppressWarnings("unchecked")
    var resultArrayClass = (Class<A[]>) Array.newInstance(value.getClass(), 0).getClass();

    var data = newArray(resultArrayClass, 1);
    data[0] = value;
    return new GenericTensor<>(true, new int[] {}, new int[] {}, data, 0);
  }

  /**
   * Assign from an element-wise unary operation.
   *
   * @param op the operation.
   * @param tensor the input tensor.
   */
  public void assignFromMap(@Nonnull UnaryOperator<T> op, @Nonnull GenericTensor<T> tensor) {
    assertMutable();
    tensor
        .broadcastLike(this)
        .forEachEntry(
            (coords, value) -> _unchecked_set(coords, op.apply(value)), BufferMode.REUSED);
  }

  /**
   * Assign from an element-wise binary operation.
   *
   * @param op the operation.
   * @param lhs the left-hand side tensor.
   * @param rhs the right-hand side tensor.
   */
  public <L, R> void assignFromZipWith(
      @Nonnull BiFunction<L, R, T> op,
      @Nonnull GenericTensor<L> lhs,
      @Nonnull GenericTensor<R> rhs) {
    assertMutable();
    lhs = lhs.broadcastLike(this);
    rhs = rhs.broadcastLike(this);
    for (int[] coords : byCoords(BufferMode.REUSED)) {
      _unchecked_set(coords, op.apply(lhs.get(coords), rhs.get(coords)));
    }
  }

  /**
   * Get the cell-value at the given coordinates.
   *
   * @param coords the coordinates.
   * @return the cell value.
   */
  public T get(@Nonnull int... coords) {
    return data[ravel(coords)];
  }

  /**
   * Set the cell-value at the given coordinates.
   *
   * <p>Assumes the tensor is mutable.
   *
   * @param coords the coordinates.
   * @param value the value to set.
   * @throws IndexOutOfBoundsException if the coordinates are out of bounds.
   */
  private void _unchecked_set(@Nonnull int[] coords, T value) {
    data[ravel(coords)] = value;
  }

  /**
   * Set the cell-value at the given coordinates.
   *
   * @param coords the coordinates.
   * @param value the value to set.
   * @throws IndexOutOfBoundsException if the coordinates are out of bounds.
   * @throws IllegalStateException if the tensor is read-only.
   */
  public void set(@Nonnull int[] coords, T value) {
    assertMutable();
    _unchecked_set(coords, value);
  }

  /**
   * Applies the given reduction operation to all values in the given tensor.
   *
   * @param op the reduction operation
   * @param initial the initial value
   * @return the int result of the reduction.
   */
  public <V> V reduceCellsAtomic(@Nonnull BiFunction<V, T, V> op, V initial) {
    return Ops.reduceCellsAtomic(this, op, initial);
  }

  /**
   * Applies the given reduction operation to all values in the given tensor.
   *
   * @param op the reduction operation
   * @param initial the initial value
   * @return a new tensor.
   */
  @Nonnull
  public GenericTensor<T> reduceCells(@Nonnull BinaryOperator<T> op, T initial) {
    return Ops.reduceCells(this, op, initial);
  }

  /**
   * Applies the given reduction operation to all values in the given tensor; grouping by the
   * specified dimensions.
   *
   * <p>The shape of the returned tensor is the same as the shape of the input tensor, except that
   * the specified dimensions are removed.
   *
   * @param op the reduction operation
   * @param initial the initial value
   * @param dims the dimensions to group by.
   * @return a new tensor.
   */
  @Nonnull
  public <R> GenericTensor<R> reduceCells(
      @Nonnull BiFunction<R, T, R> op, R initial, @Nonnull int... dims) {
    return Ops.reduceCells(this, op, initial, dims);
  }

  public static class Ops {
    private Ops() {}

    /**
     * An element-wise unary operation.
     *
     * @param op the operation.
     * @param tensor the input tensor.
     * @return a new tensor.
     */
    @CheckReturnValue
    @Nonnull
    public static <T> GenericTensor<T> map(
        @Nonnull UnaryOperator<T> op, @Nonnull GenericTensor<T> tensor) {
      var result = newZerosLike(tensor);
      result.assignFromMap(op, tensor);
      return result;
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
    public static <T> GenericTensor<T> zipWith(
        @Nonnull BinaryOperator<T> op,
        @Nonnull GenericTensor<T> lhs,
        @Nonnull GenericTensor<T> rhs) {
      var result =
          newZeros(lhs.getArrayClass(), IndexingFns.commonBroadcastShape(lhs.shape, rhs.shape));
      result.assignFromZipWith(op, lhs, rhs);
      return result;
    }

    @Nonnull
    public static <T, V, R> GenericTensor<R> zipWith(
        Class<R[]> resultArrayClass,
        @Nonnull BiFunction<T, V, R> op,
        @Nonnull GenericTensor<T> lhs,
        @Nonnull GenericTensor<V> rhs) {
      var result =
          newZeros(resultArrayClass, IndexingFns.commonBroadcastShape(lhs.shape, rhs.shape));
      result.assignFromZipWith(op, lhs, rhs);
      return result;
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
    public static <T> GenericTensor<T> zipWith(
        @Nonnull BinaryOperator<T> op, @Nonnull GenericTensor<T> lhs, T rhs) {
      return zipWith(op, lhs, newScalar(lhs.getArrayClass(), rhs));
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
    public static <T> GenericTensor<T> zipWith(
        @Nonnull BinaryOperator<T> op, T lhs, @Nonnull GenericTensor<T> rhs) {
      return zipWith(op, newScalar(rhs.getArrayClass(), lhs), rhs);
    }

    /**
     * Applies the given reduction operation to all values in the given tensor.
     *
     * @param tensor the tensor
     * @param op the reduction operation
     * @param initial the initial value
     * @return the int result of the reduction.
     */
    public static <T, V> V reduceCellsAtomic(
        @Nonnull GenericTensor<T> tensor, @Nonnull BiFunction<V, T, V> op, V initial) {
      var acc =
          new Consumer<T>() {
            V value = initial;

            @Override
            public void accept(T value) {
              this.value = op.apply(this.value, value);
            }
          };

      tensor.forEachValue(acc);
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
    public static <T, R> GenericTensor<R> reduceCells(
        @Nonnull GenericTensor<T> tensor, @Nonnull BiFunction<R, T, R> op, R initial) {
      @SuppressWarnings("unchecked")
      var resultArrayClass = (Class<R[]>) Array.newInstance(initial.getClass(), 0).getClass();

      return newScalar(resultArrayClass, reduceCellsAtomic(tensor, op, initial));
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
    public static <R, T> GenericTensor<R> reduceCells(
        @Nonnull GenericTensor<T> tensor,
        @Nonnull BiFunction<R, T, R> op,
        R initial,
        @Nonnull int... dims) {
      @SuppressWarnings("unchecked")
      var resultArrayClass = (Class<R[]>) Array.newInstance(initial.getClass(), 0).getClass();

      var sumDims = tensor.resolveDims(dims);

      var sliceDims = new int[tensor.getNDim() - sumDims.length];

      var accShape = new int[tensor.getNDim() - sumDims.length];
      for (int sourceIdx = 0, accIdx = 0; sourceIdx < tensor.getNDim(); ++sourceIdx) {
        if (IndexingFns.arrayContains(sumDims, sourceIdx)) {
          continue;
        }

        sliceDims[accIdx] = sourceIdx;
        accShape[accIdx] = tensor.shape[sourceIdx];
        accIdx++;
      }

      GenericTensor<R> acc = newZeros(resultArrayClass, accShape);
      for (var ks : acc.byCoords(BufferMode.REUSED)) {
        GenericTensor<T> slice = tensor.selectDims(sliceDims, ks);
        acc.set(ks, reduceCellsAtomic(slice, op, initial));
      }
      return acc;
    }
  }
}
