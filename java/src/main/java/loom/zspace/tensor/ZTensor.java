package loom.zspace.tensor;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.IntNode;
import com.google.errorprone.annotations.Immutable;
import loom.linear.PartialOrdering;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@ThreadSafe
@Immutable
@JsonSerialize(using = ZTensor.JsonSupport.Serializer.class)
@JsonDeserialize(using = ZTensor.JsonSupport.Deserializer.class)
public final class ZTensor {
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

    private class CoordsIterable implements Iterable<int[]> {
        @Contract(" -> new")
        @Override
        public @NotNull Iterator<int[]> iterator() {
            return new CoordsIterator();
        }

        @Contract(" -> new")
        public @NotNull Stream<int[]> stream() {
            return StreamSupport.stream(spliterator(), false);
        }
    }

    @Contract("_ -> new")
    public static @NotNull ZTensor scalar(int value) {
        return new ZTensor(new int[]{}, new int[]{value});
    }

    @Contract("_ -> new")
    public static @NotNull ZTensor vector(int @NotNull ... value) {
        return from(value);
    }

    @Contract("_ -> new")
    public static @NotNull ZTensor matrix(@NotNull int[]... rows) {
        int numRows = rows.length;

        int numCols = 0;
        if (numRows > 0) {
            numCols = rows[0].length;
        }

        int[] shape = new int[]{numRows, numCols};
        int[] data = Arrays.stream(rows).flatMapToInt(Arrays::stream).toArray();
        return new ZTensor(shape, defaultStridesForShape(shape), data);
    }

    @SuppressWarnings("Immutable")
    private final int[] shape;

    private final int size;

    @SuppressWarnings("Immutable")
    private final int[] stride;

    private final int offset;

    @SuppressWarnings("Immutable")
    private final int[] data;

    @SuppressWarnings("Immutable")
    private Integer hash;

    /**
     * Given a shape, construct the default LSC-first strides for that shape.
     *
     * <pre>
     * defaultStridesForShape(new int[]{2, 3, 4}) == new int[]{12, 4, 1}
     * </pre>
     *
     * @param shape the shape.
     * @return a new array of strides.
     */
    @Contract(pure = true)
    public static int @NotNull [] defaultStridesForShape(int @NotNull [] shape) {
        var stride = new int[shape.length];
        if (shape.length == 0) {
            return stride;
        }

        int s = 1;
        for (int i = shape.length - 1; i >= 0; --i) {
            stride[i] = s;
            s *= shape[i];
        }
        return stride;
    }

    /**
     * Given a shape, returns the number of elements that shape describes.
     *
     * @param shape the shape.
     * @return the product of the shape; which is 1 for a shape of `[]`.
     */
    @Contract(pure = true)
    private static int shapeToSize(int[] shape) {
        return Arrays.stream(shape).reduce(1, (a, b) -> a * b);
    }

    @Contract("_ -> new")
    public static @NotNull ZTensor zeros_like(@NotNull ZTensor tensor) {
        return new ZTensor(tensor.shapeAsArray());
    }

    @Contract("_ -> new")
    public static @NotNull ZTensor zeros(@NotNull int... shape) {
        return new ZTensor(shape.clone());
    }

    /**
     * Given a non-sparse array of unknown dimensionality, returns a ZTensor with the same shape and
     * data.
     *
     * @param source the source array.
     * @return a new ZTensor.
     */
    public static @NotNull ZTensor from(Object source) {
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
     * @param shape  the shape.
     * @param stride the strides.
     * @param offset the offset in the source data.
     * @param data   the data.
     */
    private ZTensor(int[] shape, int[] stride, int offset, int[] data) {
        this.shape = shape;
        this.size = shapeToSize(shape);
        this.stride = stride;
        this.offset = offset;
        this.data = data;
    }

    /**
     * Constructs a ZTensor from parts; takes ownership of the arrays.
     *
     * <p>Assumes an offset of 0.
     *
     * @param shape  the shape.
     * @param stride the strides.
     * @param data   the data.
     */
    private ZTensor(int[] shape, int[] stride, int[] data) {
        this(shape, stride, 0, data);
    }

    /**
     * Constructs a ZTensor from parts; takes ownership of the arrays.
     *
     * <p>Assumes an offset of 0, and default strides.
     *
     * @param shape the shape.
     * @param data  the data.
     */
    private ZTensor(int[] shape, int[] data) {
        this(shape, defaultStridesForShape(shape), 0, data);
    }

    /**
     * Construct a 0-filled ZTensor of the given shape; takes ownership of the shape.
     *
     * @param shape the shape.
     */
    private ZTensor(int[] shape) {
        this(shape, new int[shapeToSize(shape)]);
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
    static <T> @NotNull ZTensor fromTree(
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

        var tensor = new ZTensor(shape);

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
     * @param ndim the number of dimensions.
     */
    @Contract("_ -> fail")
    public void assertNdim(int ndim) {
        if (ndim != ndim()) {
            throw new IllegalArgumentException("expected ndim " + ndim + ", got " + ndim());
        }
    }

    /**
     * Assert that this tensor has the given shape.
     *
     * @param shape the shape.
     */
    @Contract("_ -> fail")
    public void assertShape(int... shape) {
        if (!Arrays.equals(this.shape, shape)) {
            throw new IllegalArgumentException(
                    "expected shape " + Arrays.toString(shape) + ", got " + Arrays.toString(this.shape));
        }
    }

    /**
     * Assert that this tensor has the same shape as another.
     *
     * @param other the other tensor.
     */
    @Contract("_ -> fail")
    public void assertMatchingShape(ZTensor other) {
        assertShape(other.shape);
    }

    /**
     * Assert that two tensors have the same shape.
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     */
    @Contract("_, _ -> fail")
    public static void assertMatchingShapes(@NotNull ZTensor lhs, @NotNull ZTensor rhs) {
        if (!Arrays.equals(lhs.shape, rhs.shape)) {
            throw new IllegalArgumentException(
                    String.format(
                            "Shapes don't match: %s vs %s",
                            Arrays.toString(lhs.shape), Arrays.toString(rhs.shape)));
        }
    }

    /**
     * Clone this ZTensor.
     *
     * @return a new ZTensor with the same data.
     */
    @Override
    public ZTensor clone() {
        int[] data = new int[size()];
        for (int[] coords : byCoords()) {
            data[ravel(shape, stride, coords)] = get(coords);
        }

        return new ZTensor(shape, stride, 0, data);
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
     * Returns the shape of this tensor as a ZTensor.
     *
     * @return the shape of this tensor as a ZTensor.
     */
    public ZTensor shapeAsTensor() {
        return ZTensor.vector(shape);
    }

    /**
     * Returns the number of dimensions of this tensor.
     */
    public int ndim() {
        return shape.length;
    }

    /**
     * Is this tensor a scalar?
     */
    public boolean isScalar() {
        return ndim() == 0;
    }

    /**
     * Returns the number of elements in this tensor.
     */
    public int size() {
        return size;
    }

    /**
     * Is this tensor empty?
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Returns an {@code Iterable<int[]>} over the coordinates of this tensor.
     *
     * <p>The iterable re-uses the same array for each call to {@link Iterator#next()}. The array
     * should not be mutated; it is only valid until the next call to {@link Iterator#next()}.
     *
     * @return an iterable over the coordinates of this tensor.
     */
    @Contract(value = " -> new", pure = true)
    public @NotNull CoordsIterable byCoords() {
        return new CoordsIterable();
    }

    /**
     * Returns a {@code Stream<int[]>} over the coordinates of this tensor.
     */
    @Contract(" -> new")
    public @NotNull Stream<int[]> coordsStream() {
        return byCoords().stream();
    }

    /**
     * Given shape, strides, and coords; compute the ravel index into a data array.
     *
     * @param shape  the shape array.
     * @param stride the strides array.
     * @param coords the coordinates.
     * @return the ravel index.
     */
    @Contract(pure = true)
    public static int ravel(@NotNull int[] shape, @NotNull int[] stride, @NotNull int[] coords) {
        assert shape.length == stride.length;
        assert coords.length == shape.length;

        int index = 0;
        for (int i = 0; i < shape.length; ++i) {
            index += coords[i] * stride[i];
        }
        return index;
    }

    /**
     * Compute the ravel index into the data array for the given coordinates.
     *
     * @param coords the coordinates.
     * @return the ravel index.
     */
    int ravel(int... coords) {
        return ravel(shape, stride, coords);
    }

    /**
     * Get the cell-value at the given coordinates.
     *
     * @param coords the coordinates.
     * @return the cell value.
     */
    public int get(int... coords) {
        return data[offset + ravel(shape, stride, coords)];
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
     * Namespace of ZTensor operations.
     */
    public static final class Ops {
        /**
         * Prevent instantiation.
         */
        private Ops() {
        }

        /**
         * Compute the partial ordering of two tensors.
         *
         * @param lhs the left-hand side.
         * @param rhs the right-hand side.
         * @return the partial ordering.
         */
        public static PartialOrdering partialCompare(ZTensor lhs, ZTensor rhs) {
            assertMatchingShapes(lhs, rhs);

            boolean lt = false;
            boolean gt = false;
            for (int[] coords : lhs.byCoords()) {
                int cmp = Integer.compare(lhs.get(coords), rhs.get(coords));
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
         * Are these tensors equal under partial ordering?
         *
         * @param lhs the left-hand side.
         * @param rhs the right-hand side.
         * @return true if the tensors are equal.
         */
        public static boolean eq(ZTensor lhs, ZTensor rhs) {
            return partialCompare(lhs, rhs) == PartialOrdering.EQUAL;
        }

        /**
         * Are these tensors non-equal under partial ordering?
         *
         * @param lhs the left-hand side.
         * @param rhs the right-hand side.
         * @return true if the tensors are non-equal.
         */
        public static boolean ne(ZTensor lhs, ZTensor rhs) {
            return partialCompare(lhs, rhs) != PartialOrdering.EQUAL;
        }

        /**
         * Is `lhs < rhs`?
         *
         * @param lhs the left-hand side.
         * @param rhs the right-hand side.
         * @return true or false.
         */
        public static boolean lt(ZTensor lhs, ZTensor rhs) {
            return partialCompare(lhs, rhs) == PartialOrdering.LESS_THAN;
        }

        /**
         * Is `lhs <= rhs`?
         *
         * @param lhs the left-hand side.
         * @param rhs the right-hand side.
         * @return true or false.
         */
        public static boolean le(ZTensor lhs, ZTensor rhs) {
            var ordering = partialCompare(lhs, rhs);
            return ordering == PartialOrdering.LESS_THAN || ordering == PartialOrdering.EQUAL;
        }

        /**
         * Is `lhs > rhs`?
         *
         * @param lhs the left-hand side.
         * @param rhs the right-hand side.
         * @return true or false.
         */
        public static boolean gt(ZTensor lhs, ZTensor rhs) {
            return partialCompare(lhs, rhs) == PartialOrdering.GREATER_THAN;
        }

        /**
         * Is `lhs >= rhs`?
         *
         * @param lhs the left-hand side.
         * @param rhs the right-hand side.
         * @return true or false.
         */
        public static boolean ge(ZTensor lhs, ZTensor rhs) {
            var ordering = partialCompare(lhs, rhs);
            return ordering == PartialOrdering.GREATER_THAN || ordering == PartialOrdering.EQUAL;
        }

        /**
         * An element-wise unary operation.
         *
         * @param op     the operation.
         * @param tensor the input tensor.
         * @return a new tensor.
         */
        public static @NotNull ZTensor uniOp(IntFunction<Integer> op, ZTensor tensor) {
            // Mutate-in-place is naughty, but no one has seen the tensor yet.
            var result = zeros_like(tensor);
            for (int[] coords : tensor.byCoords()) {
                result.data[result.ravel(coords)] = op.apply(tensor.get(coords));
            }
            return result;
        }

        /**
         * Negate a tensor.
         *
         * @param tensor the input tensor.
         * @return a new tensor.
         */
        public static @NotNull ZTensor neg(ZTensor tensor) {
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
        public static @NotNull ZTensor binOp(BinaryOperator<Integer> op, ZTensor lhs, ZTensor rhs) {
            assertMatchingShapes(lhs, rhs);
            // Mutate-in-place is naughty, but no one has seen the tensor yet.
            var result = zeros_like(lhs);
            for (int[] coords : lhs.byCoords()) {
                result.data[result.ravel(coords)] = op.apply(lhs.get(coords), rhs.get(coords));
            }
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
        public static @NotNull ZTensor binOp(BinaryOperator<Integer> op, ZTensor lhs, int rhs) {
            // Mutate-in-place is naughty, but no one has seen the tensor yet.
            var result = zeros_like(lhs);
            for (int[] coords : lhs.byCoords()) {
                result.data[result.ravel(coords)] = op.apply(lhs.get(coords), rhs);
            }
            return result;
        }

        /**
         * An element-wise binary operation.
         *
         * @param op  the operation.
         * @param lhs the left-hand side scalar.
         * @param rhs the right-hand side tensor.
         * @return a new tensor.
         */
        public static @NotNull ZTensor binOp(BinaryOperator<Integer> op, int lhs, ZTensor rhs) {
            // Mutate-in-place is naughty, but no one has seen the tensor yet.
            var result = zeros_like(rhs);
            for (int[] coords : rhs.byCoords()) {
                result.data[result.ravel(coords)] = op.apply(lhs, rhs.get(coords));
            }
            return result;
        }

        public static @NotNull ZTensor add(ZTensor lhs, ZTensor rhs) {
            return binOp(Integer::sum, lhs, rhs);
        }

        public static @NotNull ZTensor add(ZTensor lhs, int rhs) {
            return binOp(Integer::sum, lhs, rhs);
        }

        public static @NotNull ZTensor add(int lhs, ZTensor rhs) {
            return binOp(Integer::sum, lhs, rhs);
        }

        public static @NotNull ZTensor sub(ZTensor lhs, ZTensor rhs) {
            return binOp((l, r) -> l - r, lhs, rhs);
        }

        public static @NotNull ZTensor sub(ZTensor lhs, int rhs) {
            return binOp((l, r) -> l - r, lhs, rhs);
        }

        public static @NotNull ZTensor sub(int lhs, ZTensor rhs) {
            return binOp((l, r) -> l - r, lhs, rhs);
        }

        public static @NotNull ZTensor mul(ZTensor lhs, ZTensor rhs) {
            return binOp((l, r) -> l * r, lhs, rhs);
        }

        public static @NotNull ZTensor mul(ZTensor lhs, int rhs) {
            return binOp((l, r) -> l * r, lhs, rhs);
        }

        public static @NotNull ZTensor mul(int lhs, ZTensor rhs) {
            return binOp((l, r) -> l * r, lhs, rhs);
        }

        public static @NotNull ZTensor div(ZTensor lhs, ZTensor rhs) {
            return binOp((l, r) -> l / r, lhs, rhs);
        }

        public static @NotNull ZTensor div(ZTensor lhs, int rhs) {
            return binOp((l, r) -> l / r, lhs, rhs);
        }

        public static @NotNull ZTensor div(int lhs, ZTensor rhs) {
            return binOp((l, r) -> l / r, lhs, rhs);
        }

        public static @NotNull ZTensor mod(ZTensor lhs, ZTensor rhs) {
            return binOp((l, r) -> l % r, lhs, rhs);
        }

        public static @NotNull ZTensor mod(ZTensor lhs, int rhs) {
            return binOp((l, r) -> l % r, lhs, rhs);
        }

        public static @NotNull ZTensor mod(int lhs, ZTensor rhs) {
            return binOp((l, r) -> l % r, lhs, rhs);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof ZTensor) {
            return eq((ZTensor) other);
        } else {
            return false;
        }
    }

    @Override
    @SuppressWarnings("Immutable")
    public int hashCode() {
        if (hash == null) {
            hash = coordsStream().mapToInt(this::get).reduce(0, (h, v) -> h * 31 + v);
        }
        return hash;
    }

    public boolean eq(ZTensor rhs) {
        return Ops.eq(this, rhs);
    }

    public boolean ne(ZTensor rhs) {
        return Ops.ne(this, rhs);
    }

    public boolean lt(ZTensor rhs) {
        return Ops.lt(this, rhs);
    }

    public boolean le(ZTensor rhs) {
        return Ops.le(this, rhs);
    }

    public boolean gt(ZTensor rhs) {
        return Ops.gt(this, rhs);
    }

    public boolean ge(ZTensor rhs) {
        return Ops.ge(this, rhs);
    }

    public @NotNull ZTensor neg() {
        return Ops.neg(this);
    }

    public @NotNull ZTensor add(ZTensor rhs) {
        return Ops.add(this, rhs);
    }

    public @NotNull ZTensor add(int rhs) {
        return Ops.add(this, rhs);
    }

    public @NotNull ZTensor sub(ZTensor rhs) {
        return Ops.sub(this, rhs);
    }

    public @NotNull ZTensor sub(int rhs) {
        return Ops.sub(this, rhs);
    }

    public @NotNull ZTensor mul(ZTensor rhs) {
        return Ops.mul(this, rhs);
    }

    public @NotNull ZTensor mul(int rhs) {
        return Ops.mul(this, rhs);
    }

    public @NotNull ZTensor div(ZTensor rhs) {
        return Ops.div(this, rhs);
    }

    public @NotNull ZTensor div(int rhs) {
        return Ops.div(this, rhs);
    }

    public @NotNull ZTensor mod(ZTensor rhs) {
        return Ops.mod(this, rhs);
    }

    public @NotNull ZTensor mod(int rhs) {
        return Ops.mod(this, rhs);
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
