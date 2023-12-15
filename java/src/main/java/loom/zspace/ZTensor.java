package loom.zspace;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.common.primitives.Ints;
import com.google.errorprone.annotations.CheckReturnValue;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import loom.common.HasToJsonString;
import loom.common.IteratorUtils;
import loom.common.serialization.JsonUtil;

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
@JsonSerialize(using = ZTensor.JsonSupport.Serializer.class)
@JsonDeserialize(using = ZTensor.JsonSupport.Deserializer.class)
public final class ZTensor implements Cloneable, HasSize, HasPermute<ZTensor>, HasToJsonString {

  @Getter private final boolean mutable;
  private Integer hash;
  @Nonnull private final int[] shape;
  @Getter private final int size;
  @Nonnull private final int[] stride;
  private final int data_offset;
  @Nonnull private final int[] data;

  /**
   * Constructs a ZTensor from parts; takes ownership of the arrays.
   *
   * @param mutable whether the ZTensor is mutable.
   * @param shape the shape.
   * @param stride the strides.
   * @param data the data.
   * @param data_offset the offset in the source data.
   */
  private ZTensor(
      boolean mutable,
      @Nonnull int[] shape,
      @Nonnull int[] stride,
      @Nonnull int[] data,
      int data_offset) {
    this.shape = shape;
    this.size = IndexingFns.shapeToSize(shape);
    this.stride = stride;
    this.data_offset = data_offset;
    this.data = data;

    this.mutable = mutable;
    // Hash is lazy, and only well-defined for immutable ZTensors.
    this.hash = null;
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
  private ZTensor(
      boolean mutable, @Nonnull int[] shape, @Nonnull int[] stride, @Nonnull int[] data) {
    this(mutable, shape, stride, data, 0);
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
  private ZTensor(boolean mutable, @Nonnull int[] shape, @Nonnull int[] data) {
    this(mutable, shape, IndexingFns.shapeToLSFStrides(shape), data);
  }

  /**
   * Construct a 0-filled ZTensor of the given shape; takes ownership of the shape.
   *
   * @param mutable whether the ZTensor is mutable.
   * @param shape the shape.
   */
  private ZTensor(boolean mutable, @Nonnull int[] shape) {
    this(mutable, shape, new int[IndexingFns.shapeToSize(shape)]);
  }

  /**
   * Construct a new mutable scalar (0-dim) tensor.
   *
   * @param value the scalar value.
   * @return the new tensor.
   */
  public static @Nonnull ZTensor newScalar(int value) {
    return new ZTensor(true, new int[] {}, new int[] {value});
  }

  /**
   * Construct a new mutable vector (1-dim) tensor.
   *
   * @param values the vector values.
   * @return the new tensor.
   */
  public static @Nonnull ZTensor newVector(@Nonnull int... values) {
    return fromArray(values);
  }

  /**
   * Construct a new mutable vector (1-dim) tensor.
   *
   * @param values the vector values.
   * @return the new tensor.
   */
  public static @Nonnull ZTensor newVector(@Nonnull Iterable<Integer> values) {
    return newVector(IteratorUtils.iterableToStream(values).mapToInt(Integer::intValue).toArray());
  }

  /**
   * Construct an iota tensor [0, .., size-1].
   *
   * @param size the size of the tensor.
   * @return the new tensor.
   */
  public static @Nonnull ZTensor newIota(int size) {
    return newVector(IndexingFns.iota(size));
  }

  /**
   * Construct a new mutable matrix tensor (2-dim) from the given values.
   *
   * @param rows the {@code int[][]} values.
   * @return the new tensor.
   */
  public static @Nonnull ZTensor newMatrix(@Nonnull int[]... rows) {
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
  public static @Nonnull ZTensor newZeros(@Nonnull int... shape) {
    return new ZTensor(true, shape.clone());
  }

  /**
   * Construct a new mutable ZTensor filled with zeros with a shape like the given ZTensor.
   *
   * @param ref the ZTensor to copy the shape from.
   * @return a new mutable ZTensor.
   */
  public static @Nonnull ZTensor newZerosLike(@Nonnull ZTensor ref) {
    return new ZTensor(true, ref.shapeAsArray());
  }

  /**
   * Construct a new mutable tensor filled with the given fill value.
   *
   * @param shape the shape of the tensor.
   * @param fill_value the value to fill the tensor with.
   * @return a new mutable ZTensor.
   */
  public static @Nonnull ZTensor newFilled(@Nonnull int[] shape, int fill_value) {
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
  public static @Nonnull ZTensor newFilledLike(@Nonnull ZTensor ref, int fill_value) {
    return newFilled(ref.shape, fill_value);
  }

  /**
   * Construct a new mutable tensor filled with ones.
   *
   * @param shape the shape of the tensor.
   * @return a new mutable ZTensor.
   */
  public static @Nonnull ZTensor newOnes(@Nonnull int... shape) {
    return newFilled(shape, 1);
  }

  /**
   * Construct a new mutable ZTensor filled with ones with a shape like the given ZTensor.
   *
   * @param ref the ZTensor to copy the shape from.
   * @return a new mutable ZTensor.
   */
  public static @Nonnull ZTensor newOnesLike(@Nonnull ZTensor ref) {
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
  public static @Nonnull ZTensor fromArray(@Nonnull Object source) {
    return fromTree(
        source,
        obj -> obj.getClass().isArray(),
        Array::getLength,
        Array::get,
        obj -> (int) obj,
        obj -> (int[]) obj);
  }

  /**
   * Parse a ZTensor from a string.
   *
   * @param str the string.
   * @return the new tensor.
   * @throws IllegalArgumentException if the string is not a valid ZTensor.
   */
  public static @Nonnull ZTensor parse(@Nonnull String str) {
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

    for (int[] coords : tensor.byCoords(CoordsBufferMode.REUSED)) {
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
   * Is this ZTensor read-only / immutable?
   *
   * @return true if read-only / immutable; false otherwise.
   */
  public boolean isReadOnly() {
    return !mutable;
  }

  /** Asserts that this ZTensor is mutable. */
  public void assertMutable() {
    if (!mutable) {
      throw new IllegalStateException("ZTensor is immutable");
    }
  }

  /** Asserts that this ZTensor is read-only / immutable. */
  public void assertReadOnly() {
    if (mutable) {
      throw new IllegalStateException("ZTensor is mutable");
    }
  }

  /**
   * Return an immutable ZTensor with the same data.
   *
   * <p>If this ZTensor is already immutable, returns this; otherwise, returns an immutable clone.
   *
   * <p>Semantically equivalent to {@code clone(false)}.
   *
   * <p>A performance oriented Tensor library would track open mutable views of the underlying data,
   * and perform copy-on-write when necessary; as this is a correctness-oriented Tensor library, we
   * simply clone the data to go from mutable to immutable.
   *
   * @return an immutable ZTensor.
   */
  @Nonnull
  public ZTensor asImmutable() {
    return clone(false);
  }

  /**
   * Return if this tensor is compact.
   *
   * <p>A tensor is compact if its data array is exactly the size of the tensor.
   *
   * @return true if this tensor is compact.
   */
  public boolean isCompact() {
    return data.length == size;
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

    for (int[] coords : byCoords(CoordsBufferMode.REUSED)) {
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

    for (var coords : byCoords(CoordsBufferMode.REUSED)) {
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
      hash = reduceCellsAsInt((a, b) -> 31 * a + b, Arrays.hashCode(shape));
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
    for (int[] coords : byCoords(CoordsBufferMode.REUSED)) {
      var it = arr;
      for (int d = 0; d < ndim - 1; ++d) {
        it = Array.get(it, coords[d]);
      }
      int[] chunk = (int[]) it;
      chunk[coords[ndim - 1]] = get(coords);
    }

    return arr;
  }

  /**
   * Assert that this tensor has the given number of dimensions.
   *
   * @param ndim the number of dimensions.
   */
  @Override
  public void assertNDim(int ndim) {
    HasDimension.assertNDim(ndim, getNDim());
  }

  /**
   * Assert that this tensor has the given shape.
   *
   * @param shape the shape.
   */
  public void assertShape(@Nonnull int... shape) {
    IndexingFns.assertShape(this.shape, shape);
  }

  /**
   * Assert that this tensor has the same shape as another.
   *
   * @param other the other tensor.
   */
  public void assertMatchingShape(@Nonnull ZTensor other) {
    IndexingFns.assertShape(shape, other.shape);
  }

  /**
   * Clone this ZTensor.
   *
   * <p>If this ZTensor is immutable and compact, returns this.
   *
   * <p>If this ZTensor is immutable and non-compact, returns a compact clone.
   *
   * <p>If this ZTensor is mutable, returns a compact mutable clone.
   *
   * @return a ZTensor with the same data.
   */
  @Override
  @SuppressWarnings("MethodDoesntCallSuperMethod")
  public ZTensor clone() {
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
  public ZTensor clone(boolean mutable) {
    if (isReadOnly() && isCompact() && !mutable) {
      return this;
    }

    var res = new ZTensor(true, shape);
    forEachEntry(res::set, CoordsBufferMode.REUSED);
    if (!mutable) {
      return new ZTensor(false, res.shape, res.stride, res.data, 0);
    }
    return res;
  }

  /**
   * Get the shape of this tensor along a given dimension.
   *
   * @param dim the dimension; supports negative indexing.
   * @return the size of the dimension.
   */
  public int shape(int dim) {
    return shape[resolveDim(dim)];
  }

  /**
   * Returns the shape of this tensor.
   *
   * @return a copy of the shape array.
   */
  @Nonnull
  public int[] shapeAsArray() {
    return shape.clone();
  }

  /**
   * Returns the shape of this tensor as a list.
   *
   * @return an immutable shape list.
   */
  @Nonnull
  public List<Integer> shapeAsList() {
    return Collections.unmodifiableList(Ints.asList(shape));
  }

  /**
   * Returns the shape of this tensor as a ZTensor.
   *
   * @return the shape of this tensor as a ZTensor.
   */
  @Nonnull
  public ZTensor shapeAsTensor() {
    return ZTensor.newVector(shape);
  }

  @Override
  public int getNDim() {
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
  public int resolveDim(int dim) {
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
  public int[] resolveDims(int... dims) {
    return IndexingFns.resolveDims(dims, shape);
  }

  /** Returns the number of elements in this tensor. */
  @Override
  public int size() {
    return size;
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
    return byCoords(CoordsBufferMode.REUSED).stream().allMatch(c -> predicate.test(get(c)));
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
    return byCoords(CoordsBufferMode.REUSED).stream().anyMatch(c -> predicate.test(get(c)));
  }

  /**
   * Iterate over the coordinates and values of this tensor.
   *
   * <p>When the buffer mode is {@link CoordsBufferMode#REUSED}, the buffer is shared between
   * subsequent calls to {@link TensorEntryConsumer#accept(int[], int)}. When the buffer mode is
   * {@link CoordsBufferMode#SAFE}, the buffer is not shared between subsequent calls to {@link
   * TensorEntryConsumer#accept(int[], int)}.
   *
   * @param consumer the consumer.
   * @param bufferMode the buffer mode.
   */
  public void forEachEntry(
      @Nonnull TensorEntryConsumer consumer, @Nonnull CoordsBufferMode bufferMode) {
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
    for (int[] coords : byCoords(CoordsBufferMode.REUSED)) {
      consumer.accept(get(coords));
    }
  }

  /**
   * Returns an {@code Iterable<int[]>} over the coordinates of this tensor.
   *
   * <p>When the buffer mode is {@link CoordsBufferMode#REUSED}, the buffer is shared between
   * subsequent calls to {@link Iterator#next()}. When the buffer mode is {@link
   * CoordsBufferMode#SAFE}, the buffer is not shared between subsequent calls to {@link
   * Iterator#next()}.
   *
   * <p>Empty tensors will return an empty iterable.
   *
   * <p>Scalar tensors will return an iterable with a single empty coordinate array.
   *
   * @param bufferMode the buffer mode.
   * @return an iterable over the coordinates of this tensor.
   */
  public @Nonnull IterableCoords byCoords(@Nonnull CoordsBufferMode bufferMode) {
    return new IterableCoords(bufferMode);
  }

  @Override
  public ZTensor permute(@Nonnull int... permutation) {
    var perm = IndexingFns.resolvePermutation(permutation, getNDim());
    var newShape = IndexingFns.applyResolvedPermutation(shape, perm);
    var newStride = IndexingFns.applyResolvedPermutation(stride, perm);

    return new ZTensor(mutable, newShape, newStride, data, data_offset);
  }

  /**
   * Creates a reordered view of this tensor along a specified dimension.
   *
   * <p>Being a view, mutations in the new tensor affect the original tensor and vice versa.
   *
   * <p><b>Example:</b> Suppose we have tensor 't' with shape [2,3]:
   *
   * <pre>
   * t = [[0, 1, 2],
   *      [3, 4, 5]]
   * </pre>
   *
   * If we call {@code t.reorderDim([1,0,2], 1)}, the returned tensor will look like:
   *
   * <pre>
   * v = [[1, 0, 2],
   *      [4, 3, 5]]
   * </pre>
   *
   * <p>Supports negative dimension indexing - i.e. -1 represents the last dimension, -2 represents
   * the second last, and so on.
   *
   * @param permutation An array of unique integers representing the new order of indices along the
   *     specified dimension. Each integer should be a valid index for that dimension.
   * @param dim Index of the dimension to be reordered. Dimensions are zero-indexed. This must be a
   *     valid dimension of this tensor.
   * @return A new ZTensor, a "view" of the original tensor, with the specified dimension reordered.
   *     This view shares data with the original tensor.
   */
  @Nonnull
  public ZTensor reorderDim(@Nonnull int[] permutation, int dim) {
    var d = resolveDim(dim);
    var perm = IndexingFns.resolvePermutation(permutation, shape[d]);
    var res = new ZTensor(true, shape);
    for (int i = 0; i < shape[d]; ++i) {
      res.selectDim(d, i).assign(this.selectDim(d, perm[i]));
    }
    return res;
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
  public ZTensor transpose(int a, int b) {
    int rA = resolveDim(a);
    int rB = resolveDim(b);
    if (rA == rB) {
      return this;
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
  public ZTensor transpose() {
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
  public ZTensor T() {
    return transpose();
  }

  /**
   * Returns a view of this tensor with the given dimension reversed.
   *
   * @param d the dimension to reverse, accepts negative indices.
   * @return a view of this tensor with the given dimension reversed.
   */
  @Nonnull
  public ZTensor reverse(int d) {
    int rD = resolveDim(d);

    int[] newStride = stride.clone();
    newStride[rD] *= -1;

    int newOffset = data_offset + (shape[rD] - 1) * stride[rD];

    return new ZTensor(mutable, shape, newStride, data, newOffset);
  }

  /**
   * Create a view of this tensor with an extra dimension added at index `d`.
   *
   * @param d the dimension to add.
   * @return a view of this tensor with an extra dimension added at index `d`.
   */
  @Nonnull
  public ZTensor unsqueeze(int d) {
    int rD = IndexingFns.resolveDim(d, shape.length + 1);

    int[] newShape = new int[getNDim() + 1];
    int[] newStride = new int[getNDim() + 1];

    System.arraycopy(shape, 0, newShape, 0, rD);
    System.arraycopy(shape, rD, newShape, rD + 1, getNDim() - rD);

    System.arraycopy(stride, 0, newStride, 0, rD);
    System.arraycopy(stride, rD, newStride, rD + 1, getNDim() - rD);

    newShape[rD] = 1;
    newStride[rD] = 0;

    return new ZTensor(mutable, newShape, newStride, data, data_offset);
  }

  /**
   * Returns a view of this tensor with a dimensions of size 1 removed.
   *
   * @param d the dimension to remove; accepts negative indices.
   * @return a view of this tensor with a dimensions of size 1 removed.
   */
  @Nonnull
  public ZTensor squeeze(int d) {
    int rD = resolveDim(d);

    if (stride[rD] != 0) {
      throw new IllegalArgumentException(
          "dimension " + rD + ", shape " + shape[rD] + " is not squeezable");
    }

    return new ZTensor(
        mutable,
        IndexingFns.removeIdx(shape, rD),
        IndexingFns.removeIdx(stride, rD),
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
  public ZTensor broadcastDim(int dim, int size) {
    dim = resolveDim(dim);
    if (stride[dim] != 0) {
      throw new IllegalArgumentException(
          "Cannot broadcast dimension %d with real-size %d".formatted(dim, shape[dim]));
    }

    var new_shape = shape.clone();
    new_shape[dim] = size;

    var new_stride = stride.clone();
    new_stride[dim] = 0;

    return new ZTensor(mutable, new_shape, new_stride, data, data_offset);
  }

  /**
   * Is this dimension broadcasted (i.e. has stride 0 but a shape > 1)?
   *
   * @param dim the dimension to check; supports negative indices.
   * @return true if the dimension is broadcasted.
   */
  @JsonIgnore
  public boolean isBroadcastDim(int dim) {
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
  public ZTensor broadcastLike(@Nonnull ZTensor ref) {
    return broadcastTo(ref.shape);
  }

  /**
   * Return a view of this tensor broadcasted to the given shape.
   *
   * @param targetShape the target shape.
   * @return a broadcasted view of this tensor.
   */
  @Nonnull
  public ZTensor broadcastTo(@Nonnull int... targetShape) {
    if (Arrays.equals(shape, targetShape)) {
      return this;
    }

    if (isScalar() && IndexingFns.shapeToSize(targetShape) == 0) {
      return new ZTensor(mutable, targetShape);
    }

    var res = this;
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
   * Compute the ravel index into the data array for the given coordinates.
   *
   * @param coords the coordinates.
   * @return the ravel index.
   */
  private int ravel(@Nonnull int... coords) {
    return data_offset + IndexingFns.ravel(shape, stride, coords);
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
   * Return a view of this tensor with the given dimension selected.
   *
   * @param dim the dimension to select.
   * @param index the index to select.
   * @return a view of this tensor with the given dimension selected.
   */
  @Nonnull
  public ZTensor selectDim(int dim, int index) {
    var d = resolveDim(dim);
    var i = IndexingFns.resolveIndex("index", index, shape[d]);

    var new_shape = shape.clone();
    new_shape[d] = 1;
    var new_stride = stride.clone();
    new_stride[d] = 0;
    int new_offset = data_offset + i * stride[d];

    return new ZTensor(mutable, new_shape, new_stride, data, new_offset).squeeze(d);
  }

  /**
   * Return a view of this tensor with the given dimension selected.
   *
   * @param dims the dimensions to select.
   * @param indexes the matching indexes to select.
   * @return a view of this tensor with the given dimensions selected.
   */
  @Nonnull
  public ZTensor selectDims(@Nonnull int[] dims, @Nonnull int[] indexes) {
    if (dims.length != indexes.length) {
      throw new IllegalArgumentException(
          "dims.length (%d) != indexes.length (%d)".formatted(dims.length, indexes.length));
    }

    var ds = resolveDims(dims);
    var is = new int[indexes.length];
    for (int i = 0; i < indexes.length; ++i) {
      is[i] = IndexingFns.resolveIndex("index", indexes[i], shape[ds[i]]);
    }

    var res = this;
    for (int i = 0; i < ds.length; ++i) {
      res = res.selectDim(ds[i] - i, is[i]);
    }
    return res;
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
  public @Nonnull int[] toT1() {
    assertNDim(1);
    return (int[]) toArray();
  }

  /** Convert this structure to a T2 (a matrix) value. Assert that the shape is valid. */
  public @Nonnull int[][] toT2() {
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
    for (int[] coords : byCoords(CoordsBufferMode.REUSED)) {
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
    tensor.broadcastLike(this).forEachEntry(this::_unchecked_set, CoordsBufferMode.REUSED);
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
   * Assign from an element-wise unary operation.
   *
   * @param op the operation.
   * @param tensor the input tensor.
   */
  public void zipWith_(@Nonnull IntUnaryOperator op, @Nonnull ZTensor tensor) {
    assertMutable();
    tensor
        .broadcastLike(this)
        .forEachEntry(
            (coords, value) -> _unchecked_set(coords, op.applyAsInt(value)),
            CoordsBufferMode.REUSED);
  }

  /**
   * Assign from an element-wise binary operation.
   *
   * @param op the operation.
   * @param lhs the left-hand side tensor.
   * @param rhs the right-hand side tensor.
   */
  public void zipWith_(@Nonnull IntBinaryOperator op, @Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
    assertMutable();
    lhs = lhs.broadcastLike(this);
    rhs = rhs.broadcastLike(this);
    for (int[] coords : byCoords(CoordsBufferMode.REUSED)) {
      _unchecked_set(coords, op.applyAsInt(lhs.get(coords), rhs.get(coords)));
    }
  }

  /**
   * An in-place element-wise binary operation.
   *
   * @param op the operation.
   * @param rhs the right-hand side tensor.
   */
  public void zipWith_(@Nonnull IntBinaryOperator op, @Nonnull ZTensor rhs) {
    zipWith_(op, this, rhs);
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
  public @Nonnull ZTensor sum() {
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
  public @Nonnull ZTensor sum(@Nonnull int... dims) {
    return Ops.sum(this, dims);
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
  public @Nonnull ZTensor neg() {
    return Ops.neg(this);
  }

  /** Returns a new elementwise absolute value of this tensor. */
  public @Nonnull ZTensor abs() {
    return Ops.abs(this);
  }

  /** Returns an elementwise broadcast addition with this tensor. */
  public @Nonnull ZTensor add(@Nonnull ZTensor rhs) {
    return Ops.add(this, rhs);
  }

  /** Returns an elementwise broadcast addition with this tensor. */
  public @Nonnull ZTensor add(int rhs) {
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
  public @Nonnull ZTensor sub(@Nonnull ZTensor rhs) {
    return Ops.sub(this, rhs);
  }

  /**
   * Returns an elementwise broadcast subtraction with this tensor.
   *
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  public @Nonnull ZTensor sub(int rhs) {
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
  public @Nonnull ZTensor mul(@Nonnull ZTensor rhs) {
    return Ops.mul(this, rhs);
  }

  /**
   * Returns an elementwise broadcast multiplication with this tensor.
   *
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  public @Nonnull ZTensor mul(int rhs) {
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
  public @Nonnull ZTensor div(@Nonnull ZTensor rhs) {
    return Ops.div(this, rhs);
  }

  /**
   * Returns an elementwise broadcast division with this tensor.
   *
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  public @Nonnull ZTensor div(int rhs) {
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
  public @Nonnull ZTensor mod(@Nonnull ZTensor rhs) {
    return Ops.mod(this, rhs);
  }

  /**
   * Returns an elementwise broadcast mod with this tensor.
   *
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  public @Nonnull ZTensor mod(int rhs) {
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
     * An element-wise unary operation.
     *
     * @param op the operation.
     * @param tensor the input tensor.
     * @return a new tensor.
     */
    @CheckReturnValue
    public static @Nonnull ZTensor map(@Nonnull IntUnaryOperator op, @Nonnull ZTensor tensor) {
      var result = newZerosLike(tensor);
      result.zipWith_(op, tensor);
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
    public static @Nonnull ZTensor zipWith(
        @Nonnull IntBinaryOperator op, @Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
      var result = newZeros(IndexingFns.commonBroadcastShape(lhs.shape, rhs.shape));
      result.zipWith_(op, lhs, rhs);
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
    public static @Nonnull ZTensor zipWith(
        @Nonnull IntBinaryOperator op, @Nonnull ZTensor lhs, int rhs) {
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
    public static @Nonnull ZTensor zipWith(
        @Nonnull IntBinaryOperator op, int lhs, @Nonnull ZTensor rhs) {
      return zipWith(op, loom.zspace.ZTensor.newScalar(lhs), rhs);
    }

    /**
     * Elementwise negation of a tensor.
     *
     * @param tensor the input tensor.
     * @return a new tensor.
     */
    public static @Nonnull ZTensor neg(@Nonnull ZTensor tensor) {
      return map(x -> -x, tensor);
    }

    /**
     * Elementwise absolute value of a tensor.
     *
     * @param tensor the input tensor.
     * @return a new tensor.
     */
    public static @Nonnull ZTensor abs(@Nonnull ZTensor tensor) {
      return map(Math::abs, tensor);
    }

    /**
     * Element-wise broadcast minimum.
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return a new tensor.
     */
    public static @Nonnull ZTensor minimum(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
      return zipWith(Math::min, lhs, rhs);
    }

    /**
     * Element-wise broadcast minimum.
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return a new tensor.
     */
    public static @Nonnull ZTensor minimum(@Nonnull ZTensor lhs, int rhs) {
      return zipWith(Math::min, lhs, rhs);
    }

    /**
     * Element-wise broadcast minimum.
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return a new tensor.
     */
    public static @Nonnull ZTensor minimum(int lhs, @Nonnull ZTensor rhs) {
      return zipWith(Math::min, lhs, rhs);
    }

    /**
     * Element-wise broadcast maximum.
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return a new tensor.
     */
    public static @Nonnull ZTensor maximum(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
      return zipWith(Math::max, lhs, rhs);
    }

    /**
     * Element-wise broadcast maximum.
     *
     * @param lhs the left-hand side tensor.
     * @param rhs the right-hand side scalar.
     * @return a new tensor.
     */
    public static @Nonnull ZTensor maximum(@Nonnull ZTensor lhs, int rhs) {
      return zipWith(Math::max, lhs, rhs);
    }

    /**
     * Element-wise broadcast maximum.
     *
     * @param lhs the left-hand side tensor.
     * @param rhs the right-hand side scalar.
     * @return a new tensor.
     */
    public static @Nonnull ZTensor maximum(int lhs, @Nonnull ZTensor rhs) {
      return zipWith(Math::max, lhs, rhs);
    }

    /**
     * Element-wise broadcast addition.
     *
     * @param lhs the left-hand side tensor.
     * @param rhs the right-hand side scalar.
     * @return a new tensor.
     */
    public static @Nonnull ZTensor add(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
      return zipWith(Integer::sum, lhs, rhs);
    }

    /**
     * Element-wise broadcast addition.
     *
     * @param lhs the left-hand side tensor.
     * @param rhs the right-hand side scalar.
     * @return a new tensor.
     */
    public static @Nonnull ZTensor add(@Nonnull ZTensor lhs, int rhs) {
      return zipWith(Integer::sum, lhs, rhs);
    }

    /**
     * Element-wise broadcast addition.
     *
     * @param lhs the left-hand side scalar.
     * @param rhs the right-hand side tensor.
     * @return a new tensor.
     */
    public static @Nonnull ZTensor add(int lhs, @Nonnull ZTensor rhs) {
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
    public static @Nonnull ZTensor sub(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
      return zipWith((l, r) -> l - r, lhs, rhs);
    }

    /**
     * Element-wise broadcast subtraction.
     *
     * @param lhs the left-hand side scalar.
     * @param rhs the right-hand side tensor.
     * @return a new tensor.
     */
    public static @Nonnull ZTensor sub(@Nonnull ZTensor lhs, int rhs) {
      return zipWith((l, r) -> l - r, lhs, rhs);
    }

    /**
     * Element-wise broadcast subtraction.
     *
     * @param lhs the left-hand side scalar.
     * @param rhs the right-hand side tensor.
     * @return a new tensor.
     */
    public static @Nonnull ZTensor sub(int lhs, @Nonnull ZTensor rhs) {
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
    public static @Nonnull ZTensor mul(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
      return zipWith((l, r) -> l * r, lhs, rhs);
    }

    /**
     * Element-wise broadcast multiplication.
     *
     * @param lhs the left-hand side scalar.
     * @param rhs the right-hand side tensor.
     * @return a new tensor.
     */
    public static @Nonnull ZTensor mul(@Nonnull ZTensor lhs, int rhs) {
      return zipWith((l, r) -> l * r, lhs, rhs);
    }

    /**
     * Element-wise broadcast multiplication.
     *
     * @param lhs the left-hand side scalar.
     * @param rhs the right-hand side tensor.
     * @return a new tensor.
     */
    public static @Nonnull ZTensor mul(int lhs, @Nonnull ZTensor rhs) {
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
    public static @Nonnull ZTensor div(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
      return zipWith((l, r) -> l / r, lhs, rhs);
    }

    /**
     * Element-wise broadcast division.
     *
     * @param lhs the left-hand side scalar.
     * @param rhs the right-hand side tensor.
     * @return a new tensor.
     */
    public static @Nonnull ZTensor div(@Nonnull ZTensor lhs, int rhs) {
      return zipWith((l, r) -> l / r, lhs, rhs);
    }

    /**
     * Element-wise broadcast division.
     *
     * @param lhs the left-hand side scalar.
     * @param rhs the right-hand side tensor.
     * @return a new tensor.
     */
    public static @Nonnull ZTensor div(int lhs, @Nonnull ZTensor rhs) {
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
    public static @Nonnull ZTensor mod(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
      return zipWith((l, r) -> l % r, lhs, rhs);
    }

    /**
     * Element-wise broadcast mod.
     *
     * @param lhs the left-hand side scalar.
     * @param rhs the right-hand side tensor.
     * @return a new tensor.
     */
    public static @Nonnull ZTensor mod(@Nonnull ZTensor lhs, int rhs) {
      return zipWith((l, r) -> l % r, lhs, rhs);
    }

    /**
     * Element-wise broadcast mod.
     *
     * @param lhs the left-hand side scalar.
     * @param rhs the right-hand side tensor.
     * @return a new tensor.
     */
    public static @Nonnull ZTensor mod(int lhs, @Nonnull ZTensor rhs) {
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
    public static @Nonnull ZTensor reduceCells(
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
    public static @Nonnull ZTensor reduceCells(
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
      for (var ks : acc.byCoords(CoordsBufferMode.REUSED)) {
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
    public static @Nonnull ZTensor sum(@Nonnull ZTensor tensor) {
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
    public static @Nonnull ZTensor sum(@Nonnull ZTensor tensor, @Nonnull int... dims) {
      return reduceCells(tensor, Integer::sum, 0, dims);
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
     * @return the scalar ZTensor sum of all elements in the tensor.
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
     * @return the scalar ZTensor sum of all elements in the tensor.
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
   * Jackson Serialization Support namespace.
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
  static final class JsonSupport {
    /** Private constructor to prevent instantiation. */
    private JsonSupport() {}

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
      public ZTensor deserialize(@Nonnull JsonParser p, @Nonnull DeserializationContext ctxt)
          throws java.io.IOException {

        return fromTree(
            p.readValueAsTree(),
            TreeNode::isArray,
            TreeNode::size,
            TreeNode::get,
            node -> ((IntNode) node).intValue(),
            node -> {
              int[] chunk = new int[node.size()];
              for (int i = 0; i < chunk.length; ++i) {
                chunk[i] = ((IntNode) node.get(i)).intValue();
              }
              return chunk;
            });
      }
    }
  }

  /** An Iterable view of the coordinates of this tensor. */
  @Data
  public final class IterableCoords implements Iterable<int[]> {
    private final CoordsBufferMode bufferMode;

    @Override
    public @Nonnull CoordsIterator iterator() {
      return new CoordsIterator(bufferMode);
    }

    public @Nonnull Stream<int[]> stream() {
      return IteratorUtils.iterableToStream(this);
    }
  }

  /**
   * An Iterator over the coordinates of this tensor.
   *
   * <p>When the buffer mode is {@link CoordsBufferMode#REUSED}, the buffer is shared between
   * subsequent calls to {@link Iterator#next()}. When the buffer mode is {@link
   * CoordsBufferMode#SAFE}, the buffer is not shared between subsequent calls to {@link
   * Iterator#next()}.
   */
  @RequiredArgsConstructor
  public final class CoordsIterator implements Iterator<int[]> {
    @Getter private final CoordsBufferMode bufferMode;

    // Assuming a non-scalar ZTensor; non-empty ZTensor.
    private int remaining = size();
    @Nullable private int[] coords = null;

    @Override
    public boolean hasNext() {
      return remaining > 0;
    }

    @Override
    @Nonnull
    public int[] next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      remaining--;

      if (coords == null) {
        coords = new int[getNDim()];
      } else {
        coords[coords.length - 1]++;
        for (int i = coords.length - 1; i >= 0; --i) {
          if (coords[i] == shape[i]) {
            coords[i] = 0;
            coords[i - 1]++;
          }
        }
      }

      if (bufferMode == CoordsBufferMode.SAFE) {
        return coords.clone();
      }

      return coords;
    }
  }
}
