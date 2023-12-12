package loom.zspace;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.IntNode;
import com.google.common.primitives.Ints;
import com.google.errorprone.annotations.CheckReturnValue;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.SneakyThrows;
import loom.common.HasToJsonString;
import loom.common.IteratorUtils;
import loom.common.serialization.JsonUtil;

/**
 * Minimal discrete Tensor.
 *
 * <p>As this tensor exists solely to support discrete space range calculations, C/C++/JNI/CUDA/BLAS
 * accelerations are not needed; the focus is on what permits the block expression index projection
 * math to be as readable as possible; targeting succinctness with range and shape assertions.
 *
 * <p>The ZTensor serialization format is a JSON array of integers; and degenerate ZTensors (those
 * with a mix of 0-sized and non-0-sized dimensions) will serialize down to the minimal empty tensor
 * in the given dimension.
 */
@JsonSerialize(using = ZTensor.JsonSupport.Serializer.class)
@JsonDeserialize(using = ZTensor.JsonSupport.Deserializer.class)
public final class ZTensor implements Cloneable, HasSize, HasPermute<ZTensor>, HasToJsonString {

  /**
   * An Iterable view of the coordinates of this tensor.
   *
   * <p>The {@link #bufferMode} is passed to each construction of an {@link Iterator}.
   */
  @Getter
  public final class IterableCoords implements Iterable<int[]> {
    private final CoordsBufferMode bufferMode;

    IterableCoords(CoordsBufferMode bufferMode) {
      this.bufferMode = bufferMode;
    }

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
   * CoordsBufferMode#DISTINCT}, the buffer is not shared between subsequent calls to {@link
   * Iterator#next()}.
   */
  public final class CoordsIterator implements Iterator<int[]> {
    @Getter private final CoordsBufferMode bufferMode;

    // Assuming a non-scalar ZTensor; non-empty ZTensor.
    private int remaining = size();
    @Nullable private int[] coords = null;

    CoordsIterator(CoordsBufferMode bufferMode) {
      this.bufferMode = bufferMode;
    }

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
        coords = new int[ndim()];
      } else {
        coords[coords.length - 1]++;
        for (int i = coords.length - 1; i >= 0; --i) {
          if (coords[i] == shape[i]) {
            coords[i] = 0;
            coords[i - 1]++;
          }
        }
      }

      if (bufferMode == CoordsBufferMode.DISTINCT) {
        return coords.clone();
      }

      return coords;
    }
  }

  /**
   * Construct a new mutable scalar (0-dim) tensor.
   *
   * @param value the scalar value.
   * @return the new tensor.
   */
  public static @Nonnull ZTensor scalar(int value) {
    return new ZTensor(true, new int[] {}, new int[] {value});
  }

  /**
   * Construct a new mutable vector (1-dim) tensor.
   *
   * @param values the vector values.
   * @return the new tensor.
   */
  public static @Nonnull ZTensor vector(@Nonnull int... values) {
    return from(values);
  }

  /**
   * Construct a new mutable vector (1-dim) tensor.
   *
   * @param values the vector values.
   * @return the new tensor.
   */
  public static @Nonnull ZTensor vector(@Nonnull Iterable<Integer> values) {
    return vector(IteratorUtils.iterableToStream(values).mapToInt(Integer::intValue).toArray());
  }

  /**
   * Construct a new mutable matrix tensor (2-dim) from the given values.
   *
   * @param rows the {@code int[][]} values.
   * @return the new tensor.
   */
  public static @Nonnull ZTensor matrix(@Nonnull int[]... rows) {
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
  public static @Nonnull ZTensor zeros(@Nonnull int... shape) {
    return new ZTensor(true, shape.clone());
  }

  /**
   * Construct a new mutable ZTensor filled with zeros with a shape like the given ZTensor.
   *
   * @param ref the ZTensor to copy the shape from.
   * @return a new mutable ZTensor.
   */
  public static @Nonnull ZTensor zeros_like(@Nonnull ZTensor ref) {
    return new ZTensor(true, ref.shapeAsArray());
  }

  /**
   * Construct a new mutable tensor filled with the given fill value.
   *
   * @param shape the shape of the tensor.
   * @param fill_value the value to fill the tensor with.
   * @return a new mutable ZTensor.
   */
  public static @Nonnull ZTensor full(@Nonnull int[] shape, int fill_value) {
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
  public static @Nonnull ZTensor full_like(@Nonnull ZTensor ref, int fill_value) {
    return full(ref.shape, fill_value);
  }

  /**
   * Construct a new mutable tensor filled with ones.
   *
   * @param shape the shape of the tensor.
   * @return a new mutable ZTensor.
   */
  public static @Nonnull ZTensor ones(@Nonnull int... shape) {
    return full(shape, 1);
  }

  /**
   * Construct a new mutable ZTensor filled with ones with a shape like the given ZTensor.
   *
   * @param ref the ZTensor to copy the shape from.
   * @return a new mutable ZTensor.
   */
  public static @Nonnull ZTensor ones_like(@Nonnull ZTensor ref) {
    return full_like(ref, 1);
  }

  /**
   * Construct a diagonal matrix from a list of values.
   *
   * @param diag the values to put on the diagonal.
   * @return a new ZTensor.
   */
  public static ZTensor diagonal(@Nonnull int... diag) {
    var tensor = zeros(diag.length, diag.length);
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
  public static ZTensor identity_matrix(int n) {
    int[] diag = new int[n];
    Arrays.fill(diag, 1);
    return diagonal(diag);
  }

  /**
   * Given a non-sparse array of unknown dimensionality, returns a ZTensor with the same shape and
   * data.
   *
   * @param source the source array.
   * @return a new ZTensor.
   */
  public static @Nonnull ZTensor from(@Nonnull Object source) {
    return fromTree(
        source,
        obj -> obj.getClass().isArray(),
        Array::getLength,
        Array::get,
        obj -> (int) obj,
        obj -> (int[]) obj);
  }

  @Getter private final boolean mutable;
  private final int hash;

  @Nonnull private final int[] shape;

  private final int size;

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
    if (mutable) {
      // Hash is only well-defined for immutable ZTensors.
      hash = -1;
    } else {
      int acc = Arrays.hashCode(this.shape);
      for (var coords : byCoords(CoordsBufferMode.REUSED)) {
        acc = 31 * acc + get(coords);
      }
      hash = acc;
    }
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
   * Construct an immutable ZPoint from this ZTensor. Asserts that this is a 1-dim tensor.
   *
   * @return a new immutable ZPoint.
   */
  public ZPoint zpoint() {
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
  public ZTensor immutable() {
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
   * Given a tree datastructure representing a tensor of unknown dimensionality, returns a ZTensor.
   *
   * @param <T> the type of the tree.
   * @param root the root of the tree.
   * @param isArray is this node an array, or a scalar?
   * @param getArrayLength get the length of this array.
   * @param getArrayElement get the ith element of this array.
   * @param scalarValue get the value of this scalar.
   * @param getChunk get a coherent chunk of data for a final layer array.
   * @return a new ZTensor.
   */
  static <T> @Nonnull ZTensor fromTree(
      T root,
      Predicate<T> isArray,
      Function<T, Integer> getArrayLength,
      BiFunction<T, Integer, T> getArrayElement,
      Function<T, Integer> scalarValue,
      Function<T, int[]> getChunk) {

    if (!isArray.test(root)) {
      return loom.zspace.ZTensor.scalar(scalarValue.apply(root));
    }

    List<Integer> shapeList = new ArrayList<>();
    {
      var it = root;
      while (isArray.test(it)) {
        var size = getArrayLength.apply(it);
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
      return loom.zspace.ZTensor.zeros(new int[ndim]);
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

      int[] chunk = getChunk.apply(it);

      System.arraycopy(chunk, 0, tensor.data, chunkCount * chunkStride, chunkStride);
      chunkCount++;
    }

    return tensor;
  }

  /**
   * Serialize this tensor to a tree datastructure.
   *
   * @param startArray start an array.
   * @param endArray end an array.
   * @param elemSep write an element separator.
   * @param writeNumber write a number.
   */
  public void toTree(
      Runnable startArray, Runnable endArray, Runnable elemSep, Consumer<Integer> writeNumber) {
    if (isScalar()) {
      writeNumber.accept(get());
      return;
    }

    int ndim = ndim();

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

    HasDimension.assertSameNDim(this, that);
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
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    toTree(() -> sb.append('['), () -> sb.append(']'), () -> sb.append(", "), sb::append);

    return sb.toString();
  }

  /**
   * Parse a ZTensor from a string.
   *
   * @param s the string.
   * @return the new tensor.
   */
  public static ZTensor parse(String s) {
    return JsonUtil.fromJson(s, ZTensor.class);
  }

  /**
   * Convert this tensor to a Java array. Scalars will be converted to a single int.
   *
   * @return the new java structure.
   */
  public Object toArray() {
    // TODO: scalars?
    if (isScalar()) {
      return item();
    }

    Object arr = Array.newInstance(int.class, shape);

    var ndim = ndim();
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
   * @param actualNdim the actual number of dimensions.
   * @param expectedNdim the expected number of dimensions.
   */
  public static void assertNdim(int actualNdim, int expectedNdim) {
    if (actualNdim != expectedNdim) {
      throw new IllegalArgumentException("expected ndim " + expectedNdim + ", got " + actualNdim);
    }
  }

  /**
   * Assert that this tensor has the given number of dimensions.
   *
   * @param ndim the number of dimensions.
   */
  public void assertNdim(int ndim) {
    assertNdim(ndim, ndim());
  }

  /**
   * Assert that this tensor has the given shape.
   *
   * @param actual the actual shape.
   * @param expected the expected shape.
   * @throws IllegalStateException if the shapes do not match.
   */
  public static void assertShape(@Nonnull int[] actual, @Nonnull int[] expected) {
    if (!Arrays.equals(actual, expected)) {
      throw new IllegalArgumentException(
          "shape " + Arrays.toString(actual) + " != expected shape " + Arrays.toString(expected));
    }
  }

  /**
   * Assert that this tensor has the given shape.
   *
   * @param shape the shape.
   */
  public void assertShape(@Nonnull int... shape) {
    assertShape(this.shape, shape);
  }

  /**
   * Assert that this tensor has the same shape as another.
   *
   * @param other the other tensor.
   */
  public void assertMatchingShape(@Nonnull ZTensor other) {
    assertMatchingShapes(this, other);
  }

  /**
   * Assert that two tensors have the same shape.
   *
   * @param lhs the left-hand side.
   * @param rhs the right-hand side.
   */
  public static void assertMatchingShapes(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
    assertShape(lhs.shape, rhs.shape);
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
  public ZTensor clone(boolean mutable) {
    if (isReadOnly() && isCompact() && !mutable) {
      return this;
    }

    var res = new ZTensor(true, shape);
    forEachItem(res::set);
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
  public int[] shapeAsArray() {
    return shape.clone();
  }

  /**
   * Returns the shape of this tensor as a list.
   *
   * @return an immutable shape list.
   */
  public List<Integer> shapeAsList() {
    return Collections.unmodifiableList(Ints.asList(shape));
  }

  /**
   * Returns the shape of this tensor as a ZTensor.
   *
   * @return the shape of this tensor as a ZTensor.
   */
  public ZTensor shapeAsTensor() {
    return ZTensor.vector(shape);
  }

  @Override
  public int ndim() {
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

  /** Returns the number of elements in this tensor. */
  @Override
  public int size() {
    return size;
  }

  /** Are all cells in this tensor > 0? */
  public boolean isStrictlyPositive() {
    for (var c : byCoords(CoordsBufferMode.REUSED)) {
      if (get(c) <= 0) {
        return false;
      }
    }
    return true;
  }

  /**
   * Iterate over the coordinates and values of this tensor.
   *
   * @param consumer the consumer.
   */
  public void forEachItem(@Nonnull BiConsumer<int[], Integer> consumer) {
    for (int[] coords : byCoords(CoordsBufferMode.REUSED)) {
      consumer.accept(coords, get(coords));
    }
  }

  /**
   * Returns an {@code Iterable<int[]>} over the coordinates of this tensor.
   *
   * <p>When the buffer mode is {@link CoordsBufferMode#REUSED}, the buffer is shared between
   * subsequent calls to {@link Iterator#next()}. When the buffer mode is {@link
   * CoordsBufferMode#DISTINCT}, the buffer is not shared between subsequent calls to {@link
   * Iterator#next()}.
   *
   * <p>Empty tensors will return an empty iterable.
   *
   * <p>Scalar tensors will return an iterable with a single empty coordinate array.
   *
   * @param bufferMode the buffer mode.
   * @return an iterable over the coordinates of this tensor.
   */
  public @Nonnull IterableCoords byCoords(CoordsBufferMode bufferMode) {
    return new IterableCoords(bufferMode);
  }

  /**
   * Returns a permuted view of this tensor.
   *
   * @param permutation the permutation (accepts negative indices).
   * @return a permuted view of this tensor.
   */
  @Override
  public ZTensor permute(@Nonnull int... permutation) {
    var perm = IndexingFns.resolvePermutation(permutation, ndim());

    int[] newShape = new int[ndim()];
    int[] newStride = new int[ndim()];
    for (int i = 0; i < ndim(); ++i) {
      newShape[i] = shape[perm[i]];
      newStride[i] = stride[perm[i]];
    }

    return new ZTensor(mutable, newShape, newStride, data, data_offset);
  }

  /**
   * Create a copy of this tensor with a reordered dimension.
   *
   * @param dim the dimension to reorder.
   * @param permutation the permutation of the dimension.
   * @return a copy of this tensor with a reordered dimension.
   */
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
   * Transpose two dimensions of this tensor.
   *
   * @param a the first dimension, accepts negative indices.
   * @param b the second dimension, accepts negative indices.
   * @return a transposed view of this tensor.
   */
  public ZTensor transpose(int a, int b) {
    int rA = resolveDim(a);
    int rB = resolveDim(b);
    if (rA == rB) {
      return this;
    }

    int[] perm = IndexingFns.iota(ndim());
    perm[rA] = rB;
    perm[rB] = rA;
    return permute(perm);
  }

  /**
   * Transpose this tensor by reversing its dimensions.
   *
   * @return a transposed view of this tensor.
   */
  public ZTensor transpose() {
    return permute(IndexingFns.aoti(ndim()));
  }

  /**
   * Transpose this tensor by reversing its dimensions.
   *
   * @return a transposed view of this tensor.
   */
  public ZTensor T() {
    return transpose();
  }

  /**
   * Returns a view of this tensor with the given dimension reversed.
   *
   * @param d the dimension to reverse, accepts negative indices.
   * @return a view of this tensor with the given dimension reversed.
   */
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
  public ZTensor unsqueeze(int d) {
    int rD = IndexingFns.resolveDim(d, shape.length + 1);

    int[] newShape = new int[ndim() + 1];
    int[] newStride = new int[ndim() + 1];

    System.arraycopy(shape, 0, newShape, 0, rD);
    System.arraycopy(shape, rD, newShape, rD + 1, ndim() - rD);

    System.arraycopy(stride, 0, newStride, 0, rD);
    System.arraycopy(stride, rD, newStride, rD + 1, ndim() - rD);

    newShape[rD] = 1;
    newStride[rD] = 0;

    return new ZTensor(mutable, newShape, newStride, data, data_offset);
  }

  /**
   * Copy the given array, removing the given index.
   *
   * @param arr the array.
   * @param index the index to remove.
   * @return a copy of the array with the given index removed.
   */
  int[] _removeIdx(int[] arr, int index) {
    int[] res = new int[arr.length - 1];
    System.arraycopy(arr, 0, res, 0, index);
    System.arraycopy(arr, index + 1, res, index, arr.length - index - 1);
    return res;
  }

  /**
   * Returns a view of this tensor with a dimensions of size 1 removed.
   *
   * @param d the dimension to remove; accepts negative indices.
   * @return a view of this tensor with a dimensions of size 1 removed.
   */
  public ZTensor squeeze(int d) {
    int rD = resolveDim(d);

    if (stride[rD] != 0) {
      throw new IllegalArgumentException(
          "dimension " + rD + ", shape " + shape[rD] + " is not squeezable");
    }

    return new ZTensor(mutable, _removeIdx(shape, rD), _removeIdx(stride, rD), data, data_offset);
  }

  /**
   * Return a view of this tensor with a broadcastable dimension expanded.
   *
   * @param dim the dimension to expand (must be size 1, or a previously broadcasted dimension).
   * @param size the new size of the dimension.
   * @return a view of this tensor with a broadcastable dimension expanded.
   */
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
  public ZTensor broadcastLike(ZTensor ref) {
    return broadcastTo(ref.shape);
  }

  /**
   * Return a view of this tensor broadcasted to the given shape.
   *
   * @param targetShape the target shape.
   * @return a broadcasted view of this tensor.
   */
  public ZTensor broadcastTo(@Nonnull int... targetShape) {
    if (Arrays.equals(shape, targetShape)) {
      return this;
    }

    if (isScalar() && IndexingFns.shapeToSize(targetShape) == 0) {
      return new ZTensor(mutable, targetShape);
    }

    var res = this;
    if (res.ndim() > targetShape.length) {
      throw new IllegalArgumentException(
          "Cannot broadcast shape "
              + Arrays.toString(shape)
              + " to "
              + Arrays.toString(targetShape));
    }
    while (res.ndim() < targetShape.length) {
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
   * Set the cell-value at the given coordinates.
   *
   * <p>Assumes the tensor is mutable.
   *
   * @param coords the coordinates.
   * @param value the value to set.
   * @throws IndexOutOfBoundsException if the coordinates are out of bounds.
   */
  void _unchecked_set(@Nonnull int[] coords, int value) {
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
    assertNdim(0);
    return get();
  }

  /** Convert this structure to a T1 (a vector) value. Assert that the shape is valid. */
  public @Nonnull int[] toT1() {
    assertNdim(1);
    return (int[]) toArray();
  }

  /** Convert this structure to a T2 (a matrix) value. Assert that the shape is valid. */
  public @Nonnull int[][] toT2() {
    assertNdim(2);
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
    tensor.broadcastLike(this).forEachItem(this::_unchecked_set);
  }

  /**
   * Assign from an element-wise unary operation.
   *
   * @param op the operation.
   * @param tensor the input tensor.
   */
  public void assignFromMap(@Nonnull IntFunction<Integer> op, @Nonnull ZTensor tensor) {
    assertMutable();
    tensor
        .broadcastLike(this)
        .forEachItem((coords, value) -> _unchecked_set(coords, op.apply(value)));
  }

  /**
   * Assign from an element-wise binary operation.
   *
   * @param op the operation.
   * @param lhs the left-hand side tensor.
   * @param rhs the right-hand side tensor.
   */
  public void assignFromMap(
      @Nonnull BinaryOperator<Integer> op, @Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
    assertMutable();
    lhs = lhs.broadcastLike(this);
    rhs = rhs.broadcastLike(this);
    for (int[] coords : byCoords(CoordsBufferMode.REUSED)) {
      _unchecked_set(coords, op.apply(lhs.get(coords), rhs.get(coords)));
    }
  }

  /**
   * An in-place element-wise binary operation.
   *
   * @param op the operation.
   * @param rhs the right-hand side tensor.
   */
  public void binOp_(@Nonnull BinaryOperator<Integer> op, @Nonnull ZTensor rhs) {
    assignFromMap(op, this, rhs);
  }

  /**
   * An in-place element-wise binary operation.
   *
   * @param op the operation.
   * @param rhs the right-hand side scalar.
   */
  public void binOp_(@Nonnull BinaryOperator<Integer> op, int rhs) {
    binOp_(op, ZTensor.scalar(rhs));
  }

  /** Namespace of ZTensor operations. */
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
    public static @Nonnull ZTensor uniOp(
        @Nonnull IntFunction<Integer> op, @Nonnull ZTensor tensor) {
      var result = zeros_like(tensor);
      result.assignFromMap(op, tensor);
      return result;
    }

    /**
     * Negate a tensor.
     *
     * @param tensor the input tensor.
     * @return a new tensor.
     */
    public static @Nonnull ZTensor neg(@Nonnull ZTensor tensor) {
      return uniOp(x -> -x, tensor);
    }

    /**
     * An element-wise binary operation.
     *
     * @param op the operation.
     * @param lhs the left-hand side tensor.
     * @param rhs the right-hand side tensor.
     * @return a new tensor.
     */
    public static @Nonnull ZTensor binOp(
        BinaryOperator<Integer> op, @Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
      var result = zeros(IndexingFns.commonBroadcastShape(lhs.shape, rhs.shape));
      result.assignFromMap(op, lhs, rhs);
      return result;
    }

    /**
     * An element-wise binary operation.
     *
     * @param op the operation.
     * @param lhs the left-hand side tensor.
     * @param rhs the right-hand side scalar.
     * @return a new tensor.
     */
    public static @Nonnull ZTensor binOp(
        BinaryOperator<Integer> op, @Nonnull ZTensor lhs, int rhs) {
      return binOp(op, lhs, loom.zspace.ZTensor.scalar(rhs));
    }

    /**
     * An element-wise binary operation.
     *
     * @param op the operation.
     * @param lhs the left-hand side scalar.
     * @param rhs the right-hand side tensor.
     * @return a new tensor.
     */
    public static @Nonnull ZTensor binOp(
        BinaryOperator<Integer> op, int lhs, @Nonnull ZTensor rhs) {
      return binOp(op, loom.zspace.ZTensor.scalar(lhs), rhs);
    }

    public static @Nonnull ZTensor min(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
      return binOp(Math::min, lhs, rhs);
    }

    public static @Nonnull ZTensor min(@Nonnull ZTensor lhs, int rhs) {
      return binOp(Math::min, lhs, rhs);
    }

    public static @Nonnull ZTensor min(int lhs, @Nonnull ZTensor rhs) {
      return binOp(Math::min, lhs, rhs);
    }

    public static @Nonnull ZTensor max(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
      return binOp(Math::max, lhs, rhs);
    }

    public static @Nonnull ZTensor max(@Nonnull ZTensor lhs, int rhs) {
      return binOp(Math::max, lhs, rhs);
    }

    public static @Nonnull ZTensor max(int lhs, @Nonnull ZTensor rhs) {
      return binOp(Math::max, lhs, rhs);
    }

    public static @Nonnull ZTensor add(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
      return binOp(Integer::sum, lhs, rhs);
    }

    public static @Nonnull ZTensor add(@Nonnull ZTensor lhs, int rhs) {
      return binOp(Integer::sum, lhs, rhs);
    }

    public static @Nonnull ZTensor add(int lhs, @Nonnull ZTensor rhs) {
      return binOp(Integer::sum, lhs, rhs);
    }

    public static void add_(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
      lhs.binOp_(Integer::sum, rhs);
    }

    public static void add_(@Nonnull ZTensor lhs, int rhs) {
      lhs.binOp_(Integer::sum, rhs);
    }

    public static @Nonnull ZTensor sub(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
      return binOp((l, r) -> l - r, lhs, rhs);
    }

    public static @Nonnull ZTensor sub(@Nonnull ZTensor lhs, int rhs) {
      return binOp((l, r) -> l - r, lhs, rhs);
    }

    public static void sub_(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
      lhs.binOp_((l, r) -> l - r, rhs);
    }

    public static void sub_(@Nonnull ZTensor lhs, int rhs) {
      lhs.binOp_((l, r) -> l - r, rhs);
    }

    public static @Nonnull ZTensor sub(int lhs, @Nonnull ZTensor rhs) {
      return binOp((l, r) -> l - r, lhs, rhs);
    }

    public static @Nonnull ZTensor mul(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
      return binOp((l, r) -> l * r, lhs, rhs);
    }

    public static @Nonnull ZTensor mul(@Nonnull ZTensor lhs, int rhs) {
      return binOp((l, r) -> l * r, lhs, rhs);
    }

    public static @Nonnull ZTensor mul(int lhs, @Nonnull ZTensor rhs) {
      return binOp((l, r) -> l * r, lhs, rhs);
    }

    public static void mul_(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
      lhs.binOp_((l, r) -> l * r, rhs);
    }

    public static void mul_(@Nonnull ZTensor lhs, int rhs) {
      lhs.binOp_((l, r) -> l * r, rhs);
    }

    public static @Nonnull ZTensor div(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
      return binOp((l, r) -> l / r, lhs, rhs);
    }

    public static @Nonnull ZTensor div(@Nonnull ZTensor lhs, int rhs) {
      return binOp((l, r) -> l / r, lhs, rhs);
    }

    public static @Nonnull ZTensor div(int lhs, @Nonnull ZTensor rhs) {
      return binOp((l, r) -> l / r, lhs, rhs);
    }

    public static void div_(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
      lhs.binOp_((l, r) -> l / r, rhs);
    }

    public static void div_(@Nonnull ZTensor lhs, int rhs) {
      lhs.binOp_((l, r) -> l / r, rhs);
    }

    public static @Nonnull ZTensor mod(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
      return binOp((l, r) -> l % r, lhs, rhs);
    }

    public static @Nonnull ZTensor mod(@Nonnull ZTensor lhs, int rhs) {
      return binOp((l, r) -> l % r, lhs, rhs);
    }

    public static @Nonnull ZTensor mod(int lhs, @Nonnull ZTensor rhs) {
      return binOp((l, r) -> l % r, lhs, rhs);
    }

    public static void mod_(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
      lhs.binOp_((l, r) -> l % r, rhs);
    }

    public static void mod_(@Nonnull ZTensor lhs, int rhs) {
      lhs.binOp_((l, r) -> l % r, rhs);
    }
  }

  public @Nonnull ZTensor neg() {
    return Ops.neg(this);
  }

  public @Nonnull ZTensor add(@Nonnull ZTensor rhs) {
    return Ops.add(this, rhs);
  }

  public @Nonnull ZTensor add(int rhs) {
    return Ops.add(this, rhs);
  }

  public void add_(@Nonnull ZTensor rhs) {
    Ops.add_(this, rhs);
  }

  public void add_(int rhs) {
    Ops.add_(this, rhs);
  }

  public @Nonnull ZTensor sub(@Nonnull ZTensor rhs) {
    return Ops.sub(this, rhs);
  }

  public @Nonnull ZTensor sub(int rhs) {
    return Ops.sub(this, rhs);
  }

  public void sub_(@Nonnull ZTensor rhs) {
    Ops.sub_(this, rhs);
  }

  public void sub_(int rhs) {
    Ops.sub_(this, rhs);
  }

  public @Nonnull ZTensor mul(@Nonnull ZTensor rhs) {
    return Ops.mul(this, rhs);
  }

  public @Nonnull ZTensor mul(int rhs) {
    return Ops.mul(this, rhs);
  }

  public void mul_(@Nonnull ZTensor rhs) {
    Ops.mul_(this, rhs);
  }

  public void mul_(int rhs) {
    Ops.mul_(this, rhs);
  }

  public @Nonnull ZTensor div(@Nonnull ZTensor rhs) {
    return Ops.div(this, rhs);
  }

  public @Nonnull ZTensor div(int rhs) {
    return Ops.div(this, rhs);
  }

  public void div_(@Nonnull ZTensor rhs) {
    Ops.div_(this, rhs);
  }

  public void div_(int rhs) {
    Ops.div_(this, rhs);
  }

  public @Nonnull ZTensor mod(@Nonnull ZTensor rhs) {
    return Ops.mod(this, rhs);
  }

  public @Nonnull ZTensor mod(int rhs) {
    return Ops.mod(this, rhs);
  }

  public void mod_(@Nonnull ZTensor rhs) {
    Ops.mod_(this, rhs);
  }

  public void mod_(int rhs) {
    Ops.mod_(this, rhs);
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

    static final class Serializer extends JsonSerializer<ZTensor> {
      @Override
      @SuppressWarnings({"Convert2Lambda", "Anonymous2MethodRef"})
      public void serialize(ZTensor value, JsonGenerator gen, SerializerProvider serializers) {

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
      public ZTensor deserialize(JsonParser p, DeserializationContext ctxt)
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
}
