package org.tensortapestry.zspace.indexing;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

import lombok.experimental.UtilityClass;
import org.tensortapestry.zspace.HasDimension;

/**
 * Utility functions for computing tensor indices.
 */
@UtilityClass
public class IndexingFns {

    /**
     * Sum the elements of an array of integers.
     *
     * @param arr the array of integers.
     * @return the sum of the elements.
     */
    public int intSum(@Nonnull int[] arr) {
        int sum = 0;
        for (int i : arr) {
            sum += i;
        }
        return sum;
    }

    /**
     * Convert a list of integers to an array of integers.
     *
     * @param list the list of integers.
     * @return an array of integers.
     */
    @Nonnull
    public int[] unboxList(@Nonnull List<Integer> list) {
        var size = list.size();
        var res = new int[size];
        for (int i = 0; i < size; ++i) {
            res[i] = list.get(i);
        }
        return res;
    }

    /**
     * Convert an array of integers to a list of integers.
     *
     * @param arr the array of integers.
     * @return a list of integers.
     */
    @Nonnull
    public List<Integer> boxArray(@Nonnull int[] arr) {
        var size = arr.length;
        List<Integer> res = new ArrayList<>(size);
        for (int j : arr) {
            res.add(j);
        }
        return res;
    }

    /**
     * Return an array of integers from 0 to n - 1.
     *
     * @param n the number of integers to return.
     * @return an array of integers from 0 to n - 1.
     */
    @Nonnull
    public int[] iota(int n) {
        int[] result = new int[n];
        for (int i = 0; i < n; ++i) {
            result[i] = i;
        }
        return result;
    }

    /**
     * Return an array of integers from n - 1 to 0.
     *
     * @param n the number of integers to return.
     * @return an array of integers from n - 1 to 0.
     */
    @Nonnull
    public int[] aoti(int n) {
        int[] result = new int[n];
        for (int i = 0; i < n; ++i) {
            result[i] = n - 1 - i;
        }
        return result;
    }

    /**
     * Resolve a dimension index with negative indexing.
     *
     * @param msg the dimension name.
     * @param idx the index.
     * @param size the size of the bounds.
     * @return the resolved index.
     * @throws IndexOutOfBoundsException if the index is out of range.
     */
    public int resolveIndex(@Nonnull String msg, int idx, int size) {
        var res = idx;
        if (res < 0) {
            res += size;
        }
        if (res < 0 || res >= size) {
            throw new IndexOutOfBoundsException(
                    String.format("%s: index %d out of range [0, %d)", msg, idx, size)
            );
        }
        return res;
    }

    public int resolveIndex(@Nonnull String msg, int idx, int size, int bounds) {
        var res = idx;

        if (idx >= bounds || idx < -bounds) {
            throw new IndexOutOfBoundsException(
                    String.format("%s: index %d out of range [0, %d)", msg, idx, bounds)
            );
        }

        if (res < 0) {
            res += size;
        }
        return res;
    }

    /**
     * Resolve a dimension index.
     *
     * <p>Negative dimension indices are resolved relative to the number of dimensions.
     *
     * @param dim the dimension index.
     * @param ndim the number of dimensions.
     * @return the resolved dimension index.
     * @throws IndexOutOfBoundsException if the index is out of range.
     */
    public int resolveDim(int dim, int ndim) {
        return resolveIndex("invalid dimension", dim, ndim);
    }

    /**
     * Resolve a dimension index.
     *
     * <p>Negative dimension indices are resolved relative to the number of dimensions.
     *
     * @param dim the dimension index.
     * @param shape the shape of the tensor.
     * @return the resolved dimension index.
     * @throws IndexOutOfBoundsException if the index is out of range.
     */
    public int resolveDim(int dim, @Nonnull int[] shape) {
        return resolveDim(dim, shape.length);
    }

    /**
     * Resolve dimension indexes.
     *
     * <p>Negative dimension indices are resolved relative to the number of dimensions.
     *
     * @param dims the dimension indexes.
     * @param shape the shape of the tensor.
     * @return the resolved dimension indexes.
     * @throws IndexOutOfBoundsException if the index is out of range.
     * @throws IllegalArgumentException if the indexes are duplicated.
     */
    @Nonnull
    public int[] resolveDims(@Nonnull int[] dims, @Nonnull int[] shape) {
        int[] res = new int[dims.length];
        for (int i = 0; i < dims.length; ++i) {
            var d = resolveDim(dims[i], shape);
            res[i] = d;
        }
        for (int i = 0; i < dims.length; ++i) {
            var d = res[i];
            for (int k = 0; k < i; ++k) {
                if (res[k] == d) {
                    // Duplicate dimensions detected.
                    throw new IllegalArgumentException(
                            "dims %s resolve to %s on shape %s with duplicate dimensions".formatted(
                                    Arrays.toString(dims),
                                    Arrays.toString(res),
                                    Arrays.toString(shape)
                            )
                    );
                }
            }
        }
        return res;
    }

    /**
     * Given a permutation with potentially negative indexes; resolve to positive indexes.
     *
     * @param permutation a permutation; supports negative indexes.
     * @param ndim the number of dimensions.
     * @return a resolved permutation of non-negative indices.
     * @throws IllegalArgumentException if the permutation is invalid.
     * @throws IndexOutOfBoundsException if a dimension is out of range.
     */
    @Nonnull
    public int[] resolvePermutation(@Nonnull int[] permutation, int ndim) {
        HasDimension.assertNDim(permutation.length, ndim);

        int acc = 0;
        int[] perm = new int[ndim];
        for (int idx = 0; idx < ndim; ++idx) {
            int d = resolveDim(permutation[idx], ndim);
            perm[idx] = d;
            acc += d;
        }
        if (acc != (ndim * (ndim - 1)) / 2) {
            throw new IllegalArgumentException("invalid permutation: " + Arrays.toString(permutation));
        }
        return perm;
    }

    /**
     * Apply a permutation to an array; resolving negative indexes. Given a permutation with
     * potentially negative indexes; resolve to positive indexes.
     *
     * @param arr the array.
     * @param permutation the permutation.
     * @return a new array with the permutation applied.
     */
    @Nonnull
    public int[] permute(@Nonnull int[] arr, @Nonnull int[] permutation) {
        var per = resolvePermutation(permutation, arr.length);
        return applyResolvedPermutation(arr, per);
    }

    /**
     * Check the length of an array and a permutation.
     *
     * @param arr the array
     * @param permutation the permutation
     * @return the shared length
     */
    private int _checkResolvedPermutation(@Nonnull Object arr, @Nonnull int[] permutation) {
        var length = Array.getLength(arr);

        if (length != permutation.length) {
            throw new IllegalArgumentException(
                    "array length %d and permutation length %d must be equal".formatted(
                            length,
                            permutation.length
                    )
            );
        }

        return length;
    }

    /**
     * Apply a resolved permutation to an array.
     *
     * @param arr the array.
     * @param permutation the permutation.
     * @param <T> the type of the array.
     * @return a new array with the permutation applied.
     */
    @Nonnull
    public <T> T[] applyResolvedPermutation(@Nonnull T[] arr, @Nonnull int[] permutation) {
        int length = _checkResolvedPermutation(arr, permutation);
        var res = arr.clone();
        for (int i = 0; i < length; ++i) {
            res[i] = arr[permutation[i]];
        }
        return res;
    }

    /**
     * Apply a resolved permutation to an array.
     *
     * @param arr the array.
     * @param permutation the permutation.
     * @return a new array with the permutation applied.
     */
    @Nonnull
    public int[] applyResolvedPermutation(@Nonnull int[] arr, @Nonnull int[] permutation) {
        int length = _checkResolvedPermutation(arr, permutation);
        var res = arr.clone();
        for (int i = 0; i < length; ++i) {
            res[i] = arr[permutation[i]];
        }
        return res;
    }

    /**
     * Given shape, strides, and coords; compute the ravel index into a data array.
     *
     * @param shape the shape array.
     * @param stride the strides array.
     * @param coords the coordinates.
     * @param offset the offset into the data array.
     * @return the ravel index.
     * @throws IndexOutOfBoundsException if the coordinates are out of bounds.
     */
    public int ravel(@Nonnull int[] shape, @Nonnull int[] stride, @Nonnull int[] coords, int offset) {
        var ndim = shape.length;
        if (stride.length != ndim) {
            throw new IllegalArgumentException(
                    "shape %s and stride %s must have the same dimensions".formatted(
                            Arrays.toString(shape),
                            Arrays.toString(stride)
                    )
            );
        }
        if (coords.length != ndim) {
            throw new IllegalArgumentException(
                    "shape %s and coords %s must have the same dimensions".formatted(
                            Arrays.toString(shape),
                            Arrays.toString(coords)
                    )
            );
        }

        int index = offset;
        for (int i = 0; i < shape.length; ++i) {
            try {
                index += resolveIndex("coord", coords[i], shape[i]) * stride[i];
            } catch (IndexOutOfBoundsException e) {
                throw new IndexOutOfBoundsException(
                        String.format(
                                "coords %s are out of bounds of shape %s",
                                Arrays.toString(coords),
                                Arrays.toString(shape)
                        )
                );
            }
        }
        return index;
    }

    /**
     * Given a shape, construct the default least-significant-coord-first strides for that shape.
     *
     * <pre>
     * defaultStridesForShape(new int[]{2, 3, 4}) == new int[]{12, 4, 1}
     * </pre>
     *
     * @param shape the shape.
     * @return a new array of strides.
     */
    @Nonnull
    public int[] shapeToLSFStrides(@Nonnull int[] shape) {
        var stride = new int[shape.length];
        if (shape.length == 0) {
            return stride;
        }

        int s = 1;
        for (int i = shape.length - 1; i >= 0; --i) {
            int k = shape[i];
            if (k < 0) {
                throw new IllegalArgumentException("shape must be non-negative: " + Arrays.toString(shape));
            }

            if (k == 0 || k == 1) {
                stride[i] = 0;
                continue;
            }

            stride[i] = s;
            s *= k;
        }

        return stride;
    }

    /**
     * Given a shape, returns the number of elements that shape describes.
     *
     * <p>Will return a size of 1 if there are no dimensions.</p>
     * <p>Will return a size of 0 if any dimension is 0.</p>
     *
     * @param shape the shape.
     * @return the product of the shape; which is 1 for a shape of `[]`.
     * @throws IllegalArgumentException if the shape contains a negative dimension.
     */
    public int shapeToSize(@Nonnull int[] shape) {
        int size = 1;
        for (int k : shape) {
            if (k < 0) {
                throw new IllegalArgumentException("shape must be non-negative: " + Arrays.toString(shape));
            }
            size *= k;
        }
        return size;
    }

    /**
     * Given multiple shapes; compute the broadcast shape.
     *
     * @param shapes the shapes.
     * @return the common broadcast shape.
     * @throws IndexOutOfBoundsException if the shapes are not compatible.
     */
    @Nonnull
    public int[] commonBroadcastShape(@Nonnull int[]... shapes) {
        int ndim = 0;
        for (int[] ints : shapes) {
            ndim = Math.max(ndim, ints.length);
        }

        int[] res = new int[ndim];
        for (int i = 0; i < ndim; ++i) res[i] = -1;

        for (int[] shape : shapes) {
            int shift = ndim - shape.length;
            for (int d = shift; d < ndim; ++d) {
                var size = shape[d - shift];

                if (res[d] == size) {
                    continue;
                }

                if (res[d] == 1 || res[d] == -1) {
                    res[d] = size;
                    continue;
                }

                throw new IndexOutOfBoundsException(
                        "cannot broadcast shapes: " +
                        Arrays.stream(shapes).map(Arrays::toString).collect(Collectors.joining(", "))
                );
            }
        }

        return res;
    }

    /**
     * Does the given array contain the given value?
     *
     * @param array the array.
     * @param value the value.
     * @return true if the array contains the value.
     */
    public boolean arrayContains(@Nonnull int[] array, int value) {
        for (int v : array) {
            if (v == value) {
                return true;
            }
        }
        return false;
    }

    /**
     * Copy the given array, removing the given index.
     *
     * @param arr the array.
     * @param index the index to remove.
     * @return a copy of the array with the given index removed.
     */
    @Nonnull
    public int[] removeIdx(@Nonnull int[] arr, int index) {
        int[] res = new int[arr.length - 1];
        System.arraycopy(arr, 0, res, 0, index);
        System.arraycopy(arr, index + 1, res, index, arr.length - index - 1);
        return res;
    }

    /**
     * Copy an array, adding a value at the given index.
     *
     * @param arr the array.
     * @param index the index to add the value.
     * @param value the value to add.
     * @return a copy of the array with the given value added at the given index.
     */
    @Nonnull
    public int[] addIdx(@Nonnull int[] arr, int index, int value) {
        int[] res = new int[arr.length + 1];
        System.arraycopy(arr, 0, res, 0, index);
        res[index] = value;
        System.arraycopy(arr, index, res, index + 1, arr.length - index);
        return res;
    }

    /**
     * Assert that this tensor has the given shape.
     *
     * @param actual the actual shape.
     * @param expected the expected shape.
     * @throws IllegalStateException if the shapes do not match.
     */
    public void assertShape(@Nonnull int[] actual, @Nonnull int[] expected) {
        if (!Arrays.equals(actual, expected)) {
            throw new IllegalArgumentException(
                    "shape " + Arrays.toString(actual) + " != expected shape " + Arrays.toString(expected)
            );
        }
    }

    /**
     * Returns the shape of the tree as seen from the spine.
     *
     * @param <T> the type of the tree.
     * @param root the root of the tree.
     * @param isArray is this node an array, or a scalar?
     * @param getArrayLength get the length of this array.
     * @param getArrayElement get the ith element of this array.
     * @return the shape of the tree as seen from the spine.
     */
    public <T> List<Integer> treeSpineShape(
            @Nonnull T root,
            @Nonnull Predicate<T> isArray,
            @Nonnull ToIntFunction<T> getArrayLength,
            @Nonnull BiFunction<T, Integer, T> getArrayElement
    ) {
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
        return shapeList;
    }

    /**
     * Decode a non-ragged recursive int array from a tree.
     *
     * @param <T> the type of the tree.
     * @param root the root of the tree.
     * @param isArray is this node an array, or a scalar?
     * @param getArrayLength get the length of this array.
     * @param getArrayElement get the ith element of this array.
     * @param nodeAsScalar get the value of this scalar.
     * @param nodeAsSimpleArray get a coherent chunk of data for a final layer array.
     * @return a {@code Pair<int[], int[]>} of (shape, data)
     */
    public <T> @Nonnull FlatArray arrayFromTree(
            @Nonnull T root,
            @Nonnull Predicate<T> isArray,
            @Nonnull ToIntFunction<T> getArrayLength,
            @Nonnull BiFunction<T, Integer, T> getArrayElement,
            @Nonnull ToIntFunction<T> nodeAsScalar,
            @Nonnull Function<T, int[]> nodeAsSimpleArray
    ) {
        try {
            if (!isArray.test(root)) {
                return new FlatArray(new int[]{}, new int[]{nodeAsScalar.applyAsInt(root)});
            }

            var shapeList = treeSpineShape(root, isArray, getArrayLength, getArrayElement);

            var ndim = shapeList.size();

            if (shapeList.contains(0)) {
                // Handle degenerate tensors.
                return new FlatArray(new int[ndim], new int[]{});
            }

            int[] shape = shapeList.stream().mapToInt(i -> i).toArray();
            int[] data = new int[shapeToSize(shape)];

            int chunkCount = 0;
            int chunkStride = shape[ndim - 1];

            for (int[] coords : new IterableCoordinates(BufferOwnership.REUSED, shape)) {
                if (coords[ndim - 1] != 0) continue;

                var it = root;
                for (int d = 0; d < ndim - 1; ++d) {
                    it = getArrayElement.apply(it, coords[d]);
                }

                int[] chunk = nodeAsSimpleArray.apply(it);

                System.arraycopy(chunk, 0, data, chunkCount * chunkStride, chunkStride);
                chunkCount++;
            }

            return new FlatArray(shape, data);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not parse array from tree", e);
        }
    }

    /**
     * Is this object a recursive int array?
     *
     * @param source the object.
     * @return true if this object is a recursive int array.
     */
    public boolean isRecursiveIntArray(@Nonnull Object source) {
        var clazz = source.getClass();
        while (true) {
            if (clazz.isArray()) {
                clazz = clazz.getComponentType();
                continue;
            }

            return clazz == int.class || clazz == Integer.class;
        }
    }
}
