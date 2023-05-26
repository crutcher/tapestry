package loom.zspace;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.IntNode;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import loom.common.HasToJsonString;
import loom.common.JsonUtil;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
public final class ZTensor implements HasDimension, HasToJsonString, HasPermute, HasSize {
    private class CoordsIterator implements Iterator<int[]> {
        // Assuming a non-scalar ZTensor; non-empty ZTensor.
        private int remaining = size();
        private int[] coords;

        @Override
        public boolean hasNext() {
            return remaining > 0;
        }

        @Override
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

            return coords;
        }
    }

    public class CoordsIterable implements Iterable<int[]> {
        @Override
        public @Nonnull Iterator<int[]> iterator() {
            return new CoordsIterator();
        }

        public @Nonnull Stream<int[]> stream() {
            return StreamSupport.stream(spliterator(), false);
        }
    }

    /**
     * Construct a new mutable scalar (0-dim) tensor.
     *
     * @param value the scalar value.
     * @return the new tensor.
     */
    public static @Nonnull ZTensor scalar(int value) {
        return new ZTensor(true, new int[]{}, new int[]{value});
    }

    /**
     * Construct a new mutable vector (1-dim) tensor.
     *
     * @param value the vector values.
     * @return the new tensor.
     */
    public static @Nonnull ZTensor vector(@Nonnull int... value) {
        return from(value);
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

        int[] shape = new int[]{numRows, numCols};
        int[] data = Arrays.stream(rows).flatMapToInt(Arrays::stream).toArray();
        return new ZTensor(true, shape, Indexing.shapeToLSFStrides(shape), data);
    }

    private final boolean mutable;
    private final int hash;

    @Nonnull
    private final int[] shape;
    @Nonnull
    private final ImmutableList<Integer> shapeList;

    private final int size;

    @Nonnull
    private final int[] stride;

    private final int offset;

    @Nonnull
    private final int[] data;

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
     * Construct a new mutable tensor filled with zeros.
     *
     * @param shape the shape of the tensor.
     * @return a new mutable ZTensor.
     */
    public static @Nonnull ZTensor zeros(@Nonnull int... shape) {
        return new ZTensor(true, shape.clone());
    }

    /**
     * Construct a new mutable tensor filled with the given fill value.
     *
     * @param shape      the shape of the tensor.
     * @param fill_value the value to fill the tensor with.
     * @return a new mutable ZTensor.
     */
    public static @Nonnull ZTensor full(@Nonnull int[] shape, int fill_value) {
        var tensor = zeros(shape);
        tensor.fill(fill_value);
        return tensor;
    }

    /**
     * Construct a new mutable ZTensor filled with the fill value with a shape like the given ZTensor.
     *
     * @param ref        the ZTensor to copy the shape from.
     * @param fill_value the value to fill the tensor with.
     * @return a new mutable ZTensor.
     */
    public static @Nonnull ZTensor full_like(ZTensor ref, int fill_value) {
        var tensor = zeros_like(ref);
        tensor.fill(fill_value);
        return tensor;
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
    public static @Nonnull ZTensor ones_like(ZTensor ref) {
        return full_like(ref, 1);
    }

    /**
     * Construct a diagonal matrix from a list of values.
     *
     * @param diag the values to put on the diagonal.
     * @return a new ZTensor.
     */
    public static ZTensor diagonal(int... diag) {
        var tensor = zeros(diag.length, diag.length);
        for (int i = 0; i < diag.length; ++i) {
            tensor._set(new int[]{i, i}, diag[i]);
        }
        return tensor;
    }

    /**
     * Construct an identity matrix of size nxn.
     *
     * @param n the size of a side of the matrix.
     * @return a new ZTensor.
     */
    public static ZTensor identity(int n) {
        int[] diag = new int[n];
        for (int i = 0; i < n; ++i) {
            diag[i] = 1;
        }
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

    /**
     * Constructs a ZTensor from parts; takes ownership of the arrays.
     *
     * @param mutable whether the ZTensor is mutable.
     * @param shape   the shape.
     * @param stride  the strides.
     * @param data    the data.
     * @param offset  the offset in the source data.
     */
    private ZTensor(boolean mutable, int[] shape, int[] stride, int[] data, int offset) {
        this.shape = shape;
        this.shapeList = ImmutableList.copyOf(Ints.asList(shape));
        this.size = Indexing.shapeToSize(shape);
        this.stride = stride;
        this.offset = offset;
        this.data = data;

        this.mutable = mutable;
        if (mutable) {
            // Hash is only well-defined for immutable ZTensors.
            hash = -1;
        } else {
            hash =
                    coordsStream().mapToInt(this::get).reduce(0, (h, v) -> h * 31 + v)
                            + Arrays.hashCode(this.shape);
        }
    }

    /**
     * Constructs a ZTensor from parts; takes ownership of the arrays.
     *
     * <p>Assumes an offset of 0.
     *
     * @param mutable whether the ZTensor is mutable.
     * @param shape   the shape.
     * @param stride  the strides.
     * @param data    the data.
     */
    private ZTensor(boolean mutable, int[] shape, int[] stride, int[] data) {
        this(mutable, shape, stride, data, 0);
    }

    /**
     * Constructs a ZTensor from parts; takes ownership of the arrays.
     *
     * <p>Assumes an offset of 0, and default strides.
     *
     * @param mutable whether the ZTensor is mutable.
     * @param shape   the shape.
     * @param data    the data.
     */
    private ZTensor(boolean mutable, int[] shape, int[] data) {
        this(mutable, shape, Indexing.shapeToLSFStrides(shape), data);
    }

    /**
     * Construct a 0-filled ZTensor of the given shape; takes ownership of the shape.
     *
     * @param mutable whether the ZTensor is mutable.
     * @param shape   the shape.
     */
    private ZTensor(boolean mutable, int[] shape) {
        this(mutable, shape, new int[Indexing.shapeToSize(shape)]);
    }

    /**
     * Is this ZTensor mutable?
     *
     * @return true if mutable; false otherwise.
     */
    public boolean isMutable() {
        return mutable;
    }

    /**
     * Is this ZTensor read-only / immutable?
     *
     * @return true if read-only / immutable; false otherwise.
     */
    public boolean isReadOnly() {
        return !mutable;
    }

    /**
     * Asserts that this ZTensor is mutable.
     */
    public void assertMutable() {
        if (!mutable) {
            throw new IllegalStateException("ZTensor is immutable");
        }
    }

    /**
     * Asserts that this ZTensor is read-only / immutable.
     */
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
     * <p>A performance oriented Tensor library would track open mutable views of the underlying data,
     * and perform copy-on-write when necessary; as this is a correctness-oriented Tensor library, we
     * simply clone the data to go from mutable to immutable.
     *
     * @return an immutable ZTensor.
     */
    public ZTensor immutable() {
        if (!mutable) {
            return this;
        }
        return clone(false);
    }

    /**
     * Given a tree datastructure representing a tensor of unknown dimensionality, returns a ZTensor.
     *
     * @param <T>             the type of the tree.
     * @param root            the root of the tree.
     * @param isArray         is this node an array, or a scalar?
     * @param getArrayLength  get the length of this array.
     * @param getArrayElement get the ith element of this array.
     * @param scalarValue     get the value of this scalar.
     * @param getChunk        get a coherent chunk of data for a final layer array.
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
            return ZTensor.scalar(scalarValue.apply(root));
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
            return ZTensor.zeros(new int[ndim]);
        }

        int[] shape = shapeList.stream().mapToInt(i -> i).toArray();

        var tensor = new ZTensor(true, shape);

        int chunkCount = 0;
        int chunkStride = tensor.shape[ndim - 1];

        for (int[] coords : tensor.byCoords()) {
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
     * @param startArray  start an array.
     * @param endArray    end an array.
     * @param elemSep     write an element separator.
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

        for (int[] coords : byCoords()) {
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
        for (var coords : byCoords()) {
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
        for (int[] coords : byCoords()) {
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
     * @param actualNdim   the actual number of dimensions.
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
     * @param actual   the actual shape.
     * @param expected the expected shape.
     * @throws IllegalStateException if the shapes do not match.
     */
    public static void assertShape(int[] actual, int[] expected) {
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
    public void assertShape(int... shape) {
        assertShape(this.shape, shape);
    }

    /**
     * Assert that this tensor has the same shape as another.
     *
     * @param other the other tensor.
     */
    public void assertMatchingShape(ZTensor other) {
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
     * @return a new ZTensor with the same data.
     */
    @Override
    public ZTensor clone() {
        return clone(mutable);
    }

    /**
     * Clone this ZTensor.
     *
     * @param mutable whether the clone should be mutable.
     * @return a new ZTensor with the same data.
     */
    public ZTensor clone(boolean mutable) {
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
    public ImmutableList<Integer> shapeAsList() {
        return shapeList;
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
        return Indexing.resolveDim(dim, shape);
    }

    /**
     * Returns the number of elements in this tensor.
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * Are all cells in this tensor > 0?
     */
    public boolean isStrictlyPositive() {
        for (var c : byCoords()) {
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
    public void forEachItem(BiConsumer<int[], Integer> consumer) {
        for (int[] coords : byCoords()) {
            consumer.accept(coords, get(coords));
        }
    }

    /**
     * Returns an {@code Iterable<int[]>} over the coordinates of this tensor.
     *
     * <p>The iterable re-uses the same array for each call to {@link Iterator#next()}. The array
     * should not be mutated; it is only valid until the next call to {@link Iterator#next()}.
     *
     * @return an iterable over the coordinates of this tensor.
     */
    public @Nonnull CoordsIterable byCoords() {
        return new CoordsIterable();
    }

    /**
     * Returns a {@code Stream<int[]>} over the coordinates of this tensor.
     */
    public @Nonnull Stream<int[]> coordsStream() {
        return byCoords().stream();
    }

    /**
     * Returns a permuted view of this tensor.
     *
     * @param permutation the permutation (accepts negative indices).
     * @return a permuted view of this tensor.
     */
    @Override
    public ZTensor permute(int... permutation) {
        var perm = Indexing.resolvePermutation(permutation, ndim());

        int[] newShape = new int[ndim()];
        int[] newStride = new int[ndim()];
        for (int i = 0; i < ndim(); ++i) {
            newShape[i] = shape[perm[i]];
            newStride[i] = stride[perm[i]];
        }

        return new ZTensor(mutable, newShape, newStride, data, offset);
    }

    /**
     * Create a copy of this tensor with a reordered dimension.
     *
     * @param dim         the dimension to reorder.
     * @param permutation the permutation of the dimension.
     * @return a copy of this tensor with a reordered dimension.
     */
    public ZTensor reorderDim(int[] permutation, int dim) {
        var d = resolveDim(dim);
        var perm = Indexing.resolvePermutation(permutation, shape[d]);
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

        int[] perm = Indexing.iota(ndim());
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
        return permute(Indexing.aoti(ndim()));
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

        int newOffset = offset + (shape[rD] - 1) * stride[rD];

        return new ZTensor(mutable, shape, newStride, data, newOffset);
    }

    /**
     * Create a view of this tensor with an extra dimension added at index `d`.
     *
     * @param d the dimension to add.
     * @return a view of this tensor with an extra dimension added at index `d`.
     */
    public ZTensor unsqueeze(int d) {
        if (d < 0 || d > ndim()) {
            throw new IllegalArgumentException("invalid dimension: " + d);
        }

        int[] newShape = new int[ndim() + 1];
        int[] newStride = new int[ndim() + 1];

        System.arraycopy(shape, 0, newShape, 0, d);
        System.arraycopy(shape, d, newShape, d + 1, ndim() - d);

        System.arraycopy(stride, 0, newStride, 0, d);
        System.arraycopy(stride, d, newStride, d + 1, ndim() - d);

        newShape[d] = 1;
        newStride[d] = 0;

        return new ZTensor(mutable, newShape, newStride, data, offset);
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

        int[] newShape = new int[ndim() - 1];
        System.arraycopy(shape, 0, newShape, 0, rD);
        System.arraycopy(shape, rD + 1, newShape, rD, ndim() - rD - 1);

        int[] newStride = new int[ndim() - 1];
        System.arraycopy(stride, 0, newStride, 0, rD);
        System.arraycopy(stride, rD + 1, newStride, rD, ndim() - rD - 1);

        return new ZTensor(mutable, newShape, newStride, data, offset);
    }

    /**
     * Return a view of this tensor with a broadcastable dimension expanded.
     *
     * @param dim  the dimension to expand (must be size 1, or a previously broadcasted dimension).
     * @param size the new size of the dimension.
     * @return a view of this tensor with a broadcastable dimension expanded.
     */
    public ZTensor broadcastDim(int dim, int size) {
        dim = resolveDim(dim);
        if (stride[dim] != 0) {
            throw new IllegalArgumentException("dimension " + dim + " is not broadcastable");
        }

        var new_shape = shape.clone();
        new_shape[dim] = size;

        var new_stride = stride.clone();
        new_stride[dim] = 0;

        return new ZTensor(mutable, new_shape, new_stride, data, offset);
    }

    @JsonIgnore
    public boolean isBroadcastDim(int dim) {
        dim = resolveDim(dim);
        return shape[dim] > 1 && stride[dim] == 0;
    }

    public ZTensor broadcastLike(ZTensor ref) {
        return broadcastTo(ref.shape);
    }

    public ZTensor broadcastTo(int... targetShape) {
        if (Arrays.equals(shape, targetShape)) {
            return this;
        }

        if (isScalar() && Indexing.shapeToSize(targetShape) == 0) {
            return new ZTensor(mutable, targetShape);
        }

        var res = this;
        if (res.ndim() > targetShape.length) {
            throw new IllegalArgumentException(
                    "cannot broadcast shape "
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
                        "cannot broadcast shape "
                                + Arrays.toString(res.shape)
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
    int ravel(int... coords) {
        return offset + Indexing.ravel(shape, stride, coords);
    }

    /**
     * Get the cell-value at the given coordinates.
     *
     * @param coords the coordinates.
     * @return the cell value.
     */
    public int get(int... coords) {
        return data[ravel(coords)];
    }

    /**
     * Return a view of this tensor with the given dimension selected.
     *
     * @param dim   the dimension to select.
     * @param index the index to select.
     * @return a view of this tensor with the given dimension selected.
     */
    public ZTensor selectDim(int dim, int index) {
        var d = resolveDim(dim);
        var i = Indexing.resolveIndex("index", index, shape[d]);

        var new_shape = shape.clone();
        new_shape[d] = 1;
        var new_stride = stride.clone();
        new_stride[d] = 0;
        int new_offset = offset + i * stride[d];

        return new ZTensor(mutable, new_shape, new_stride, data, new_offset).squeeze(d);
    }

    /**
     * Set the cell-value at the given coordinates.
     *
     * <p>Assumes the tensor is mutable.
     *
     * @param coords the coordinates.
     * @param value  the value to set.
     * @throws IndexOutOfBoundsException if the coordinates are out of bounds.
     */
    void _set(int[] coords, int value) {
        data[ravel(coords)] = value;
    }

    /**
     * Set the cell-value at the given coordinates.
     *
     * @param coords the coordinates.
     * @param value  the value to set.
     * @throws IndexOutOfBoundsException if the coordinates are out of bounds.
     * @throws IllegalStateException     if the tensor is read-only.
     */
    public void set(int[] coords, int value) {
        assertMutable();
        _set(coords, value);
    }

    /**
     * Get the value of this tensor as a T0 (a scalar).
     */
    public int item() {
        return toT0();
    }

    /**
     * Convert this structure to a T0 (a scalar) value. Assert that the shape is valid.
     */
    public int toT0() {
        assertNdim(0);
        return get();
    }

    /**
     * Convert this structure to a T1 (a vector) value. Assert that the shape is valid.
     */
    public int[] toT1() {
        assertNdim(1);
        return (int[]) toArray();
    }

    /**
     * Convert this structure to a T2 (a matrix) value. Assert that the shape is valid.
     */
    public int[][] toT2() {
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
        for (int[] coords : byCoords()) {
            _set(coords, fill_value);
        }
    }

    /**
     * Assign from a tensor.
     *
     * @param tensor the input tensor.
     */
    public void assign(ZTensor tensor) {
        assertMutable();
        tensor.broadcastTo(shape).forEachItem(this::_set);
    }

    /**
     * Assign from an element-wise unary operation.
     *
     * @param op     the operation.
     * @param tensor the input tensor.
     */
    public void assignFromMap(IntFunction<Integer> op, ZTensor tensor) {
        assertMutable();
        tensor.broadcastTo(shape).forEachItem((coords, value) -> _set(coords, op.apply(value)));
    }

    /**
     * Assign from an element-wise binary operation.
     *
     * @param op  the operation.
     * @param lhs the left-hand side tensor.
     * @param rhs the right-hand side tensor.
     */
    public void assignFromMap(BinaryOperator<Integer> op, ZTensor lhs, ZTensor rhs) {
        assertMutable();
        lhs = lhs.broadcastTo(shape);
        rhs = rhs.broadcastTo(shape);
        for (int[] coords : byCoords()) {
            _set(coords, op.apply(lhs.get(coords), rhs.get(coords)));
        }
    }

    /**
     * An in-place element-wise binary operation.
     *
     * @param op  the operation.
     * @param rhs the right-hand side tensor.
     */
    public void binOp_(BinaryOperator<Integer> op, ZTensor rhs) {
        assignFromMap(op, this, rhs);
    }

    /**
     * An in-place element-wise binary operation.
     *
     * @param op  the operation.
     * @param rhs the right-hand side scalar.
     */
    public void binOp_(BinaryOperator<Integer> op, int rhs) {
        binOp_(op, ZTensor.scalar(rhs));
    }

    /**
     * Namespace of ZTensor operations.
     */
    public static final class Ops {
        /**
         * Prevent instantiation.
         */
        private Ops() {
        }

        /**
         * An element-wise unary operation.
         *
         * @param op     the operation.
         * @param tensor the input tensor.
         * @return a new tensor.
         */
        public static @Nonnull ZTensor uniOp(IntFunction<Integer> op, ZTensor tensor) {
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
        public static @Nonnull ZTensor neg(ZTensor tensor) {
            return uniOp(x -> -x, tensor);
        }

        /**
         * An element-wise binary operation.
         *
         * @param op  the operation.
         * @param lhs the left-hand side tensor.
         * @param rhs the right-hand side tensor.
         * @return a new tensor.
         */
        public static @Nonnull ZTensor binOp(BinaryOperator<Integer> op, ZTensor lhs, ZTensor rhs) {
            var result = zeros(Indexing.commonBroadcastShape(lhs.shape, rhs.shape));
            result.assignFromMap(op, lhs, rhs);
            return result;
        }

        /**
         * An element-wise binary operation.
         *
         * @param op  the operation.
         * @param lhs the left-hand side tensor.
         * @param rhs the right-hand side scalar.
         * @return a new tensor.
         */
        public static @Nonnull ZTensor binOp(BinaryOperator<Integer> op, ZTensor lhs, int rhs) {
            return binOp(op, lhs, ZTensor.scalar(rhs));
        }

        /**
         * An element-wise binary operation.
         *
         * @param op  the operation.
         * @param lhs the left-hand side scalar.
         * @param rhs the right-hand side tensor.
         * @return a new tensor.
         */
        public static @Nonnull ZTensor binOp(BinaryOperator<Integer> op, int lhs, ZTensor rhs) {
            return binOp(op, ZTensor.scalar(lhs), rhs);
        }

        public static @Nonnull ZTensor min(ZTensor lhs, ZTensor rhs) {
            return binOp(Math::min, lhs, rhs);
        }

        public static @Nonnull ZTensor min(ZTensor lhs, int rhs) {
            return binOp(Math::min, lhs, rhs);
        }

        public static @Nonnull ZTensor min(int lhs, ZTensor rhs) {
            return binOp(Math::min, lhs, rhs);
        }

        public static @Nonnull ZTensor max(ZTensor lhs, ZTensor rhs) {
            return binOp(Math::max, lhs, rhs);
        }

        public static @Nonnull ZTensor max(ZTensor lhs, int rhs) {
            return binOp(Math::max, lhs, rhs);
        }

        public static @Nonnull ZTensor max(int lhs, ZTensor rhs) {
            return binOp(Math::max, lhs, rhs);
        }

        public static @Nonnull ZTensor add(ZTensor lhs, ZTensor rhs) {
            return binOp(Integer::sum, lhs, rhs);
        }

        public static @Nonnull ZTensor add(ZTensor lhs, int rhs) {
            return binOp(Integer::sum, lhs, rhs);
        }

        public static @Nonnull ZTensor add(int lhs, ZTensor rhs) {
            return binOp(Integer::sum, lhs, rhs);
        }

        public static void add_(ZTensor lhs, ZTensor rhs) {
            lhs.binOp_(Integer::sum, rhs);
        }

        public static void add_(ZTensor lhs, int rhs) {
            lhs.binOp_(Integer::sum, rhs);
        }

        public static @Nonnull ZTensor sub(ZTensor lhs, ZTensor rhs) {
            return binOp((l, r) -> l - r, lhs, rhs);
        }

        public static @Nonnull ZTensor sub(ZTensor lhs, int rhs) {
            return binOp((l, r) -> l - r, lhs, rhs);
        }

        public static void sub_(ZTensor lhs, ZTensor rhs) {
            lhs.binOp_((l, r) -> l - r, rhs);
        }

        public static void sub_(ZTensor lhs, int rhs) {
            lhs.binOp_((l, r) -> l - r, rhs);
        }

        public static @Nonnull ZTensor sub(int lhs, ZTensor rhs) {
            return binOp((l, r) -> l - r, lhs, rhs);
        }

        public static @Nonnull ZTensor mul(ZTensor lhs, ZTensor rhs) {
            return binOp((l, r) -> l * r, lhs, rhs);
        }

        public static @Nonnull ZTensor mul(ZTensor lhs, int rhs) {
            return binOp((l, r) -> l * r, lhs, rhs);
        }

        public static @Nonnull ZTensor mul(int lhs, ZTensor rhs) {
            return binOp((l, r) -> l * r, lhs, rhs);
        }

        public static void mul_(ZTensor lhs, ZTensor rhs) {
            lhs.binOp_((l, r) -> l * r, rhs);
        }

        public static void mul_(ZTensor lhs, int rhs) {
            lhs.binOp_((l, r) -> l * r, rhs);
        }

        public static @Nonnull ZTensor div(ZTensor lhs, ZTensor rhs) {
            return binOp((l, r) -> l / r, lhs, rhs);
        }

        public static @Nonnull ZTensor div(ZTensor lhs, int rhs) {
            return binOp((l, r) -> l / r, lhs, rhs);
        }

        public static @Nonnull ZTensor div(int lhs, ZTensor rhs) {
            return binOp((l, r) -> l / r, lhs, rhs);
        }

        public static void div_(ZTensor lhs, ZTensor rhs) {
            lhs.binOp_((l, r) -> l / r, rhs);
        }

        public static void div_(ZTensor lhs, int rhs) {
            lhs.binOp_((l, r) -> l / r, rhs);
        }

        public static @Nonnull ZTensor mod(ZTensor lhs, ZTensor rhs) {
            return binOp((l, r) -> l % r, lhs, rhs);
        }

        public static @Nonnull ZTensor mod(ZTensor lhs, int rhs) {
            return binOp((l, r) -> l % r, lhs, rhs);
        }

        public static @Nonnull ZTensor mod(int lhs, ZTensor rhs) {
            return binOp((l, r) -> l % r, lhs, rhs);
        }

        public static void mod_(ZTensor lhs, ZTensor rhs) {
            lhs.binOp_((l, r) -> l % r, rhs);
        }

        public static void mod_(ZTensor lhs, int rhs) {
            lhs.binOp_((l, r) -> l % r, rhs);
        }
    }

    public @Nonnull ZTensor neg() {
        return Ops.neg(this);
    }

    public @Nonnull ZTensor add(ZTensor rhs) {
        return Ops.add(this, rhs);
    }

    public @Nonnull ZTensor add(int rhs) {
        return Ops.add(this, rhs);
    }

    public void add_(ZTensor rhs) {
        Ops.add_(this, rhs);
    }

    public void add_(int rhs) {
        Ops.add_(this, rhs);
    }

    public @Nonnull ZTensor sub(ZTensor rhs) {
        return Ops.sub(this, rhs);
    }

    public @Nonnull ZTensor sub(int rhs) {
        return Ops.sub(this, rhs);
    }

    public void sub_(ZTensor rhs) {
        Ops.sub_(this, rhs);
    }

    public void sub_(int rhs) {
        Ops.sub_(this, rhs);
    }

    public @Nonnull ZTensor mul(ZTensor rhs) {
        return Ops.mul(this, rhs);
    }

    public @Nonnull ZTensor mul(int rhs) {
        return Ops.mul(this, rhs);
    }

    public void mul_(ZTensor rhs) {
        Ops.mul_(this, rhs);
    }

    public void mul_(int rhs) {
        Ops.mul_(this, rhs);
    }

    public @Nonnull ZTensor div(ZTensor rhs) {
        return Ops.div(this, rhs);
    }

    public @Nonnull ZTensor div(int rhs) {
        return Ops.div(this, rhs);
    }

    public void div_(ZTensor rhs) {
        Ops.div_(this, rhs);
    }

    public void div_(int rhs) {
        Ops.div_(this, rhs);
    }

    public @Nonnull ZTensor mod(ZTensor rhs) {
        return Ops.mod(this, rhs);
    }

    public @Nonnull ZTensor mod(int rhs) {
        return Ops.mod(this, rhs);
    }

    public void mod_(ZTensor rhs) {
        Ops.mod_(this, rhs);
    }

    public void mod_(int rhs) {
        Ops.mod_(this, rhs);
    }

    /**
     * Jackson Serialization Support namespace.
     *
     * <ul>
     *   scalars are serialized as a single number;
     * </ul>
     *
     * <ul>
     *   vectors are serialized as a single array;
     * </ul>
     *
     * <ul>
     *   matrices are serialized as an array of arrays;
     * </ul>
     *
     * <ul>
     *   etc.
     * </ul>
     *
     * <p>All empty tensors serialize to nested "[...]"; so all degenerate tensors (empty tensors with
     * non-zero shapes) are serialized as empty tensors.
     */
    static class JsonSupport {
        /**
         * Private constructor to prevent instantiation.
         */
        private JsonSupport() {
        }

        static class Serializer extends JsonSerializer<ZTensor> {
            @Override
            public void serialize(
                    ZTensor value,
                    com.fasterxml.jackson.core.JsonGenerator gen,
                    com.fasterxml.jackson.databind.SerializerProvider serializers)
                    throws java.io.IOException {

                value.toTree(
                        () -> {
                            try {
                                gen.writeStartArray();
                            } catch (IOException e) { // coverage: ignore
                                throw new RuntimeException(e);
                            }
                        },
                        () -> {
                            try {
                                gen.writeEndArray();
                            } catch (IOException e) { // coverage: ignore
                                throw new RuntimeException(e);
                            }
                        },
                        () -> {
                        },
                        val -> {
                            try {
                                gen.writeNumber(val);
                            } catch (IOException e) { // coverage: ignore
                                throw new RuntimeException(e);
                            }
                        });
            }
        }

        static class Deserializer extends StdDeserializer<ZTensor> {
            public Deserializer() {
                super(ZTensor.class);
            }

            @Override
            public ZTensor deserialize(
                    com.fasterxml.jackson.core.JsonParser p,
                    com.fasterxml.jackson.databind.DeserializationContext ctxt)
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