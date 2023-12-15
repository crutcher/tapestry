package loom.zspace;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.errorprone.annotations.CheckReturnValue;
import lombok.SneakyThrows;
import loom.common.HasToJsonString;
import loom.common.IteratorUtils;
import loom.common.serialization.JsonUtil;

import javax.annotation.Nonnull;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.*;

/**
 * A multidimensional int array used for numerical operations.
 *
 * <p>ZTensor is a class representing a multidimensional array used for numerical operations,
 * commonly known as a tensor. Important functionalities of this class include mutability, views,
 * and cloning. These concepts functionally impact the class's interactions with operators and
 * storage management.
 *
 * <p>As this tensor exists solely to support discrete space range calculations, C/C++/JNI/CUDA/BLAS
 * accelerations are not needed; the focus is on what permits the block expression index projection
 * math to be as readable as possible; targeting succinctness with range and shape assertions.
 *
 * <h2>Mutability</h2>
 *
 * <p>A ZTensor can be mutable or immutable. A mutable ZTensor permits modifications to its values
 * after creation. In contrast, an immutable ZTensor guarantees that its values remain fixed
 * post-creation. This can be advantageous in ensuring consistent state.
 *
 * <h2>Views</h2>
 *
 * <p>A ZTensor view offers an alternative perspective of the original tensor's data without
 * duplicating it. It operates on a selected slice or arranged view of the original tensor. Any
 * updates made to the original tensor are reflected in all of its views and vice versa. Also,
 * mutating a view mutates the original tensor if both are mutable.
 *
 * <h2>Cloning</h2>
 *
 * <p>Creating a clone of a ZTensor results in an independent ZTensor instance with the same values
 * but separate underlying data. Consequently, changes in the cloned tensor will not affect the
 * original ZTensor - a useful attribute when performing mutating operations without touching the
 * original tensor. However, cloning may incur additional memory costs, as separate storage for the
 * clone's data is created.
 *
 * <p>Note: The clone of an immutable tensor <b>may</b> return the original tensor; when the
 * immutable tensor is {@link #isCompact()}.
 *
 * <h2>Serialization</h2>
 *
 * <p>The ZTensor serialization format is a JSON array of integers; and degenerate ZTensors (those
 * with a mix of 0-sized and non-0-sized dimensions) will serialize down to the minimal empty tensor
 * in the given dimension.
 *
 * <h2>Shape, Size, and Scalar Tensors</h2>
 *
 * <p>The shape of a ZTensor describes the dimensions of the tensor, and the size of a tensor is
 * defined as the product of those dimensions. For example, a tensor with shape [3, 4, 5] is a
 * 3-dimensional tensor, with size 3*4*5=60
 *
 * <p>A tensor with any dimension of size 0 will have size 0, and thus be an empty tensor.
 *
 * <h3>Scalar Tensors</h3>
 *
 * <p>A scalar tensor is a tensor with no-dimensions, and shape []. The product of an empty list is
 * 1, so the size of a scalar tensor is 1. Scalar tensors are often used to represent single
 * dimensionless values.
 */
@JsonSerialize(using = ZTensor.Serialization.Serializer.class)
@JsonDeserialize(using = ZTensor.Serialization.Deserializer.class)
public final class ZTensor extends AbstractTensor<ZTensor, int[]> implements HasToJsonString {

  /**
   * Constructs a ZTensor from parts; takes ownership of the arrays.
   *
   * @param mutable whether the ZTensor is mutable.
   * @param shape the shape.
   * @param stride the strides.
   * @param data the data.
   * @param data_offset the offset in the source data.
   */
  ZTensor(
      boolean mutable,
      @Nonnull int[] shape,
      @Nonnull int[] stride,
      @Nonnull int[] data,
      int data_offset) {
    super(mutable, shape, stride, data, data_offset);
  }

  /**
   * Constructs a ZTensor from parts; takes ownership of the arrays.
   *
   * <p>Assumes an offset of 0.
   *
   * @param mutable whether the ZTensor is mutable.
   * @param shape the shape.
   * @param stride the strides.
   * @param data the data.
   */
  ZTensor(boolean mutable, @Nonnull int[] shape, @Nonnull int[] stride, @Nonnull int[] data) {
    super(mutable, shape, stride, data);
  }

  /**
   * Constructs a ZTensor from parts; takes ownership of the arrays.
   *
   * <p>Assumes an offset of 0, and default strides.
   *
   * @param mutable whether the ZTensor is mutable.
   * @param shape the shape.
   * @param data the data.
   */
  ZTensor(boolean mutable, @Nonnull int[] shape, @Nonnull int[] data) {
    super(mutable, shape, data);
  }

  /**
   * Construct a 0-filled ZTensor of the given shape; takes ownership of the shape.
   *
   * @param mutable whether the ZTensor is mutable.
   * @param shape the shape.
   */
  ZTensor(boolean mutable, @Nonnull int[] shape) {
    super(mutable, shape, new int[IndexingFns.shapeToSize(shape)]);
  }

  /**
   * Construct a new mutable scalar (0-dim) tensor.
   *
   * @param value the scalar value.
   * @return the new tensor.
   */
  @Nonnull
  public static ZTensor newScalar(int value) {
    return new ZTensor(true, new int[] {}, new int[] {value});
  }

  /**
   * Construct a new mutable vector (1-dim) tensor.
   *
   * @param values the vector values.
   * @return the new tensor.
   */
  @Nonnull
  public static ZTensor newVector(@Nonnull int... values) {
    return fromArray(values);
  }

  /**
   * Construct a new mutable vector (1-dim) tensor.
   *
   * @param values the vector values.
   * @return the new tensor.
   */
  @Nonnull
  public static ZTensor newVector(@Nonnull Iterable<Integer> values) {
    return newVector(IteratorUtils.iterableToStream(values).mapToInt(Integer::intValue).toArray());
  }

  /**
   * Construct an iota tensor [0, ..., size-1].
   *
   * @param size the size of the tensor.
   * @return the new tensor.
   */
  @Nonnull
  public static ZTensor newIota(int size) {
    return newVector(IndexingFns.iota(size));
  }

  /**
   * Construct a new mutable matrix tensor (2-dim) from the given values.
   *
   * @param rows the {@code int[][]} values.
   * @return the new tensor.
   */
  @Nonnull
  public static ZTensor newMatrix(@Nonnull int[]... rows) {
    int numRows = rows.length;

    int numCols = 0;
    if (numRows > 0) {
      numCols = rows[0].length;
    }

    int[] shape = new int[] {numRows, numCols};
    int[] data = Arrays.stream(rows).flatMapToInt(Arrays::stream).toArray();
    return new ZTensor(true, shape, IndexingFns.shapeToLSFStrides(shape), data);
  }

  /**
   * Construct a new mutable tensor filled with zeros.
   *
   * @param shape the shape of the tensor.
   * @return a new mutable ZTensor.
   */
  @Nonnull
  public static ZTensor newZeros(@Nonnull int... shape) {
    return new ZTensor(true, shape.clone());
  }

  /**
   * Construct a new mutable ZTensor filled with zeros with a shape like the given ZTensor.
   *
   * @param ref the ZTensor to copy the shape from.
   * @return a new mutable ZTensor.
   */
  @Nonnull
  public static ZTensor newZerosLike(@Nonnull ZTensor ref) {
    return new ZTensor(true, ref.shapeAsArray());
  }

  /**
   * Construct a new mutable tensor filled with the given fill value.
   *
   * @param shape the shape of the tensor.
   * @param fill_value the value to fill the tensor with.
   * @return a new mutable ZTensor.
   */
  @Nonnull
  public static ZTensor newFilled(@Nonnull int[] shape, int fill_value) {
    var size = IndexingFns.shapeToSize(shape);
    var data = new int[size];
    Arrays.fill(data, fill_value);
    return new ZTensor(true, shape.clone(), data);
  }

  /**
   * Construct a new mutable ZTensor filled with the fill value with a shape like the given ZTensor.
   *
   * @param ref the ZTensor to copy the shape from.
   * @param fill_value the value to fill the tensor with.
   * @return a new mutable ZTensor.
   */
  @Nonnull
  public static ZTensor newFilledLike(@Nonnull ZTensor ref, int fill_value) {
    return newFilled(ref.shape, fill_value);
  }

  /**
   * Construct a new mutable tensor filled with ones.
   *
   * @param shape the shape of the tensor.
   * @return a new mutable ZTensor.
   */
  @Nonnull
  public static ZTensor newOnes(@Nonnull int... shape) {
    return newFilled(shape, 1);
  }

  /**
   * Construct a new mutable ZTensor filled with ones with a shape like the given ZTensor.
   *
   * @param ref the ZTensor to copy the shape from.
   * @return a new mutable ZTensor.
   */
  @Nonnull
  public static ZTensor newOnesLike(@Nonnull ZTensor ref) {
    return newFilledLike(ref, 1);
  }

  /**
   * Construct a diagonal matrix from a list of values.
   *
   * @param diag the values to put on the diagonal.
   * @return a new ZTensor.
   */
  public static ZTensor newDiagonal(@Nonnull int... diag) {
    var tensor = newZeros(diag.length, diag.length);
    for (int i = 0; i < diag.length; ++i) {
      tensor._unchecked_set(new int[] {i, i}, diag[i]);
    }
    return tensor;
  }

  /**
   * Construct an identity matrix of size nxn.
   *
   * @param n the size of a side of the matrix.
   * @return a new ZTensor.
   */
  public static ZTensor newIdentityMatrix(int n) {
    int[] diag = new int[n];
    Arrays.fill(diag, 1);
    return newDiagonal(diag);
  }

  /**
   * Given a non-sparse array of unknown dimensionality, returns a ZTensor with the same shape and
   * data.
   *
   * @param source the source array.
   * @return a new ZTensor.
   */
  @Nonnull
  public static ZTensor fromArray(@Nonnull Object source) {
    return fromTree(
        source,
        obj -> obj.getClass().isArray(),
        Array::getLength,
        Array::get,
        obj -> (int) obj,
        int[].class::cast);
  }

  /**
   * Parse a ZTensor from a string.
   *
   * @param str the string.
   * @return the new tensor.
   * @throws IllegalArgumentException if the string is not a valid ZTensor.
   */
  @Nonnull
  public static ZTensor parse(@Nonnull String str) {
    return JsonUtil.fromJson(str, ZTensor.class);
  }

  /**
   * Decode a ZTensor from a tree of type {@code <T>}.
   *
   * <p>This is common used by the Jackson deserializer and the {@link #fromArray} method.
   *
   * @param <T> the type of the tree.
   * @param root the root of the tree.
   * @param isArray is this node an array, or a scalar?
   * @param getArrayLength get the length of this array.
   * @param getArrayElement get the ith element of this array.
   * @param nodeAsScalar get the value of this scalar.
   * @param nodeAsSimpleArray get a coherent chunk of data for a final layer array.
   * @return a new ZTensor.
   */
  public static <T> @Nonnull ZTensor fromTree(
      @Nonnull T root,
      @Nonnull Predicate<T> isArray,
      @Nonnull ToIntFunction<T> getArrayLength,
      @Nonnull BiFunction<T, Integer, T> getArrayElement,
      @Nonnull ToIntFunction<T> nodeAsScalar,
      @Nonnull Function<T, int[]> nodeAsSimpleArray) {

    if (!isArray.test(root)) {
      return loom.zspace.ZTensor.newScalar(nodeAsScalar.applyAsInt(root));
    }

    List<Integer> shapeList = new ArrayList<>();
    {
      var it = root;
      while (isArray.test(it)) {
        var size = getArrayLength.applyAsInt(it);
        shapeList.add(size);
        if (size == 0) {
          break;
        } else {
          it = getArrayElement.apply(it, 0);
        }
      }
    }

    var ndim = shapeList.size();

    if (shapeList.contains(0)) {
      // Handle degenerate tensors.
      return loom.zspace.ZTensor.newZeros(new int[ndim]);
    }

    int[] shape = shapeList.stream().mapToInt(i -> i).toArray();

    var tensor = new ZTensor(true, shape);

    int chunkCount = 0;
    int chunkStride = tensor.shape[ndim - 1];

    for (int[] coords : tensor.byCoords(BufferMode.REUSED)) {
      if (coords[ndim - 1] != 0) continue;

      var it = root;
      for (int d = 0; d < ndim - 1; ++d) {
        it = getArrayElement.apply(it, coords[d]);
      }

      int[] chunk = nodeAsSimpleArray.apply(it);

      System.arraycopy(chunk, 0, tensor.data, chunkCount * chunkStride, chunkStride);
      chunkCount++;
    }

    return tensor;
  }

  /**
   * Construct an immutable ZPoint from this ZTensor. Asserts that this is a 1-dim tensor.
   *
   * @return a new immutable ZPoint.
   */
  public ZPoint newZPoint() {
    return new ZPoint(this);
  }

  /**
   * Serialize this tensor to a tree data structure.
   *
   * <p>This is common code used by the Jackson serializer and the {@link #toArray} method.
   *
   * @param startArray start an array.
   * @param endArray end an array.
   * @param elemSep write an element separator.
   * @param writeNumber write a number.
   */
  public void toTree(
      @Nonnull Runnable startArray,
      @Nonnull Runnable endArray,
      @Nonnull Runnable elemSep,
      @Nonnull Consumer<Integer> writeNumber) {
    if (isScalar()) {
      writeNumber.accept(get());
      return;
    }

    int ndim = getNDim();

    if (isEmpty()) {
      for (int d = 0; d < ndim; ++d) {
        startArray.run();
      }
      for (int d = 0; d < ndim; ++d) {
        endArray.run();
      }
      return;
    }

    for (int[] coords : byCoords(BufferMode.REUSED)) {
      for (int d = ndim - 1; d >= 0; --d) {
        if (coords[d] == 0) {
          startArray.run();
        } else {
          break;
        }
      }

      writeNumber.accept(get(coords));

      for (int d = ndim - 1; d >= 0; --d) {
        if (coords[d] != shape[d] - 1) {
          elemSep.run();
          break;

        } else {
          endArray.run();
        }
      }
    }
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null || getClass() != other.getClass()) return false;
    var that = (ZTensor) other;

    if (this.getNDim() != that.getNDim()) {
      return false;
    }

    for (var coords : byCoords(BufferMode.REUSED)) {
      if (that.get(coords) != get(coords)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    if (mutable) {
      throw new IllegalStateException("Cannot take the hash of a mutable tensor.");
    }
    if (hash == null) {
      synchronized (this) {
        hash = reduceCellsAsInt((a, b) -> 31 * a + b, Arrays.hashCode(shape));
      }
    }
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    toTree(() -> sb.append('['), () -> sb.append(']'), () -> sb.append(", "), sb::append);

    return sb.toString();
  }

  /**
   * Convert this tensor to a Java array. Scalars will be converted to a single int.
   *
   * @return the new java structure.
   */
  @Nonnull
  public Object toArray() {
    // TODO: scalars?
    if (isScalar()) {
      return item();
    }

    Object arr = Array.newInstance(int.class, shape);

    var ndim = getNDim();
    for (int[] coords : byCoords(BufferMode.REUSED)) {
      var it = arr;
      for (int d = 0; d < ndim - 1; ++d) {
        it = Array.get(it, coords[d]);
      }
      int[] chunk = (int[]) it;
      chunk[coords[ndim - 1]] = get(coords);
    }

    return arr;
  }

  @Override
  @Nonnull
  public ZTensor clone(boolean mutable) {
    if (isReadOnly() && isCompact() && !mutable) {
      return this;
    }

    var res = new ZTensor(true, shape);
    forEachEntry(res::set, BufferMode.REUSED);
    if (!mutable) {
      return new ZTensor(false, res.shape, res.stride, res.data, 0);
    }
    return res;
  }

  /** Are all cells in this tensor > 0? */
  public boolean isStrictlyPositive() {
    return allMatch(x -> x > 0);
  }

  /**
   * Does every cell in this tensor match the given predicate?
   *
   * <p>Trivially true for an empty tensor.
   *
   * @param predicate the predicate.
   * @return true if every cell matches the predicate.
   */
  public boolean allMatch(@Nonnull IntPredicate predicate) {
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
  public boolean anyMatch(@Nonnull IntPredicate predicate) {
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
  public void forEachEntry(@Nonnull TensorEntryConsumer consumer, @Nonnull BufferMode bufferMode) {
    for (int[] coords : byCoords(bufferMode)) {
      consumer.accept(coords, get(coords));
    }
  }

  /**
   * Iterate over the values of this tensor.
   *
   * @param consumer the consumer.
   */
  public void forEachValue(@Nonnull IntConsumer consumer) {
    for (int[] coords : byCoords(BufferMode.REUSED)) {
      consumer.accept(get(coords));
    }
  }

  /**
   * Get the cell-value at the given coordinates.
   *
   * @param coords the coordinates.
   * @return the cell value.
   */
  public int get(@Nonnull int... coords) {
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
  private void _unchecked_set(@Nonnull int[] coords, int value) {
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
  public void set(@Nonnull int[] coords, int value) {
    assertMutable();
    _unchecked_set(coords, value);
  }

  /** Get the value of this tensor as a T0 (a scalar). */
  public int item() {
    return toT0();
  }

  /** Convert this structure to a T0 (a scalar) value. Assert that the shape is valid. */
  public int toT0() {
    assertNDim(0);
    return get();
  }

  /** Convert this structure to a T1 (a vector) value. Assert that the shape is valid. */
  @Nonnull
  public int[] toT1() {
    assertNDim(1);
    return (int[]) toArray();
  }

  /** Convert this structure to a T2 (a matrix) value. Assert that the shape is valid. */
  @Nonnull
  public int[][] toT2() {
    assertNDim(2);
    return (int[][]) toArray();
  }

  /**
   * Fill the tensor with a value.
   *
   * @param fill_value the value to fill with.
   */
  public void fill(int fill_value) {
    assertMutable();
    for (int[] coords : byCoords(BufferMode.REUSED)) {
      _unchecked_set(coords, fill_value);
    }
  }

  /**
   * Assign from a tensor.
   *
   * @param tensor the input tensor.
   */
  public void assign(@Nonnull ZTensor tensor) {
    assertMutable();
    tensor.broadcastLike(this).forEachEntry(this::_unchecked_set, BufferMode.REUSED);
  }

  /**
   * Assign from an element-wise unary operation.
   *
   * @param op the operation.
   * @param tensor the input tensor.
   */
  public void assignFromMap(@Nonnull IntUnaryOperator op, @Nonnull ZTensor tensor) {
    assertMutable();
    tensor
        .broadcastLike(this)
        .forEachEntry(
            (coords, value) -> _unchecked_set(coords, op.applyAsInt(value)), BufferMode.REUSED);
  }

  /**
   * Assign from an element-wise binary operation.
   *
   * @param op the operation.
   * @param lhs the left-hand side tensor.
   * @param rhs the right-hand side tensor.
   */
  public void assignFromZipWith(
      @Nonnull IntBinaryOperator op, @Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
    assertMutable();
    lhs = lhs.broadcastLike(this);
    rhs = rhs.broadcastLike(this);
    for (int[] coords : byCoords(BufferMode.REUSED)) {
      _unchecked_set(coords, op.applyAsInt(lhs.get(coords), rhs.get(coords)));
    }
  }

  /**
   * Create a new tensor by mapping a function over the values of the tensor.
   *
   * @param op the function to apply.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor map(@Nonnull IntUnaryOperator op) {
    return Ops.map(op, this);
  }

  /**
   * An in-place element-wise unary operation.
   *
   * @param op the operation.
   */
  public void map_(IntUnaryOperator op) {
    assignFromMap(op, this);
  }

  /**
   * Broadcasts two tensors together, and maps a function over the values of the tensors.
   *
   * @param op the function to apply.
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor zipWith(@Nonnull IntBinaryOperator op, @Nonnull ZTensor rhs) {
    return Ops.zipWith(op, this, rhs);
  }

  /**
   * An in-place element-wise binary operation.
   *
   * @param op the operation.
   * @param rhs the right-hand side tensor.
   */
  public void zipWith_(@Nonnull IntBinaryOperator op, @Nonnull ZTensor rhs) {
    assignFromZipWith(op, this, rhs);
  }

  /**
   * An in-place element-wise binary operation.
   *
   * @param op the operation.
   * @param rhs the right-hand side scalar.
   */
  public void zipWith_(@Nonnull IntBinaryOperator op, int rhs) {
    zipWith_(op, ZTensor.newScalar(rhs));
  }

  /**
   * Applies the given reduction operation to all values in the given tensor.
   *
   * @param op the reduction operation
   * @param initial the initial value
   * @return the int result of the reduction.
   */
  public int reduceCellsAsInt(@Nonnull IntBinaryOperator op, int initial) {
    return Ops.reduceCellsAsInt(this, op, initial);
  }

  /**
   * Applies the given reduction operation to all values in the given tensor.
   *
   * @param op the reduction operation
   * @param initial the initial value
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor reduceCells(@Nonnull IntBinaryOperator op, int initial) {
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
  public ZTensor reduceCells(@Nonnull IntBinaryOperator op, int initial, @Nonnull int... dims) {
    return Ops.reduceCells(this, op, initial, dims);
  }

  /**
   * Matrix multiplication agaist the given rhs tensor.
   *
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  public ZTensor matmul(@Nonnull ZTensor rhs) {
    return Ops.matmul(this, rhs);
  }

  /**
   * Returns the sum of all elements in the tensor.
   *
   * @return the int sum of all elements in the tensor.
   */
  public int sumAsInt() {
    return Ops.sumAsInt(this);
  }

  /**
   * Returns the sum of all elements in the tensor.
   *
   * @return the scalar ZTensor sum of all elements in the tensor.
   */
  @Nonnull
  public ZTensor sum() {
    return Ops.sum(this);
  }

  /**
   * Returns the sum of all elements in the tensor, grouped by the specified dimensions.
   *
   * <p>The shape of the returned tensor is the same as the shape of the input tensor, except that
   * the specified dimensions are removed.
   *
   * @param dims the dimensions to group by.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor sum(@Nonnull int... dims) {
    return Ops.sum(this, dims);
  }

  /**
   * Returns the product of all elements in the tensor.
   *
   * @return the int prod of all elements in the tensor.
   */
  public int prodAsInt() {
    return Ops.prodAsInt(this);
  }

  /**
   * Returns the product of all elements in the tensor.
   *
   * @return the scalar ZTensor prod of all elements in the tensor.
   */
  @Nonnull
  public ZTensor prod() {
    return Ops.prod(this);
  }

  /**
   * Returns the product of all elements in the tensor, grouped by the specified dimensions.
   *
   * <p>The shape of the returned tensor is the same as the shape of the input tensor, except that
   * the specified dimensions are removed.
   *
   * @param dims the dimensions to group by.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor prod(@Nonnull int... dims) {
    return Ops.prod(this, dims);
  }

  /**
   * Returns the minimum of all elements in the tensor.
   *
   * @return the int minimum of all elements in the tensor.
   */
  public int minAsInt() {
    return Ops.minAsInt(this);
  }

  /**
   * Returns a new scalar ZTensor that contains the minimum value in this ZTensor.
   *
   * @return a new ZTensor object containing the minimum value
   */
  @Nonnull
  public ZTensor min() {
    return Ops.min(this);
  }

  /**
   * Returns a new ZTensor that contains the minimum value in this ZTensor, grouped by the specified
   * dimensions.
   *
   * <p>The shape of the returned tensor is the same as the shape of the input tensor, except that
   * the specified dimensions are removed.
   *
   * @param dims the dimensions to group by.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor min(@Nonnull int... dims) {
    return Ops.min(this, dims);
  }

  /**
   * Returns the maximum of all elements in the tensor.
   *
   * @return the int maximum of all elements in the tensor.
   */
  public int maxAsInt() {
    return Ops.maxAsInt(this);
  }

  /**
   * Returns a new scalar ZTensor that contains the maximum value in this ZTensor.
   *
   * @return a new ZTensor object containing the maximum value
   */
  @Nonnull
  public ZTensor max() {
    return Ops.max(this);
  }

  /**
   * Returns a new ZTensor that contains the maximum value in this ZTensor, grouped by the specified
   * dimensions.
   *
   * <p>The shape of the returned tensor is the same as the shape of the input tensor, except that
   * the specified dimensions are removed.
   *
   * @param dims the dimensions to group by.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor max(@Nonnull int... dims) {
    return Ops.max(this, dims);
  }

  /** Returns a new elementwise negation of this tensor. */
  @Nonnull
  public ZTensor neg() {
    return Ops.neg(this);
  }

  /** Returns a new elementwise absolute value of this tensor. */
  @Nonnull
  public ZTensor abs() {
    return Ops.abs(this);
  }

  /** Returns an elementwise broadcast addition with this tensor. */
  @Nonnull
  public ZTensor add(@Nonnull ZTensor rhs) {
    return Ops.add(this, rhs);
  }

  /** Returns an elementwise broadcast addition with this tensor. */
  @Nonnull
  public ZTensor add(int rhs) {
    return Ops.add(this, rhs);
  }

  /**
   * Performs an in-place elementwise broadcast addition on this tensor.
   *
   * <p>This tensor must be mutable.
   *
   * @param rhs the right-hand side tensor.
   */
  public void add_(@Nonnull ZTensor rhs) {
    Ops.add_(this, rhs);
  }

  /**
   * Performs an in-place elementwise broadcast addition on this tensor.
   *
   * <p>This tensor must be mutable.
   *
   * @param rhs the right-hand side tensor.
   */
  public void add_(int rhs) {
    Ops.add_(this, rhs);
  }

  /**
   * Returns an elementwise broadcast subtraction with this tensor.
   *
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor sub(@Nonnull ZTensor rhs) {
    return Ops.sub(this, rhs);
  }

  /**
   * Returns an elementwise broadcast subtraction with this tensor.
   *
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor sub(int rhs) {
    return Ops.sub(this, rhs);
  }

  /**
   * Performs an in-place elementwise broadcast subtraction on this tensor.
   *
   * <p>This tensor must be mutable.
   *
   * @param rhs the right-hand side tensor.
   */
  public void sub_(@Nonnull ZTensor rhs) {
    Ops.sub_(this, rhs);
  }

  /**
   * Performs an in-place elementwise broadcast subtraction on this tensor.
   *
   * <p>This tensor must be mutable.
   *
   * @param rhs the right-hand side tensor.
   */
  public void sub_(int rhs) {
    Ops.sub_(this, rhs);
  }

  /**
   * Returns an elementwise broadcast multiplication with this tensor.
   *
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor mul(@Nonnull ZTensor rhs) {
    return Ops.mul(this, rhs);
  }

  /**
   * Returns an elementwise broadcast multiplication with this tensor.
   *
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor mul(int rhs) {
    return Ops.mul(this, rhs);
  }

  /**
   * Performs an in-place elementwise broadcast multiplication on this tensor.
   *
   * <p>This tensor must be mutable.
   *
   * @param rhs the right-hand side tensor.
   */
  public void mul_(@Nonnull ZTensor rhs) {
    Ops.mul_(this, rhs);
  }

  /**
   * Performs an in-place elementwise broadcast multiplication on this tensor.
   *
   * <p>This tensor must be mutable.
   *
   * @param rhs the right-hand side tensor.
   */
  public void mul_(int rhs) {
    Ops.mul_(this, rhs);
  }

  /**
   * Returns an elementwise broadcast division with this tensor.
   *
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor div(@Nonnull ZTensor rhs) {
    return Ops.div(this, rhs);
  }

  /**
   * Returns an elementwise broadcast division with this tensor.
   *
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor div(int rhs) {
    return Ops.div(this, rhs);
  }

  /**
   * Performs an in-place elementwise broadcast division on this tensor.
   *
   * <p>This tensor must be mutable.
   *
   * @param rhs the right-hand side tensor.
   */
  public void div_(@Nonnull ZTensor rhs) {
    Ops.div_(this, rhs);
  }

  /**
   * Performs an in-place elementwise broadcast division on this tensor.
   *
   * <p>This tensor must be mutable.
   *
   * @param rhs the right-hand side tensor.
   */
  public void div_(int rhs) {
    Ops.div_(this, rhs);
  }

  /**
   * Returns an elementwise broadcast mod with this tensor.
   *
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor mod(@Nonnull ZTensor rhs) {
    return Ops.mod(this, rhs);
  }

  /**
   * Returns an elementwise broadcast mod with this tensor.
   *
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor mod(int rhs) {
    return Ops.mod(this, rhs);
  }

  /**
   * Performs an in-place elementwise broadcast mod on this tensor.
   *
   * <p>This tensor must be mutable.
   *
   * @param rhs the right-hand side tensor.
   */
  public void mod_(@Nonnull ZTensor rhs) {
    Ops.mod_(this, rhs);
  }

  /**
   * Performs an in-place elementwise broadcast mod on this tensor.
   *
   * <p>This tensor must be mutable.
   *
   * @param rhs the right-hand side tensor.
   */
  public void mod_(int rhs) {
    Ops.mod_(this, rhs);
  }

  /** ZTensor math operations. */
  public static final class Ops {
    /** Prevent instantiation. */
    private Ops() {}

    /**
     * Matrix multiplication of {@code lhs * rhs}.
     *
     * @param lhs the left-hand side tensor.
     * @param rhs the right-hand side tensor.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor matmul(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
      lhs.assertNDim(2);
      if (lhs.shape(1) != rhs.shape(0)) {
        throw new IllegalArgumentException(
            "lhs shape %s not compatible with rhs shape %s"
                .formatted(lhs.shapeAsList(), rhs.shapeAsList()));
      }

      if (rhs.getNDim() > 2 || rhs.getNDim() == 0) {
        throw new IllegalArgumentException(
            "rhs must be a 1D or 2D tensor, got %dD".formatted(rhs.getNDim()));
      }

      boolean rhsIsVector = rhs.getNDim() == 1;
      if (rhsIsVector) {
        rhs = rhs.unsqueeze(1);
      }

      var res = newZeros(lhs.shape(0), rhs.shape(1));
      var coords = new int[2];
      for (int i = 0; i < lhs.shape(0); ++i) {
        coords[0] = i;
        for (int j = 0; j < rhs.shape(1); ++j) {
          coords[1] = j;
          int sum = 0;
          for (int k = 0; k < lhs.shape(1); ++k) {
            sum += lhs.get(i, k) * rhs.get(k, j);
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
     * An element-wise unary operation.
     *
     * @param op the operation.
     * @param tensor the input tensor.
     * @return a new tensor.
     */
    @CheckReturnValue
    @Nonnull
    public static ZTensor map(@Nonnull IntUnaryOperator op, @Nonnull ZTensor tensor) {
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
    public static ZTensor zipWith(
        @Nonnull IntBinaryOperator op, @Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
      var result = newZeros(IndexingFns.commonBroadcastShape(lhs.shape, rhs.shape));
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
    public static ZTensor zipWith(@Nonnull IntBinaryOperator op, @Nonnull ZTensor lhs, int rhs) {
      return zipWith(op, lhs, loom.zspace.ZTensor.newScalar(rhs));
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
    public static ZTensor zipWith(@Nonnull IntBinaryOperator op, int lhs, @Nonnull ZTensor rhs) {
      return zipWith(op, loom.zspace.ZTensor.newScalar(lhs), rhs);
    }

    /**
     * Elementwise negation of a tensor.
     *
     * @param tensor the input tensor.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor neg(@Nonnull ZTensor tensor) {
      return map(x -> -x, tensor);
    }

    /**
     * Elementwise absolute value of a tensor.
     *
     * @param tensor the input tensor.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor abs(@Nonnull ZTensor tensor) {
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
    public static ZTensor minimum(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
      return zipWith(Math::min, lhs, rhs);
    }

    /**
     * Element-wise broadcast minimum.
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor minimum(@Nonnull ZTensor lhs, int rhs) {
      return zipWith(Math::min, lhs, rhs);
    }

    /**
     * Element-wise broadcast minimum.
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor minimum(int lhs, @Nonnull ZTensor rhs) {
      return zipWith(Math::min, lhs, rhs);
    }

    /**
     * Element-wise broadcast maximum.
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return a new tensor.
     */
    @Nonnull
    public static ZTensor maximum(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
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
    public static ZTensor maximum(@Nonnull ZTensor lhs, int rhs) {
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
    public static ZTensor maximum(int lhs, @Nonnull ZTensor rhs) {
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
    public static ZTensor add(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
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
    public static ZTensor add(@Nonnull ZTensor lhs, int rhs) {
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
    public static ZTensor add(int lhs, @Nonnull ZTensor rhs) {
      return zipWith(Integer::sum, lhs, rhs);
    }

    /**
     * Element-wise broadcast in-place addition on the lhs.
     *
     * @param lhs the left-hand side tensor, modified in-place; must be mutable.
     * @param rhs the right-hand side tensor.
     */
    public static void add_(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
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
    public static ZTensor sub(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
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
    public static ZTensor sub(@Nonnull ZTensor lhs, int rhs) {
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
    public static ZTensor sub(int lhs, @Nonnull ZTensor rhs) {
      return zipWith((l, r) -> l - r, lhs, rhs);
    }

    /**
     * Element-wise broadcast in-place subtraction on the lhs.
     *
     * @param lhs the left-hand side tensor, modified in-place; must be mutable.
     * @param rhs the right-hand side tensor.
     */
    public static void sub_(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
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
    public static ZTensor mul(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
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
    public static ZTensor mul(@Nonnull ZTensor lhs, int rhs) {
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
    public static ZTensor mul(int lhs, @Nonnull ZTensor rhs) {
      return zipWith((l, r) -> l * r, lhs, rhs);
    }

    /**
     * Element-wise broadcast in-place multiplication on the lhs.
     *
     * @param lhs the left-hand side tensor, modified in-place; must be mutable.
     * @param rhs the right-hand side tensor.
     */
    public static void mul_(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
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
    public static ZTensor div(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
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
    public static ZTensor div(@Nonnull ZTensor lhs, int rhs) {
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
    public static ZTensor div(int lhs, @Nonnull ZTensor rhs) {
      return zipWith((l, r) -> l / r, lhs, rhs);
    }

    /**
     * Element-wise broadcast in-place division on the lhs.
     *
     * @param lhs the left-hand side tensor, modified in-place; must be mutable.
     * @param rhs the right-hand side tensor.
     */
    public static void div_(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
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
    public static ZTensor mod(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
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
    public static ZTensor mod(@Nonnull ZTensor lhs, int rhs) {
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
    public static ZTensor mod(int lhs, @Nonnull ZTensor rhs) {
      return zipWith((l, r) -> l % r, lhs, rhs);
    }

    /**
     * Element-wise broadcast in-place mod on the lhs.
     *
     * @param lhs the left-hand side tensor, modified in-place; must be mutable.
     * @param rhs the right-hand side tensor.
     */
    public static void mod_(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
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
     * Applies the given reduction operation to all values in the given tensor.
     *
     * @param tensor the tensor
     * @param op the reduction operation
     * @param initial the initial value
     * @return the int result of the reduction.
     */
    public static int reduceCellsAsInt(
        @Nonnull ZTensor tensor, @Nonnull IntBinaryOperator op, int initial) {
      var acc =
          new IntConsumer() {
            int value = initial;

            @Override
            public void accept(int value) {
              this.value = op.applyAsInt(this.value, value);
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
    public static ZTensor reduceCells(
        @Nonnull ZTensor tensor, @Nonnull IntBinaryOperator op, int initial) {
      return newScalar(reduceCellsAsInt(tensor, op, initial));
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
        @Nonnull ZTensor tensor, @Nonnull IntBinaryOperator op, int initial, @Nonnull int... dims) {
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

      var acc = newZeros(accShape);
      for (var ks : acc.byCoords(BufferMode.REUSED)) {
        ZTensor slice = tensor.selectDims(sliceDims, ks);
        acc.set(ks, reduceCellsAsInt(slice, op, initial));
      }
      return acc;
    }

    /**
     * Returns the sum of all elements in the tensor.
     *
     * @param tensor the tensor.
     * @return the int sum of all elements in the tensor.
     */
    public static int sumAsInt(@Nonnull ZTensor tensor) {
      return reduceCellsAsInt(tensor, Integer::sum, 0);
    }

    /**
     * Returns the sum of all elements in the tensor.
     *
     * @param tensor the tensor.
     * @return the scalar ZTensor sum of all elements in the tensor.
     */
    @Nonnull
    public static ZTensor sum(@Nonnull ZTensor tensor) {
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
    public static ZTensor sum(@Nonnull ZTensor tensor, @Nonnull int... dims) {
      return reduceCells(tensor, Integer::sum, 0, dims);
    }

    /**
     * Returns the product of all elements in the tensor.
     *
     * @param tensor the tensor.
     * @return the int product of all elements in the tensor.
     */
    public static int prodAsInt(@Nonnull ZTensor tensor) {
      return reduceCellsAsInt(tensor, (a, b) -> a * b, 1);
    }

    /**
     * Returns the product of all elements in the tensor.
     *
     * @param tensor the tensor.
     * @return the scalar ZTensor product of all elements in the tensor.
     */
    @Nonnull
    public static ZTensor prod(@Nonnull ZTensor tensor) {
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
    public static ZTensor prod(@Nonnull ZTensor tensor, @Nonnull int... dims) {
      return reduceCells(tensor, (a, b) -> a * b, 1, dims);
    }

    /**
     * Returns the min of all elements in the tensor.
     *
     * @param tensor the tensor.
     * @return the int min of all elements in the tensor.
     */
    public static int minAsInt(@Nonnull ZTensor tensor) {
      return reduceCellsAsInt(tensor, Math::min, Integer.MAX_VALUE);
    }

    /**
     * Returns the min of all elements in the tensor.
     *
     * @param tensor the tensor.
     * @return the scalar ZTensor min of all elements in the tensor.
     */
    @Nonnull
    public static ZTensor min(@Nonnull ZTensor tensor) {
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
    public static ZTensor min(@Nonnull ZTensor tensor, @Nonnull int... dims) {
      return reduceCells(tensor, Math::min, Integer.MAX_VALUE, dims);
    }

    /**
     * Returns the int max of all elements in the tensor.
     *
     * @param tensor the tensor.
     * @return the int min of all elements in the tensor.
     */
    public static int maxAsInt(@Nonnull ZTensor tensor) {
      return reduceCellsAsInt(tensor, Math::max, Integer.MIN_VALUE);
    }

    /**
     * Returns the min of all elements in the tensor.
     *
     * @param tensor the tensor.
     * @return the scalar ZTensor max of all elements in the tensor.
     */
    @Nonnull
    public static ZTensor max(@Nonnull ZTensor tensor) {
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
    public static ZTensor max(@Nonnull ZTensor tensor, @Nonnull int... dims) {
      return reduceCells(tensor, Math::max, Integer.MIN_VALUE, dims);
    }
  }

  /**
   * Serialization Support namespace.
   *
   * <ul>
   *   <li>scalars are serialized as a single number;
   *   <li>vectors are serialized as a single array;
   *   <li>matrices are serialized as an array of arrays;
   *   <li>etc.
   * </ul>
   *
   * <p>All empty tensors serialize to nested "[...]"; so all degenerate tensors (empty tensors with
   * non-zero shapes) are serialized as empty tensors.
   */
  static final class Serialization {
    /** Private constructor to prevent instantiation. */
    private Serialization() {}

    static final class Serializer extends StdSerializer<ZTensor> {
      public Serializer() {
        super(ZTensor.class);
      }

      @Override
      @SuppressWarnings({"Convert2Lambda", "Anonymous2MethodRef"})
      public void serialize(
          @Nonnull ZTensor value,
          @Nonnull JsonGenerator gen,
          @Nonnull SerializerProvider serializers) {

        value.toTree(
            new Runnable() {
              @Override
              @SneakyThrows
              public void run() {
                gen.writeStartArray();
              }
            },
            new Runnable() {
              @Override
              @SneakyThrows
              public void run() {
                gen.writeEndArray();
              }
            },
            () -> {},
            new Consumer<>() {
              @Override
              @SneakyThrows
              public void accept(Integer val) {
                gen.writeNumber(val);
              }
            });
      }
    }

    static final class Deserializer extends StdDeserializer<ZTensor> {
      public Deserializer() {
        super(ZTensor.class);
      }

      @Override
      public ZTensor deserialize(@Nonnull JsonParser p, @Nonnull DeserializationContext context)
          throws java.io.IOException {

        return fromTree(
            p.readValueAsTree(),
            TreeNode::isArray,
            TreeNode::size,
            TreeNode::get,
            node -> ((IntNode) node).intValue(),
            node -> {
              int[] chunk = new int[node.size()];
              Iterator<JsonNode> it = ((ArrayNode) node).elements();
              for (int i = 0; i < chunk.length; ++i) {
                chunk[i] = it.next().intValue();
              }
              return chunk;
            });
      }
    }
  }
}
