package loom.zspace;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.primitives.Ints;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractTensor<T extends AbstractTensor<T, ArrayT>, ArrayT>
    implements Cloneable, HasDimension, HasSize, HasPermute<T> {
  @Getter protected final boolean mutable;

  @Nonnull protected final int[] shape;
  @Getter protected final int size;
  @Nonnull protected final int[] stride;

  @Nonnull protected final ArrayT data;
  protected final int data_offset;
  protected Integer hash;

  public AbstractTensor(
      boolean mutable,
      @Nonnull int[] shape,
      @Nonnull int[] stride,
      @Nonnull ArrayT data,
      int data_offset) {
    this.mutable = mutable;

    this.shape = shape;
    this.size = IndexingFns.shapeToSize(shape);
    this.stride = stride;

    this.data = data;
    this.data_offset = data_offset;
  }

  public AbstractTensor(
      boolean mutable, @Nonnull int[] shape, @Nonnull int[] stride, @Nonnull ArrayT data) {
    this(mutable, shape, stride, data, 0);
  }

  public AbstractTensor(boolean mutable, @Nonnull int[] shape, @Nonnull ArrayT data) {
    this(mutable, shape, IndexingFns.shapeToLSFStrides(shape), data, 0);
  }

  public Class<?> componentType() {
    return data.getClass().getComponentType();
  }

  /**
   * Return this cast to the subclass type.
   *
   * @return this cast to the subclass type.
   */
  private T self() {
    @SuppressWarnings("unchecked")
    var self = (T) this;
    return self;
  }

  private Class<T> selfClass() {
    @SuppressWarnings("unchecked")
    var selfClass = (Class<T>) getClass();
    return selfClass;
  }

  /**
   * Create a new subclass instance.
   *
   * @param mutable whether the new instance should be mutable.
   * @param shape the shape of the new instance.
   * @param stride the stride of the new instance.
   * @param data the data of the new instance.
   * @param data_offset the data offset of the new instance.
   * @return the new instance.
   */
  private T create(
      boolean mutable,
      @Nonnull int[] shape,
      @Nonnull int[] stride,
      @Nonnull ArrayT data,
      int data_offset) {
    try {
      return selfClass()
          .getDeclaredConstructor(
              boolean.class, int[].class, int[].class, data.getClass(), int.class)
          .newInstance(mutable, shape, stride, data, data_offset);

    } catch (NoSuchMethodException
        | InstantiationException
        | IllegalAccessException
        | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Assert that this tensor has the given number of dimensions.
   *
   * @param ndim the number of dimensions.
   */
  @Override
  public final void assertNDim(int ndim) {
    HasDimension.assertNDim(ndim, getNDim());
  }

  /**
   * Assert that this tensor has the given shape.
   *
   * @param shape the shape.
   */
  public final void assertShape(@Nonnull int... shape) {
    IndexingFns.assertShape(this.shape, shape);
  }

  /**
   * Assert that this tensor has the same shape as another.
   *
   * @param other the other tensor.
   */
  public final void assertMatchingShape(@Nonnull ZTensor other) {
    IndexingFns.assertShape(shape, other.shape);
  }

  /**
   * Return an immutable tensor with the same data.
   *
   * <p>If this tensor is already immutable, returns this; otherwise, returns an immutable clone.
   *
   * <p>Semantically equivalent to {@code clone(false)}.
   *
   * <p>A performance oriented Tensor library would track open mutable views of the underlying data,
   * and perform copy-on-write when necessary; as this is a correctness-oriented Tensor library, we
   * simply clone the data to go from mutable to immutable.
   *
   * @return an immutable tensor.
   */
  @Nonnull
  public final T asImmutable() {
    return clone(false);
  }

  /**
   * Clone this tensor.
   *
   * <p>If this tensor is immutable and compact, returns this.
   *
   * <p>If this tensor is immutable and non-compact, returns a compact clone.
   *
   * <p>If this tensor is mutable, returns a compact mutable clone.
   *
   * @return a tensor with the same data.
   */
  @Override
  @SuppressWarnings("MethodDoesntCallSuperMethod")
  public final T clone() {
    return clone(mutable);
  }

  /**
   * Clone this ZTensor.
   *
   * <p>If this ZTensor is immutable and compact, and mutable is false, returns this.
   *
   * <p>Otherwise, returns a new compact ZTensor with the same data and the given mutability.
   *
   * @param mutable whether the clone should be mutable.
   * @return a new ZTensor with the same data.
   */
  @Nonnull
  public abstract T clone(boolean mutable);

  /**
   * Return if this tensor is compact.
   *
   * <p>A tensor is compact if its data array is exactly the size of the tensor.
   *
   * @return true if this tensor is compact.
   */
  public final boolean isCompact() {
    return Array.getLength(data) == size;
  }

  /**
   * Is this ZTensor read-only / immutable?
   *
   * @return true if read-only / immutable; false otherwise.
   */
  public final boolean isReadOnly() {
    return !mutable;
  }

  /** Asserts that this ZTensor is mutable. */
  public final void assertMutable() {
    if (!mutable) {
      throw new IllegalStateException("ZTensor is immutable");
    }
  }

  /** Asserts that this ZTensor is read-only / immutable. */
  public final void assertReadOnly() {
    if (mutable) {
      throw new IllegalStateException("ZTensor is mutable");
    }
  }

  /**
   * Get the shape of this tensor along a given dimension.
   *
   * @param dim the dimension; supports negative indexing.
   * @return the size of the dimension.
   */
  public final int shape(int dim) {
    return shape[resolveDim(dim)];
  }

  /**
   * Returns the shape of this tensor.
   *
   * @return a copy of the shape array.
   */
  @Nonnull
  public final int[] shapeAsArray() {
    return shape.clone();
  }

  /**
   * Returns the shape of this tensor as a list.
   *
   * @return an immutable shape list.
   */
  @Nonnull
  public final List<Integer> shapeAsList() {
    return Collections.unmodifiableList(Ints.asList(shape));
  }

  /**
   * Returns the shape of this tensor as a ZTensor.
   *
   * @return the shape of this tensor as a ZTensor.
   */
  @Nonnull
  public final ZTensor shapeAsTensor() {
    return ZTensor.newVector(shape);
  }

  @Override
  public final int getNDim() {
    return shape.length;
  }

  /**
   * Resolve a dimension index.
   *
   * <p>Negative dimension indices are resolved relative to the number of dimensions.
   *
   * @param dim the dimension index.
   * @return the resolved dimension index.
   * @throws IndexOutOfBoundsException if the index is out of range.
   */
  public final int resolveDim(int dim) {
    return IndexingFns.resolveDim(dim, shape);
  }

  /**
   * Resolve dimension indexes.
   *
   * <p>Negative dimension indices are resolved relative to the number of dimensions.
   *
   * @param dims the dimension indexes.
   * @return the resolved dimension indexes.
   */
  @Nonnull
  public final int[] resolveDims(int... dims) {
    return IndexingFns.resolveDims(dims, shape);
  }

  /**
   * Returns an {@code Iterable<int[]>} over the coordinates of this tensor.
   *
   * <p>When the buffer mode is {@link BufferMode#REUSED}, the buffer is shared between subsequent
   * calls to {@link Iterator#next()}. When the buffer mode is {@link BufferMode#SAFE}, the buffer
   * is not shared between subsequent calls to {@link Iterator#next()}.
   *
   * <p>Empty tensors will return an empty iterable.
   *
   * <p>Scalar tensors will return an iterable with a single empty coordinate array.
   *
   * @param bufferMode the buffer mode.
   * @return an iterable over the coordinates of this tensor.
   */
  @Nonnull
  public final IterableCoordinates byCoords(@Nonnull BufferMode bufferMode) {
    return new IterableCoordinates(bufferMode, shape);
  }

  /**
   * Compute the ravel index into the data array for the given coordinates.
   *
   * @param coords the coordinates.
   * @return the ravel index.
   */
  protected final int ravel(@Nonnull int... coords) {
    return data_offset + IndexingFns.ravel(shape, stride, coords);
  }

  @Override
  public final T permute(@Nonnull int... permutation) {
    var perm = IndexingFns.resolvePermutation(permutation, getNDim());

    return create(
        mutable,
        IndexingFns.applyResolvedPermutation(shape, perm),
        IndexingFns.applyResolvedPermutation(stride, perm),
        data,
        data_offset);
  }

  /**
   * Transposes (swaps) two dimensions of this tensor.
   *
   * <p>This method creates a new view of the tensor where the specified dimensions are swapped. The
   * original tensor remains unchanged. The returned tensor shares data with the original tensor.
   *
   * <p>This operation can be useful in scenarios where you need to change the order of two
   * dimensions in a tensor, for example, when you want to switch rows and columns in a 2D tensor
   * (matrix).
   *
   * <p>Supports negative dimension indexing - i.e. -1 represents the last dimension, -2 represents
   * the second last, and so on.
   *
   * @param a The index of the first dimension to be transposed.
   * @param b The index of the second dimension to be transposed.
   * @return A new ZTensor that is a transposed view of the original tensor.
   * @throws IllegalArgumentException If the provided indices are not valid dimensions of the
   *     tensor.
   */
  @Nonnull
  public final T transpose(int a, int b) {
    int rA = resolveDim(a);
    int rB = resolveDim(b);
    if (rA == rB) {
      return self();
    }

    int[] perm = IndexingFns.iota(getNDim());
    perm[rA] = rB;
    perm[rB] = rA;
    return permute(perm);
  }

  /**
   * Transpose this tensor by reversing its dimensions.
   *
   * @return a transposed view of this tensor.
   */
  @Nonnull
  public final T transpose() {
    return permute(IndexingFns.aoti(getNDim()));
  }

  /**
   * Transpose this tensor by reversing its dimensions.
   *
   * <p>Alias for {@link #transpose()}.
   *
   * @return a transposed view of this tensor.
   */
  @Nonnull
  public final T T() {
    return transpose();
  }

  /**
   * Returns a view of this tensor with the given dimension reversed.
   *
   * @param dim the dimension to reverse, accepts negative indices.
   * @return a view of this tensor with the given dimension reversed.
   */
  @Nonnull
  public final T reverse(int dim) {
    int rDim = resolveDim(dim);

    int[] newStride = stride.clone();
    newStride[rDim] *= -1;

    int newOffset = data_offset + (shape[rDim] - 1) * stride[rDim];

    return create(mutable, shape, newStride, data, newOffset);
  }

  /**
   * Create a view of this tensor with an extra dimension added at index `d`.
   *
   * @param dim the dimension to add.
   * @return a view of this tensor with an extra dimension added at index `d`.
   */
  @Nonnull
  public final T unsqueeze(int dim) {
    int rDim = IndexingFns.resolveDim(dim, getNDim() + 1);

    int[] newShape = new int[getNDim() + 1];
    int[] newStride = new int[getNDim() + 1];

    System.arraycopy(shape, 0, newShape, 0, rDim);
    System.arraycopy(shape, rDim, newShape, rDim + 1, getNDim() - rDim);

    System.arraycopy(stride, 0, newStride, 0, rDim);
    System.arraycopy(stride, rDim, newStride, rDim + 1, getNDim() - rDim);

    newShape[rDim] = 1;
    newStride[rDim] = 0;

    return create(mutable, newShape, newStride, data, data_offset);
  }

  /**
   * Returns a view of this tensor with a dimensions of size 1 removed.
   *
   * @param dim the dimension to remove; accepts negative indices.
   * @return a view of this tensor with a dimensions of size 1 removed.
   */
  @Nonnull
  public final T squeeze(int dim) {
    int rDim = resolveDim(dim);

    if (stride[rDim] != 0) {
      throw new IllegalArgumentException(
          "dimension " + rDim + ", shape " + shape[rDim] + " is not squeezable");
    }

    return create(
        mutable,
        IndexingFns.removeIdx(shape, rDim),
        IndexingFns.removeIdx(stride, rDim),
        data,
        data_offset);
  }

  /**
   * Return a view of this tensor with a broadcastable dimension expanded.
   *
   * @param dim the dimension to expand (must be size 1, or a previously broadcasted dimension).
   * @param size the new size of the dimension.
   * @return a view of this tensor with a broadcastable dimension expanded.
   */
  @Nonnull
  public final T broadcastDim(int dim, int size) {
    dim = resolveDim(dim);
    if (stride[dim] != 0) {
      throw new IllegalArgumentException(
          "Cannot broadcast dimension %d with real-size %d".formatted(dim, shape[dim]));
    }

    var new_shape = shapeAsArray();
    new_shape[dim] = size;

    var new_stride = stride.clone();
    new_stride[dim] = 0;

    return create(mutable, new_shape, new_stride, data, data_offset);
  }

  /**
   * Is this dimension broadcasted (i.e. has stride 0 but a shape > 1)?
   *
   * @param dim the dimension to check; supports negative indices.
   * @return true if the dimension is broadcasted.
   */
  @JsonIgnore
  public final boolean isBroadcastDim(int dim) {
    dim = resolveDim(dim);
    return shape[dim] > 1 && stride[dim] == 0;
  }

  /**
   * Return a view of this tensor broadcasted like the reference tensor.
   *
   * @param ref the reference tensor.
   * @return a broadcasted view of this tensor.
   */
  @Nonnull
  public final T broadcastLike(@Nonnull ZTensor ref) {
    return broadcastTo(ref.shape);
  }

  /**
   * Return a view of this tensor broadcasted to the given shape.
   *
   * @param targetShape the target shape.
   * @return a broadcasted view of this tensor.
   */
  @Nonnull
  public final T broadcastTo(@Nonnull int... targetShape) {
    if (Arrays.equals(shape, targetShape)) {
      return self();
    }

    if (isScalar() && IndexingFns.shapeToSize(targetShape) == 0) {
      @SuppressWarnings("unchecked")
      var emptyArray = (ArrayT) Array.newInstance(componentType(), 0);
      return create(
          mutable, targetShape, IndexingFns.shapeToLSFStrides(targetShape), emptyArray, 0);
    }

    var res = self();
    if (res.getNDim() > targetShape.length) {
      throw new IllegalArgumentException(
          "Cannot broadcast shape "
              + Arrays.toString(shape)
              + " to "
              + Arrays.toString(targetShape));
    }
    while (res.getNDim() < targetShape.length) {
      res = res.unsqueeze(0);
    }
    for (int i = 0; i < targetShape.length; ++i) {
      if (res.shape[i] > 1 && res.shape[i] != targetShape[i]) {
        throw new IllegalArgumentException(
            "Cannot broadcast shape "
                + Arrays.toString(this.shape)
                + " to "
                + Arrays.toString(targetShape));
      }
      if (res.shape[i] == 1 && targetShape[i] > 1) {
        res = res.broadcastDim(i, targetShape[i]);
      }
    }
    return res;
  }

  /**
   * Return a view of this tensor with the given dimension selected.
   *
   * @param dim the dimension to select.
   * @param index the index to select.
   * @return a view of this tensor with the given dimension selected.
   */
  @Nonnull
  public final T selectDim(int dim, int index) {
    var d = resolveDim(dim);
    var i = IndexingFns.resolveIndex("index", index, shape[d]);

    var new_shape = shapeAsArray();
    new_shape[d] = 1;
    var new_stride = stride.clone();
    new_stride[d] = 0;
    int new_offset = data_offset + i * stride[d];

    return create(mutable, new_shape, new_stride, data, new_offset).squeeze(d);
  }

  /**
   * Return a view of this tensor with the given dimension selected.
   *
   * @param dims the dimensions to select.
   * @param indexes the matching indexes to select.
   * @return a view of this tensor with the given dimensions selected.
   */
  @Nonnull
  public final T selectDims(@Nonnull int[] dims, @Nonnull int[] indexes) {
    if (dims.length != indexes.length) {
      throw new IllegalArgumentException(
          "dims.length (%d) != indexes.length (%d)".formatted(dims.length, indexes.length));
    }

    var ds = resolveDims(dims);
    var is = new int[indexes.length];
    for (int i = 0; i < indexes.length; ++i) {
      is[i] = IndexingFns.resolveIndex("index", indexes[i], shape[ds[i]]);
    }

    var res = self();
    for (int i = 0; i < ds.length; ++i) {
      res = res.selectDim(ds[i] - i, is[i]);
    }
    return res;
  }
}
