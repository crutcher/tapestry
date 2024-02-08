package org.tensortapestry.zspace;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.*;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.tensortapestry.zspace.exceptions.ZDimMissMatchError;
import org.tensortapestry.zspace.impl.HasJsonOutput;
import org.tensortapestry.zspace.impl.ZSpaceJsonUtil;
import org.tensortapestry.zspace.indexing.*;
import org.tensortapestry.zspace.ops.CellWiseOps;
import org.tensortapestry.zspace.ops.ReduceOps;

/**
 * A multidimensional int array used for numerical operations.
 *
 * <p>ZTensor is a class representing a multidimensional array used for numerical operations,
 * commonly known as a tensor. Important functionalities of this class include mutability, views,
 * and cloning. These concepts functionally impact the class's interactions with operators and
 * storage management.
 *
 * <p>As this tensor exists solely to support discrete space range calculations,
 * C/C++/JNI/CUDA/BLAS accelerations are not needed; the focus is on what permits the block
 * expression index projection math to be as readable as possible; targeting succinctness with range
 * and shape assertions.
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
@SuppressWarnings("MemberName")
public final class ZTensor
  implements ZTensorWrapper, HasJsonOutput, Cloneable, HasDimension, HasSize, HasPermute<ZTensor> {

  /**
   * Construct a new mutable scalar (0-dim) tensor.
   *
   * @param value the scalar value.
   * @return the new tensor.
   */
  @Nonnull
  public static ZTensor newScalar(int value) {
    return new ZTensor(true, new int[] {}, new int[] {}, new int[] { value }, 0);
  }

  /**
   * Construct a new mutable vector (1-dim) tensor.
   *
   * @param values the vector values.
   * @return the new tensor.
   */
  @Nonnull
  public static ZTensor newVector(@Nonnull List<Integer> values) {
    return newVector(IndexingFns.unboxList(values));
  }

  /**
   * Construct a new mutable vector (1-dim) tensor.
   *
   * @param values the vector values.
   * @return the new tensor.
   */
  @Nonnull
  public static ZTensor newVector(@Nonnull int... values) {
    return newFromArray(values);
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
    int[] shape = new int[] { numRows, numCols };

    int[] data = new int[numRows * numCols];
    for (int i = 0; i < numRows; ++i) {
      if (rows[i].length != numCols) {
        throw new IllegalArgumentException(
          "All rows must have the same length, found: " +
          Arrays.toString(Arrays.stream(rows).mapToInt(Array::getLength).toArray())
        );
      }
      System.arraycopy(rows[i], 0, data, numCols * i, numCols);
    }
    return new ZTensor(true, shape, IndexingFns.shapeToLfsStrides(shape), data, 0);
  }

  /**
   * Construct a new mutable tensor filled with zeros.
   *
   * @param shape the shape of the tensor.
   * @return a new mutable ZTensor.
   */
  @Nonnull
  public static ZTensor newZeros(@Nonnull int... shape) {
    return new ZTensor(shape.clone());
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
   * Construct a new mutable ZTensor filled with zeros with a shape like the given ZTensor.
   *
   * @param ref the ZTensor to copy the shape from.
   * @return a new mutable ZTensor.
   */
  @Nonnull
  public static ZTensor newZerosLike(@Nonnull ZTensorWrapper ref) {
    return new ZTensor(ref.unwrap().shapeAsArray());
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
  public static ZTensor newOnesLike(@Nonnull ZTensorWrapper ref) {
    return newFilledLike(ref, 1);
  }

  /**
   * Construct a new mutable tensor filled with the given fill value.
   *
   * @param shape the shape of the tensor.
   * @param value the value to fill the tensor with.
   * @return a new mutable ZTensor.
   */
  @Nonnull
  public static ZTensor newFilled(@Nonnull int[] shape, int value) {
    var tensor = new ZTensor(shape);
    tensor.fill(value);
    return tensor;
  }

  /**
   * Construct a new mutable tensor filled with the given fill value.
   *
   * @param shape the shape of the tensor.
   * @param generator the generator to fill the tensor with.
   * @return a new mutable ZTensor.
   */
  @Nonnull
  public static ZTensor newFilled(@Nonnull int[] shape, @Nonnull CellGenerator generator) {
    var tensor = new ZTensor(shape);
    tensor.fill(generator);
    return tensor;
  }

  /**
   * Construct a new mutable tensor filled with the given fill value.
   *
   * @param shape the shape of the tensor.
   * @param generator the generator to fill the tensor with.
   * @return a new mutable ZTensor.
   */
  @Nonnull
  public static ZTensor newFilled(@Nonnull int[] shape, @Nonnull IntSupplier generator) {
    var tensor = new ZTensor(shape);
    tensor.fill(generator);
    return tensor;
  }

  /**
   * Construct a new mutable ZTensor filled with the fill value with a shape like the given
   * ZTensor.
   *
   * @param ref the ZTensor to copy the shape from.
   * @param value the value to fill the tensor with.
   * @return a new mutable ZTensor.
   */
  @Nonnull
  public static ZTensor newFilledLike(@Nonnull ZTensorWrapper ref, int value) {
    return newFilled(ref.unwrap().shape, value);
  }

  /**
   * Construct a new mutable ZTensor filled with the fill value with a shape like the given
   * ZTensor.
   *
   * @param ref the ZTensor to copy the shape from.
   * @param generator the generator to fill the tensor with.
   * @return a new mutable ZTensor.
   */
  @Nonnull
  public static ZTensor newFilledLike(
    @Nonnull ZTensorWrapper ref,
    @Nonnull CellGenerator generator
  ) {
    return newFilled(ref.unwrap().shape, generator);
  }

  /**
   * Construct a new mutable ZTensor filled with the fill value with a shape like the given
   * ZTensor.
   *
   * @param ref the ZTensor to copy the shape from.
   * @param generator the generator to fill the tensor with.
   * @return a new mutable ZTensor.
   */
  @Nonnull
  public static ZTensor newFilledLike(@Nonnull ZTensorWrapper ref, @Nonnull IntSupplier generator) {
    return newFilled(ref.unwrap().shape, generator);
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
    return newDiagonalMatrix(diag);
  }

  /**
   * Construct a diagonal matrix from a list of values.
   *
   * @param diag the values to put on the diagonal.
   * @return a new ZTensor.
   */
  public static ZTensor newDiagonalMatrix(@Nonnull int... diag) {
    int k = diag.length;
    var tensor = newZeros(k, k);
    for (int i = 0; i < k; ++i) {
      tensor._unchecked_set(new int[] { i, i }, diag[i]);
    }
    return tensor;
  }

  /**
   * Construct a diagonal matrix from a list of values.
   *
   * @param diag the values to put on the diagonal.
   * @return a new ZTensor.
   */
  public static ZTensor newDiagonalMatrix(@Nonnull List<Integer> diag) {
    return newDiagonalMatrix(IndexingFns.unboxList(diag));
  }

  /**
   * Given a non-sparse array of unknown dimensionality, returns a ZTensor with the same shape and
   * data.
   *
   * <p>For {@code Integer} input, returns a scalar ZTensor.
   *
   * @param source the source array.
   * @return a new ZTensor.
   * @throws IllegalArgumentException if the source is not an array.
   */
  @Nonnull
  public static ZTensor newFromArray(@Nonnull Object source) {
    var tensor = newFromArrayNull(source);
    if (tensor == null) {
      throw new IllegalArgumentException(
        "Cannot convert object of type %s to ZTensor".formatted(
            source.getClass().getCanonicalName()
          )
      );
    }
    return tensor;
  }

  /**
   * Given a non-sparse array of unknown dimensionality, returns a ZTensor with the same shape and
   * data.
   *
   * <p>For {@code Integer} input, returns a scalar ZTensor.
   *
   * @param source the source array.
   * @return a new ZTensor, or null if the source is not an array.
   */
  @Nullable private static ZTensor newFromArrayNull(@Nonnull Object source) {
    if (!IndexingFns.isRecursiveIntArray(source)) {
      return null;
    }
    try {
      return newFromTree(
        source,
        obj -> obj.getClass().isArray(),
        Array::getLength,
        Array::get,
        obj -> (int) obj,
        int[].class::cast
      );
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * Build a new ZTensor from an FlatArray, taking ownership of the data.
   *
   * @param flatArray the array data.
   * @return a new ZTensor.
   */
  public static ZTensor newFromFlatArray_(@Nonnull FlatArray flatArray) {
    return new ZTensor(true, flatArray.getShape(), flatArray.getStrides(), flatArray.getData(), 0);
  }

  /**
   * Decode a base64 encoded FlatArray into a ZTensor.
   *
   * @param encoded the base64 encoded FlatArray.
   * @return the new tensor.
   */
  public static ZTensor newFromFlatBase64(@Nonnull String encoded) {
    byte[] bytes = Base64.getDecoder().decode(encoded);
    return newFromFlatArray_(
      Objects.requireNonNull(ZSpaceJsonUtil.fromMsgPack(bytes, FlatArray.class))
    );
  }

  /**
   * Given a non-sparse list of unknown dimensionality, returns a ZTensor with the same shape and
   * data.
   *
   * <p>For {@code Integer} input, returns a scalar ZTensor.
   *
   * @param source the source list.
   * @return a new ZTensor.
   */
  @SuppressWarnings("unchecked")
  public static ZTensor newFromList(@Nonnull Object source) {
    return newFromTree(
      source,
      obj -> obj instanceof List,
      obj -> ((List<?>) obj).size(),
      (obj, i) -> ((List<?>) obj).get(i),
      obj -> (Integer) obj,
      obj -> IndexingFns.unboxList(((List<Integer>) obj))
    );
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
    return Objects.requireNonNull(ZSpaceJsonUtil.fromJson(str, ZTensor.class));
  }

  /**
   * Decode a ZTensor from a tree of type {@code <T>}.
   *
   * <p>This is common used by the Jackson deserializer and the {@link #newFromArray} method.
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
  public static <T> @Nonnull ZTensor newFromTree(
    @Nonnull T root,
    @Nonnull Predicate<T> isArray,
    @Nonnull ToIntFunction<T> getArrayLength,
    @Nonnull BiFunction<T, Integer, T> getArrayElement,
    @Nonnull ToIntFunction<T> nodeAsScalar,
    @Nonnull Function<T, int[]> nodeAsSimpleArray
  ) {
    var arrayData = IndexingFns.arrayFromTree(
      root,
      isArray,
      getArrayLength,
      getArrayElement,
      nodeAsScalar,
      nodeAsSimpleArray
    );
    var shape = arrayData.getShape();
    var data = arrayData.getData();
    return new ZTensor(true, shape, IndexingFns.shapeToLfsStrides(shape), data, 0);
  }

  @JsonIgnore
  @Getter
  private final boolean mutable;

  @Nonnull
  private final int[] shape;

  @Getter
  private final int size;

  @Nonnull
  private final int[] stride;

  @Nonnull
  private final int[] data;

  private final int dataOffset;

  private Integer hash;

  /**
   * Construct a mutable 0-filled ZTensor of the given shape; takes ownership of the shape.
   *
   * @param shape the shape.
   */
  ZTensor(@Nonnull int[] shape) {
    this(
      true,
      shape,
      IndexingFns.shapeToLfsStrides(shape),
      new int[IndexingFns.shapeToSize(shape)],
      0
    );
  }

  /**
   * Constructs a ZTensor from parts; takes ownership of the arrays.
   *
   * @param mutable whether the ZTensor is mutable.
   * @param shape the shape.
   * @param stride the strides.
   * @param data the data.
   * @param dataOffset the offset in the source data.
   */
  @SuppressWarnings("InconsistentOverloads")
  ZTensor(
    boolean mutable,
    @Nonnull int[] shape,
    @Nonnull int[] stride,
    @Nonnull int[] data,
    int dataOffset
  ) {
    this.mutable = mutable;
    this.shape = shape;
    this.size = IndexingFns.shapeToSize(shape);
    this.stride = stride;
    this.data = data;
    this.dataOffset = dataOffset;
  }

  @Override
  @Nonnull
  public ZTensor unwrap() {
    return this;
  }

  /**
   * Is this tensor read-only / immutable?
   *
   * @return true if read-only / immutable; false otherwise.
   */
  public boolean isReadOnly() {
    return !mutable;
  }

  /**
   * Asserts that this tensor is mutable.
   */
  public void assertMutable() {
    if (!mutable) {
      throw new IllegalStateException("tensor is immutable");
    }
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

  @Override
  public int hashCode() {
    if (mutable) {
      throw new IllegalStateException("Cannot take the hash of a mutable tensor.");
    }
    if (hash == null) {
      synchronized (this) {
        hash = reduceCellsAtomic((a, b) -> 31 * a + b, 17 + Arrays.hashCode(shape));
      }
    }
    return hash;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;

    if (other instanceof ZTensorWrapper that) {
      return equalsZTensor(that.unwrap());
    }

    return equalsTree(other);
  }

  /**
   * Private equality for recursive arrays.
   *
   * @param other the other object.
   * @return true iff equals.
   */
  private boolean equalsTree(Object other) {
    // TODO: implement tree-walking comparison without a constructor.
    var that = newFromArrayNull(other);
    if (that == null) {
      return false;
    }
    return equalsZTensor(that);
  }

  /**
   * Private equality for ZTensors.
   *
   * @param other the other object.
   * @return true iff equals.
   */
  private boolean equalsZTensor(ZTensor other) {
    if (!Arrays.equals(shape, other.shape)) {
      return false;
    }

    for (var coords : byCoords(BufferOwnership.REUSED)) {
      if (other.get(coords) != get(coords)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Return a formatted string of this tensor.
   *
   * @return the string.
   */
  @Override
  public String toString() {
    var sb = new StringBuilder();
    toTree(() -> sb.append('['), () -> sb.append(']'), () -> sb.append(", "), sb::append);
    return sb.toString();
  }

  @Override
  public int getNDim() {
    return shape.length;
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
  public ZTensor clone() {
    return clone(mutable);
  }

  /**
   * Clone this tensor.
   *
   * <p>The clone will have the target {@link #mutable} state.
   *
   * <ul>
   *     <li>If this tensor is mutable, copies the data.</li>
   *     <li>if this tensor is immutable
   *     <ul>
   *         <li>and non-compact, copies the data.</li>
   *         <li>and compact
   *         <ul>
   *             <li>and mutable is requested, copy the data.</li>
   *             <li>and immutable is requested, return {@code this}.</li>
   *         </ul>
   *         </li>
   *     </ul>
   *     </li>
   * </ul>
   *
   * @return a tensor with the same data.
   */
  @Nonnull
  public ZTensor clone(boolean mutable) {
    if (isReadOnly() && isCompact() && !mutable) {
      return this;
    }

    var res = new ZTensor(shape);
    forEach(res::set, BufferOwnership.REUSED);
    if (!mutable) {
      return new ZTensor(false, res.shape, res.stride, res.data, res.dataOffset);
    }
    return res;
  }

  /**
   * Assert that this tensor has the same shape as another tensor.
   *
   * @param other the other tensor.
   * @throws ZDimMissMatchError if the shapes do not match.
   */
  public void assertSameShape(@Nonnull ZTensorWrapper other) {
    if (!Arrays.equals(shape, other.unwrap()._unsafeGetShape())) {
      throw new ZDimMissMatchError(
        String.format("ZDim shape mismatch: %s != %s", shapeAsList(), other.unwrap().shapeAsList())
      );
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
   * Return a view of this tensor with the given dimensional selection.
   *
   * @param selectorExpr the selector expression.
   * @return the view tensor.
   */
  @Nonnull
  public ZTensor select(@Nonnull String selectorExpr) {
    return select(Selector.parseSelectors(selectorExpr));
  }

  /**
   * Return a view of this tensor with the given dimensional selection.
   *
   * @param selectors the selectors.
   * @return the view tensor.
   */
  @Nonnull
  public ZTensor select(@Nonnull Selector... selectors) {
    return select(Arrays.asList(selectors));
  }

  /**
   * Return a view of this tensor with the given dimensional selection.
   *
   * @param selectors the selectors.
   * @return the view tensor.
   */
  @Nonnull
  @SuppressWarnings("unused")
  public ZTensor select(@Nonnull List<Selector> selectors) {
    var ellipsisCount = selectors.stream().filter(selector -> selector instanceof Ellipsis).count();
    if (ellipsisCount > 1) {
      throw new IllegalArgumentException("Multiple ellipsis in selection: " + selectors);
    }

    int nextDim = 0;
    var cur = this;

    for (var selIdx = 0; selIdx < selectors.size(); selIdx++) {
      var selector = Objects.requireNonNull(selectors.get(selIdx), "selector");
      switch (selector) {
        case Index index -> cur = cur.selectDim(nextDim, index.getIndex()); // shrinks cur, so we don't increment nextDim.
        case NewAxis newAxis -> {
          cur = cur.unsqueeze(nextDim);
          nextDim++;
          cur = cur.broadcastDim(nextDim - 1, newAxis.getSize());
        }
        case Slice slice -> {
          cur = cur.sliceDim(nextDim, slice);
          nextDim++;
        }
        case Ellipsis ellipsis -> {
          var renaming = (int) selectors
            .subList(selIdx + 1, selectors.size())
            .stream()
            .filter(s -> !(s instanceof NewAxis))
            .count();

          var skipDims = selectors.size() - selIdx - 1 - renaming;
          nextDim += skipDims;
        }
        default -> throw new IllegalArgumentException("Unknown selector: " + selector);
      }
    }
    return cur;
  }

  /**
   * Compute the ravel index into the data array for the given coordinates.
   *
   * @param coords the coordinates.
   * @return the ravel index.
   */
  private int ravel(@Nonnull int... coords) {
    return IndexingFns.ravel(shape, stride, coords, dataOffset);
  }

  /**
   * Returns an {@code Iterable<int[]>} over the coordinates of this tensor.
   *
   * <p>When the buffer mode is {@link BufferOwnership#REUSED}, the buffer is shared between
   * subsequent calls to {@link Iterator#next()}. When the buffer mode is
   * {@link BufferOwnership#CLONED}, the buffer is not shared between subsequent calls to
   * {@link Iterator#next()}.
   *
   * <p>Empty tensors will return an empty iterable.
   *
   * <p>Scalar tensors will return an iterable with a single empty coordinate array.
   *
   * @param bufferOwnership the buffer mode.
   * @return an iterable over the coordinates of this tensor.
   */
  @Nonnull
  public IterableCoordinates byCoords(@Nonnull BufferOwnership bufferOwnership) {
    return new IterableCoordinates(bufferOwnership, shape);
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
   * Assign inplace from a tensor.
   *
   * @param tensor the input tensor.
   */
  public void assign_(@Nonnull ZTensorWrapper tensor) {
    assertMutable();
    tensor.unwrap().broadcastLike(this).forEach(this::_unchecked_set, BufferOwnership.REUSED);
  }

  /**
   * Fill the tensor with a value.
   *
   * @param fill_value the value to fill with.
   */
  public void fill(int fill_value) {
    assertMutable();
    if (isCompact()) {
      Arrays.fill(data, fill_value);
    } else {
      for (int[] coords : byCoords(BufferOwnership.REUSED)) {
        _unchecked_set(coords, fill_value);
      }
    }
  }

  /**
   * Fill the tensor with generated values.
   *
   * @param generator the generator.
   */
  public void fill(@Nonnull CellGenerator generator) {
    assertMutable();
    for (int[] coords : byCoords(BufferOwnership.REUSED)) {
      _unchecked_set(coords, generator.generate(coords));
    }
  }

  /**
   * Fill the tensor with generated values.
   *
   * @param generator the supplier.
   */
  public void fill(@Nonnull IntSupplier generator) {
    assertMutable();
    for (int[] coords : byCoords(BufferOwnership.REUSED)) {
      _unchecked_set(coords, generator.getAsInt());
    }
  }

  /**
   * Iterate over the coordinates and values of this tensor.
   *
   * <p>When the buffer mode is {@link BufferOwnership#REUSED}, the buffer is shared between
   * subsequent calls to {@link CellConsumer#accept(int[], int)}. When the buffer mode is
   * {@link BufferOwnership#CLONED}, the buffer is not shared between subsequent calls to
   * {@link CellConsumer#accept(int[], int)}.
   *
   * @param consumer the consumer.
   * @param bufferOwnership the buffer mode.
   */
  public void forEach(@Nonnull CellConsumer consumer, @Nonnull BufferOwnership bufferOwnership) {
    byCoords(bufferOwnership).forEach(c -> consumer.accept(c, get(c)));
  }

  /**
   * Return an IntStream of the values of this tensor.
   *
   * @return the stream.
   */
  public IntStream valueStream() {
    return byCoords(BufferOwnership.REUSED).stream().mapToInt(this::get);
  }

  /**
   * Iterate over the values of this tensor.
   *
   * @param consumer the consumer.
   */
  public void forEachValue(@Nonnull IntConsumer consumer) {
    valueStream().forEach(consumer);
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
    return valueStream().allMatch(predicate);
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
    return valueStream().anyMatch(predicate);
  }

  /**
   * Are all cells {@code > 0}?
   *
   * @return true if all cells are {@code > 0}.
   */
  public boolean isStrictlyPositive() {
    return allMatch(x -> x > 0);
  }

  /**
   * Are all cells {@code >= 0}?
   *
   * @return true if all cells are {@code >= 0}.
   */
  public boolean isNonNegative() {
    return allMatch(x -> x >= 0);
  }

  /**
   * Serialize this tensor to a tree data structure.
   *
   * <p>This is common code used by the Jackson serializer and the {@link #toArray} method.
   *
   * @param startArray start an array, called once per nested array.
   * @param endArray end an array, called once per nested array.
   * @param elemSep write an element separator.
   * @param writeNumber write a number.
   */
  public void toTree(
    @Nonnull Runnable startArray,
    @Nonnull Runnable endArray,
    @Nonnull Runnable elemSep,
    @Nonnull Consumer<Integer> writeNumber
  ) {
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

    for (int[] coords : byCoords(BufferOwnership.REUSED)) {
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

  /**
   * Convert this structure to a T0 (a scalar) value. Assert that the shape is valid.
   */
  public int toT0() {
    assertNDim(0);
    return get();
  }

  /**
   * Convert this structure to a T1 (a vector) value. Assert that the shape is valid.
   */
  @Nonnull
  public int[] toT1() {
    assertNDim(1);
    return (int[]) toArray();
  }

  /**
   * Convert this structure to a T2 (a matrix) value. Assert that the shape is valid.
   */
  @Nonnull
  public int[][] toT2() {
    assertNDim(2);
    return (int[][]) toArray();
  }

  /**
   * Convert this tensor to a Java array. Scalars will be converted to a single int.
   *
   * @return the new java structure.
   */
  @Nonnull
  @SuppressWarnings("ConstantConditions")
  public Object toArray() {
    if (isScalar()) {
      return item();
    }

    Object arr = Array.newInstance(int.class, shape);

    var ndim = getNDim();
    int[] chunk = null;
    for (int[] coords : byCoords(BufferOwnership.REUSED)) {
      int lsd = coords[ndim - 1];

      // Find the chunk to write to in the target array;
      // only recompute if the last stride dimension is 0.
      if (lsd == 0) {
        var it = arr;
        for (int d = 0; d < ndim - 1; ++d) {
          it = Array.get(it, coords[d]);
        }
        chunk = (int[]) it;
      }
      chunk[lsd] = get(coords);
    }

    return arr;
  }

  /**
   * Convert this tensor to a flat Java array.
   *
   * @return an FlatArray ( shape, data ) pair.
   */
  @Nonnull
  FlatArray toFlatArray() {
    var compact = asImmutable();
    return new FlatArray(compact.shape, compact.data);
  }

  /**
   * Encode this tensor to a base64 encoded FlatArray.
   *
   * @return the base64 encoded FlatArray.
   */
  @Nonnull
  String toFlatBase64() {
    return new String(
      Base64.getEncoder().encode(ZSpaceJsonUtil.toMsgPack(toFlatArray())),
      StandardCharsets.UTF_8
    );
  }

  /**
   * Get the value of this tensor as a T0 (a scalar).
   */
  public int item() {
    return toT0();
  }

  /**
   * Return a view of this tensor broadcasted like the reference tensor.
   *
   * @param ref the reference tensor.
   * @return a broadcasted view of this tensor.
   */
  @Nonnull
  public ZTensor broadcastLike(@Nonnull ZTensorWrapper ref) {
    return broadcastTo(ref.unwrap().shape);
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
      return new ZTensor(
        mutable,
        targetShape,
        IndexingFns.shapeToLfsStrides(targetShape),
        new int[0],
        0
      );
    }

    var res = this;
    if (res.getNDim() > targetShape.length) {
      throw new IllegalArgumentException(
        "Cannot broadcast shape %s to %s".formatted(
            Arrays.toString(shape),
            Arrays.toString(targetShape)
          )
      );
    }
    while (res.getNDim() < targetShape.length) {
      res = res.unsqueeze(0);
    }
    for (int i = 0; i < targetShape.length; ++i) {
      if (res.shape[i] > 1 && res.shape[i] != targetShape[i]) {
        throw new IllegalArgumentException(
          "Cannot broadcast shape %s to %s".formatted(
              Arrays.toString(this.shape),
              Arrays.toString(targetShape)
            )
        );
      }
      if (res.shape[i] == 1 && targetShape[i] > 1) {
        res = res.broadcastDim(i, targetShape[i]);
      }
    }
    return res;
  }

  /**
   * Create a view of this tensor with an extra dimension added at index `d`.
   *
   * @param dim the dimension to add.
   * @return a view of this tensor with an extra dimension added at index `d`.
   */
  @Nonnull
  public ZTensor unsqueeze(int dim) {
    int rDim = IndexingFns.resolveDim(dim, getNDim() + 1);

    return new ZTensor(
      mutable,
      IndexingFns.addIdx(shape, rDim, 1),
      IndexingFns.addIdx(stride, rDim, 0),
      data,
      dataOffset
    );
  }

  /**
   * Returns a view of this tensor with a dimensions of size 1 removed.
   *
   * @param dim the dimension to remove; accepts negative indices.
   * @return a view of this tensor with a dimensions of size 1 removed.
   */
  @Nonnull
  public ZTensor squeeze(int dim) {
    int rDim = resolveDim(dim);

    if (stride[rDim] != 0) {
      throw new IllegalArgumentException(
        "dimension " + rDim + ", shape " + shape[rDim] + " is not squeezable"
      );
    }

    int[] shape1 = IndexingFns.removeIdx(shape, rDim);
    int[] stride1 = IndexingFns.removeIdx(stride, rDim);
    return new ZTensor(mutable, shape1, stride1, data, dataOffset);
  }

  /**
   * Return a view of this tensor with a broadcastable dimension expanded.
   *
   * @param dim the dimension to expand (must be size 1, or a previously broadcasted
   *         dimension).
   * @param size the new size of the dimension.
   * @return a view of this tensor with a broadcastable dimension expanded.
   */
  @Nonnull
  public ZTensor broadcastDim(int dim, int size) {
    dim = resolveDim(dim);
    if (stride[dim] != 0) {
      throw new IllegalArgumentException(
        "Cannot broadcast dimension %d with real-size %d".formatted(dim, shape[dim])
      );
    }

    var new_shape = shapeAsArray();
    new_shape[dim] = size;

    var new_stride = stride.clone();
    new_stride[dim] = 0;

    return new ZTensor(mutable, new_shape, new_stride, data, dataOffset);
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
   * Create a new tensor by mapping a function over the values of the tensor.
   *
   * @param op the function to apply.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor map(@Nonnull IntUnaryOperator op) {
    return CellWiseOps.map(op, this);
  }

  /**
   * An in-place element-wise unary operation.
   *
   * @param op the operation.
   */
  public void map_(@Nonnull IntUnaryOperator op) {
    assignFromMap_(op, this);
  }

  /**
   * Assign inplace from an element-wise unary operation.
   *
   * @param op the operation.
   * @param tensor the input tensor.
   */
  public void assignFromMap_(@Nonnull IntUnaryOperator op, @Nonnull ZTensorWrapper tensor) {
    assertMutable();
    tensor
      .unwrap()
      .broadcastLike(this)
      .forEach(
        (coords, value) -> _unchecked_set(coords, op.applyAsInt(value)),
        BufferOwnership.REUSED
      );
  }

  /**
   * Broadcasts two tensors together, and maps a function over the values of the tensors.
   *
   * @param op the function to apply.
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor zipWith(@Nonnull IntBinaryOperator op, @Nonnull ZTensorWrapper rhs) {
    return CellWiseOps.zipWith(op, this, rhs);
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
   * An in-place element-wise binary operation.
   *
   * @param op the operation.
   * @param rhs the right-hand side tensor.
   */
  public void zipWith_(@Nonnull IntBinaryOperator op, @Nonnull ZTensorWrapper rhs) {
    assignFromZipWith_(op, this, rhs);
  }

  /**
   * Assign inplace from an element-wise binary operation.
   *
   * @param op the operation.
   * @param lhs the left-hand side tensor.
   * @param rhs the right-hand side tensor.
   */
  public void assignFromZipWith_(
    @Nonnull IntBinaryOperator op,
    @Nonnull ZTensorWrapper lhs,
    @Nonnull ZTensorWrapper rhs
  ) {
    var zlhs = lhs.unwrap();
    var zrhs = rhs.unwrap();
    assertMutable();
    zlhs = zlhs.broadcastLike(this);
    zrhs = zrhs.broadcastLike(this);
    for (int[] coords : byCoords(BufferOwnership.REUSED)) {
      _unchecked_set(coords, op.applyAsInt(zlhs.get(coords), zrhs.get(coords)));
    }
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
    return ReduceOps.reduceCells(this, op, initial);
  }

  /**
   * Applies the given reduction operation to all values in the given tensor; grouping by the
   * specified dimensions.
   *
   * <p>The shape of the returned tensor is the same as the shape of the input tensor, except
   * that the specified dimensions are removed.
   *
   * @param op the reduction operation
   * @param initial the initial value
   * @param dims the dimensions to group by.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor reduceCells(@Nonnull IntBinaryOperator op, int initial, @Nonnull int... dims) {
    return ReduceOps.reduceCells(this, op, initial, dims);
  }

  /**
   * Applies the given reduction operation to all values in the given tensor.
   *
   * @param op the reduction operation
   * @param initial the initial value
   * @return the int result of the reduction.
   */
  public int reduceCellsAtomic(@Nonnull IntBinaryOperator op, int initial) {
    return ReduceOps.reduceCellsAtomic(this, op, initial);
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
  public void assertMatchingShape(@Nonnull ZTensorWrapper other) {
    IndexingFns.assertShape(shape, other.unwrap().shape);
  }

  /**
   * Return an immutable tensor with the same data.
   *
   * <p>If this tensor is already immutable, returns this; otherwise, returns an immutable clone.
   *
   * <p>Semantically equivalent to {@code clone(false)}.
   *
   * <p>A performance oriented Tensor library would track open mutable views of the underlying
   * data, and perform copy-on-write when necessary; as this is a correctness-oriented Tensor
   * library, we simply clone the data to go from mutable to immutable.
   *
   * @return an immutable tensor.
   */
  @Nonnull
  public ZTensor asImmutable() {
    return clone(false);
  }

  /**
   * Asserts that this tensor is read-only / immutable.
   */
  public void assertReadOnly() {
    if (mutable) {
      throw new IllegalStateException("tensor is mutable");
    }
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
   * Unsafe accessor for the shape.
   *
   * @return the shape.
   */
  @Nonnull
  int[] _unsafeGetShape() {
    return shape;
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
    return IndexingFns.boxArray(shape);
  }

  /**
   * Returns the shape of this tensor as a tensor.
   *
   * @return the shape of this tensor as a tensor.
   */
  @Nonnull
  public ZTensor shapeAsTensor() {
    return ZTensor.newVector(shape);
  }

  /**
   * Transposes (swaps) two dimensions of this tensor.
   *
   * <p>This method creates a new view of the tensor where the specified dimensions are swapped.
   * The original tensor remains unchanged. The returned tensor shares data with the original
   * tensor.
   *
   * <p>This operation can be useful in scenarios where you need to change the order of two
   * dimensions in a tensor, for example, when you want to switch rows and columns in a 2D tensor
   * (matrix).
   *
   * <p>Supports negative dimension indexing - i.e. -1 represents the last dimension, -2
   * represents the second last, and so on.
   *
   * @param a The index of the first dimension to be transposed.
   * @param b The index of the second dimension to be transposed.
   * @return A new tensor that is a transposed view of the original tensor.
   * @throws IllegalArgumentException If the provided indices are not valid dimensions of
   *         the tensor.
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

  @Override
  @Nonnull
  public ZTensor permute(@Nonnull int... permutation) {
    var perm = IndexingFns.resolvePermutation(permutation, getNDim());
    return new ZTensor(
      mutable,
      IndexingFns.applyResolvedPermutation(shape, perm),
      IndexingFns.applyResolvedPermutation(stride, perm),
      data,
      dataOffset
    );
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
   * Transpose this tensor by reversing its dimensions.
   *
   * @return a transposed view of this tensor.
   */
  @Nonnull
  public ZTensor transpose() {
    return permute(IndexingFns.aoti(getNDim()));
  }

  /**
   * Returns a view of this tensor with the given dimension reversed.
   *
   * @param dim the dimension to reverse, accepts negative indices.
   * @return a view of this tensor with the given dimension reversed.
   */
  @Nonnull
  public ZTensor reverse(int dim) {
    int rDim = resolveDim(dim);

    int[] newStride = stride.clone();
    newStride[rDim] *= -1;

    int newOffset = dataOffset + (shape[rDim] - 1) * stride[rDim];

    return new ZTensor(mutable, shape, newStride, data, newOffset);
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
        "dims.length (%d) != indexes.length (%d)".formatted(dims.length, indexes.length)
      );
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

    var new_shape = shapeAsArray();
    new_shape[d] = 1;
    var new_stride = stride.clone();
    new_stride[d] = 0;
    int new_offset = dataOffset + i * stride[d];

    return new ZTensor(mutable, new_shape, new_stride, data, new_offset).squeeze(d);
  }

  /**
   * Return a view of this tensor with the given dimension sliced.
   *
   * <p>Supports negative indexing - i.e. -1 represents the last index, -2 represents the
   * second.</p>
   *
   * <ul>
   *     <li>If {@code start~=0} and {@code end~=size[dim]} and {@code step~=1}, return {@code this}</li>
   *     <li>If {@code step} is {@code null}, it is either 1 or -1; depending on the ordering of {@code start} and {@code end}</li>
   *     <li>If {@code step} is positive, {@code start} must be less than {@code end}; and vice-versa.</li>
   *     </ul>
   *
   * @param dim the dimension to slice.
   * @param slice the slice description..
   * @return a view of this tensor with the given dimension sliced.
   */
  @Nonnull
  public ZTensor sliceDim(int dim, @Nonnull Slice slice) {
    var d = resolveDim(dim);

    var start = slice.getStart();
    var end = slice.getEnd();
    var step = slice.getStep();

    if (step == null || step > 0) {
      if (start == null) {
        start = 0;
      }
      if (end == null) {
        end = shape[d];
      }
      if (step == null) {
        step = 1;
      }
    } else {
      if (start == null) {
        start = shape[d] - 1;
      }
      if (end == null) {
        end = -shape[d] - 1;
      }
    }

    start = IndexingFns.resolveIndex("start", start, shape[d]);
    end = IndexingFns.resolveEndIndex("end", end, shape[d]);

    if (step == 0) {
      throw new IllegalArgumentException("slice step cannot be zero: " + slice);
    } else if (step > 0 && start > end) {
      throw new IllegalArgumentException(
        "slice start (%d) must be less than end (%d) for positive step (%d): %s".formatted(
            start,
            end,
            step,
            slice
          )
      );
    } else if (step < 0 && start < end) {
      throw new IllegalArgumentException(
        "slice start (%d) must be greater than end (%d) for negative step (%d): %s".formatted(
            start,
            end,
            step,
            slice
          )
      );
    }

    if (start == 0 && end == shape[d] && step == 1) {
      return this;
    }

    var new_shape = shape.clone();
    int absStep = Math.abs(step);
    new_shape[d] = (Math.abs(end - start) + absStep - 1) / absStep;

    var new_stride = stride.clone();
    new_stride[d] *= step;

    int new_offset = dataOffset + start * stride[d];

    return new ZTensor(mutable, new_shape, new_stride, data, new_offset);
  }

  /**
   * Creates a reordered view of this tensor along a specified dimension.
   *
   * <p><b>Example:</b> Suppose we have tensor "t" with shape [2,3]:
   *
   * <pre>
   * t = [[0, 1, 2],
   *      [3, 4, 5]]
   * </pre>
   * <p>
   * If we call {@code t.reorderDim([1,0,2], 1)}, the returned tensor will look like:
   *
   * <pre>
   * v = [[1, 0, 2],
   *      [4, 3, 5]]
   * </pre>
   *
   * <p>Supports negative dimension indexing - i.e. -1 represents the last dimension, -2
   * represents the second last, and so on.
   *
   * @param permutation An array of unique integers representing the new order of indices
   *         along the specified dimension. Each integer should be a valid index for that
   *         dimension.
   * @param dim Index of the dimension to be reordered. Dimensions are zero-indexed. This
   *         must be a valid dimension of this tensor.
   * @return A new ZTensor, with the specified dimension reordered.
   */
  @Nonnull
  public ZTensor reorderedDimCopy(@Nonnull int[] permutation, int dim) {
    var d = resolveDim(dim);
    var shape = shapeAsArray();
    var perm = IndexingFns.resolvePermutation(permutation, shape[d]);
    var res = newZeros(shape);
    for (int i = 0; i < shape[d]; ++i) {
      res.selectDim(d, i).assign_(selectDim(d, perm[i]));
    }
    return res;
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
  @UtilityClass
  public static class Serialization {

    public final class Serializer extends StdSerializer<ZTensor> {

      public Serializer() {
        super(ZTensor.class);
      }

      @Override
      @SuppressWarnings({ "Convert2Lambda", "Anonymous2MethodRef" })
      public void serialize(
        @Nonnull ZTensor value,
        @Nonnull JsonGenerator gen,
        @Nonnull SerializerProvider serializers
      ) {
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
          }
        );
      }
    }

    public class Deserializer extends StdDeserializer<ZTensor> {

      public Deserializer() {
        super(ZTensor.class);
      }

      @Override
      public ZTensor deserialize(@Nonnull JsonParser p, @Nonnull DeserializationContext context)
        throws java.io.IOException {
        return fromTreeNode(p.readValueAsTree());
      }
    }

    /**
     * Deserialize a tensor from a {@link TreeNode} data structure.
     *
     * @param node the node.
     * @return the tensor.
     */
    @Nonnull
    public ZTensor fromTreeNode(@Nonnull TreeNode node) {
      return newFromTree(
        node,
        TreeNode::isArray,
        TreeNode::size,
        TreeNode::get,
        node1 -> ((IntNode) node1).intValue(),
        node1 -> {
          int[] chunk = new int[node1.size()];
          Iterator<JsonNode> it = ((ArrayNode) node1).elements();
          for (int i = 0; i < chunk.length; ++i) {
            chunk[i] = it.next().intValue();
          }
          return chunk;
        }
      );
    }
  }

  /**
   * Returns the sum of all elements in the tensor.
   *
   * @return the int sum of all elements in the tensor.
   */
  public int sumAsInt() {
    return ReduceOps.sumAsInt(this);
  }

  /**
   * Returns the sum of all elements in the tensor.
   *
   * @return the scalar ZTensor sum of all elements in the tensor.
   */
  @Nonnull
  public ZTensor sum() {
    return ReduceOps.sum(this);
  }

  /**
   * Returns the sum of all elements in the tensor, grouped by the specified dimensions.
   *
   * <p>The shape of the returned tensor is the same as the shape of the input tensor, except
   * that the specified dimensions are removed.
   *
   * @param dims the dimensions to group by.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor sum(@Nonnull int... dims) {
    return ReduceOps.sum(this, dims);
  }

  /**
   * Returns the product of all elements in the tensor.
   *
   * @return the int prod of all elements in the tensor.
   */
  public int prodAsInt() {
    return ReduceOps.prodAsInt(this);
  }

  /**
   * Returns the product of all elements in the tensor.
   *
   * @return the scalar ZTensor prod of all elements in the tensor.
   */
  @Nonnull
  public ZTensor prod() {
    return ReduceOps.prod(this);
  }

  /**
   * Returns the product of all elements in the tensor, grouped by the specified dimensions.
   *
   * <p>The shape of the returned tensor is the same as the shape of the input tensor, except
   * that the specified dimensions are removed.
   *
   * @param dims the dimensions to group by.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor prod(@Nonnull int... dims) {
    return ReduceOps.prod(this, dims);
  }

  /**
   * Returns the minimum of all elements in the tensor.
   *
   * @return the int minimum of all elements in the tensor.
   */
  public int minAsInt() {
    return ReduceOps.minAsInt(this);
  }

  /**
   * Returns a new scalar ZTensor that contains the minimum value in this ZTensor.
   *
   * @return a new ZTensor object containing the minimum value
   */
  @Nonnull
  public ZTensor min() {
    return ReduceOps.min(this);
  }

  /**
   * Returns a new ZTensor that contains the minimum value in this ZTensor, grouped by the
   * specified dimensions.
   *
   * <p>The shape of the returned tensor is the same as the shape of the input tensor, except
   * that the specified dimensions are removed.
   *
   * @param dims the dimensions to group by.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor min(@Nonnull int... dims) {
    return ReduceOps.min(this, dims);
  }

  /**
   * Returns the maximum of all elements in the tensor.
   *
   * @return the int maximum of all elements in the tensor.
   */
  public int maxAsInt() {
    return ReduceOps.maxAsInt(this);
  }

  /**
   * Returns a new scalar ZTensor that contains the maximum value in this ZTensor.
   *
   * @return a new ZTensor object containing the maximum value
   */
  @Nonnull
  public ZTensor max() {
    return ReduceOps.max(this);
  }

  /**
   * Returns a new ZTensor that contains the maximum value in this ZTensor, grouped by the
   * specified dimensions.
   *
   * <p>The shape of the returned tensor is the same as the shape of the input tensor, except
   * that the specified dimensions are removed.
   *
   * @param dims the dimensions to group by.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor max(@Nonnull int... dims) {
    return ReduceOps.max(this, dims);
  }

  /**
   * Returns a new elementwise negation of this tensor.
   */
  @Nonnull
  public ZTensor neg() {
    return CellWiseOps.neg(this);
  }

  /**
   * Returns a new elementwise absolute value of this tensor.
   */
  @Nonnull
  public ZTensor abs() {
    return CellWiseOps.abs(this);
  }

  /**
   * Returns an elementwise broadcast addition with this tensor.
   */
  @Nonnull
  public ZTensor add(@Nonnull ZTensorWrapper rhs) {
    return CellWiseOps.add(this, rhs);
  }

  /**
   * Returns an elementwise broadcast addition with this tensor.
   */
  @Nonnull
  public ZTensor add(int rhs) {
    return CellWiseOps.add(this, rhs);
  }

  /**
   * Performs an in-place elementwise broadcast addition on this tensor.
   *
   * <p>This tensor must be mutable.
   *
   * @param rhs the right-hand side tensor.
   */
  public void add_(@Nonnull ZTensorWrapper rhs) {
    CellWiseOps.add_(this, rhs);
  }

  /**
   * Performs an in-place elementwise broadcast addition on this tensor.
   *
   * <p>This tensor must be mutable.
   *
   * @param rhs the right-hand side tensor.
   */
  public void add_(int rhs) {
    CellWiseOps.add_(this, rhs);
  }

  /**
   * Returns an elementwise broadcast subtraction with this tensor.
   *
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor sub(@Nonnull ZTensorWrapper rhs) {
    return CellWiseOps.sub(this, rhs);
  }

  /**
   * Returns an elementwise broadcast subtraction with this tensor.
   *
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor sub(int rhs) {
    return CellWiseOps.sub(this, rhs);
  }

  /**
   * Performs an in-place elementwise broadcast subtraction on this tensor.
   *
   * <p>This tensor must be mutable.
   *
   * @param rhs the right-hand side tensor.
   */
  public void sub_(@Nonnull ZTensorWrapper rhs) {
    CellWiseOps.sub_(this, rhs);
  }

  /**
   * Performs an in-place elementwise broadcast subtraction on this tensor.
   *
   * <p>This tensor must be mutable.
   *
   * @param rhs the right-hand side tensor.
   */
  public void sub_(int rhs) {
    CellWiseOps.sub_(this, rhs);
  }

  /**
   * Returns an elementwise broadcast multiplication with this tensor.
   *
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor mul(@Nonnull ZTensorWrapper rhs) {
    return CellWiseOps.mul(this, rhs);
  }

  /**
   * Returns an elementwise broadcast multiplication with this tensor.
   *
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor mul(int rhs) {
    return CellWiseOps.mul(this, rhs);
  }

  /**
   * Performs an in-place elementwise broadcast multiplication on this tensor.
   *
   * <p>This tensor must be mutable.
   *
   * @param rhs the right-hand side tensor.
   */
  public void mul_(@Nonnull ZTensorWrapper rhs) {
    CellWiseOps.mul_(this, rhs);
  }

  /**
   * Performs an in-place elementwise broadcast multiplication on this tensor.
   *
   * <p>This tensor must be mutable.
   *
   * @param rhs the right-hand side tensor.
   */
  public void mul_(int rhs) {
    CellWiseOps.mul_(this, rhs);
  }

  /**
   * Returns an elementwise broadcast division with this tensor.
   *
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor div(@Nonnull ZTensorWrapper rhs) {
    return CellWiseOps.div(this, rhs);
  }

  /**
   * Returns an elementwise broadcast division with this tensor.
   *
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor div(int rhs) {
    return CellWiseOps.div(this, rhs);
  }

  /**
   * Performs an in-place elementwise broadcast division on this tensor.
   *
   * <p>This tensor must be mutable.
   *
   * @param rhs the right-hand side tensor.
   */
  public void div_(@Nonnull ZTensorWrapper rhs) {
    CellWiseOps.div_(this, rhs);
  }

  /**
   * Performs an in-place elementwise broadcast division on this tensor.
   *
   * <p>This tensor must be mutable.
   *
   * @param rhs the right-hand side tensor.
   */
  public void div_(int rhs) {
    CellWiseOps.div_(this, rhs);
  }

  /**
   * Returns an elementwise broadcast mod with this tensor.
   *
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor mod(@Nonnull ZTensorWrapper rhs) {
    return CellWiseOps.mod(this, rhs);
  }

  /**
   * Returns an elementwise broadcast mod with this tensor.
   *
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor mod(int rhs) {
    return CellWiseOps.mod(this, rhs);
  }

  /**
   * Performs an in-place elementwise broadcast mod on this tensor.
   *
   * <p>This tensor must be mutable.
   *
   * @param rhs the right-hand side tensor.
   */
  public void mod_(@Nonnull ZTensorWrapper rhs) {
    CellWiseOps.mod_(this, rhs);
  }

  /**
   * Performs an in-place elementwise broadcast mod on this tensor.
   *
   * <p>This tensor must be mutable.
   *
   * @param rhs the right-hand side tensor.
   */
  public void mod_(int rhs) {
    CellWiseOps.mod_(this, rhs);
  }

  /**
   * Returns an elementwise broadcast power with this tensor.
   *
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor pow(@Nonnull ZTensorWrapper rhs) {
    return CellWiseOps.pow(this, rhs);
  }

  /**
   * Returns an elementwise broadcast power with this tensor.
   *
   * @param rhs the right-hand side scalar.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor pow(int rhs) {
    return CellWiseOps.pow(this, rhs);
  }

  /**
   * Performs an in-place elementwise broadcast power on this tensor.
   *
   * <p>This tensor must be mutable.
   *
   * @param rhs the right-hand side tensor.
   */
  public void pow_(@Nonnull ZTensorWrapper rhs) {
    CellWiseOps.pow_(this, rhs);
  }

  /**
   * Performs an in-place elementwise broadcast power on this tensor.
   *
   * <p>This tensor must be mutable.
   *
   * @param rhs the right-hand side scalar.
   */
  public void pow_(int rhs) {
    CellWiseOps.pow_(this, rhs);
  }

  /**
   * Returns an elementwise broadcast log with this tensor.
   *
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor log(@Nonnull ZTensorWrapper rhs) {
    return CellWiseOps.log(this, rhs);
  }

  /**
   * Returns an elementwise broadcast log with this tensor.
   *
   * @param rhs the right-hand side scalar.
   * @return a new tensor.
   */
  @Nonnull
  public ZTensor log(int rhs) {
    return CellWiseOps.log(this, rhs);
  }

  /**
   * Performs an in-place elementwise broadcast log on this tensor.
   *
   * <p>This tensor must be mutable.
   *
   * @param rhs the right-hand side tensor.
   */
  public void log_(@Nonnull ZTensorWrapper rhs) {
    CellWiseOps.log_(this, rhs);
  }

  /**
   * Performs an in-place elementwise broadcast log on this tensor.
   *
   * <p>This tensor must be mutable.
   *
   * @param rhs the right-hand side scalar.
   */
  public void log_(int rhs) {
    CellWiseOps.log_(this, rhs);
  }
}
