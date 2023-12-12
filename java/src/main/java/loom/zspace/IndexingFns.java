package loom.zspace;

import java.util.Arrays;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

/** Utility functions for computing tensor indices. */
public final class IndexingFns {
  // Prevent instantiation.
  private IndexingFns() {}

  /**
   * Return an array of integers from 0 to n - 1.
   *
   * @param n the number of integers to return.
   * @return an array of integers from 0 to n - 1.
   */
  static int[] iota(int n) {
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
  static int[] aoti(int n) {
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
  static int resolveIndex(String msg, int idx, int size) {
    var res = idx;
    if (res < 0) {
      res += size;
    }
    if (res < 0 || res >= size) {
      throw new IndexOutOfBoundsException(
          String.format("%s: index %d out of range [0, %d)", msg, idx, size));
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
  public static int resolveDim(int dim, int ndim) {
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
  public static int resolveDim(int dim, int[] shape) {
    return resolveDim(dim, shape.length);
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
  public static int[] resolvePermutation(int[] permutation, int ndim) {
    ZTensor.assertNdim(permutation.length, ndim);

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
   * Given shape, strides, and coords; compute the ravel index into a data array.
   *
   * @param shape the shape array.
   * @param stride the strides array.
   * @param coords the coordinates.
   * @return the ravel index.
   * @throws IndexOutOfBoundsException if the coordinates are out of bounds.
   */
  public static int ravel(@Nonnull int[] shape, @Nonnull int[] stride, @Nonnull int[] coords) {
    var ndim = shape.length;
    if (stride.length != ndim) {
      throw new IllegalArgumentException(
          "shape %s and stride %s must have the same dimensions"
              .formatted(Arrays.toString(shape), Arrays.toString(stride)));
    }
    if (coords.length != ndim) {
      throw new IllegalArgumentException(
          "shape %s and coords %s must have the same dimensions"
              .formatted(Arrays.toString(shape), Arrays.toString(coords)));
    }

    int index = 0;
    for (int i = 0; i < shape.length; ++i) {
      try {
        index += resolveIndex("coord", coords[i], shape[i]) * stride[i];
      } catch (IndexOutOfBoundsException e) {
        throw new IndexOutOfBoundsException(
            String.format(
                "coords %s are out of bounds of shape %s",
                Arrays.toString(coords), Arrays.toString(shape)));
      }
    }
    return index;
  }

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
  public static @Nonnull int[] shapeToLSFStrides(@Nonnull int[] shape) {
    var stride = new int[shape.length];
    if (shape.length == 0) {
      return stride;
    }

    int s = 1;
    for (int i = shape.length - 1; i >= 0; --i) {
      int k = shape[i];
      if (k < 0) {
        throw new IllegalArgumentException("shape must be non-negative");
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
   * @param shape the shape.
   * @return the product of the shape; which is 1 for a shape of `[]`.
   */
  static int shapeToSize(int[] shape) {
    return Arrays.stream(shape).reduce(1, (a, b) -> a * b);
  }

  /**
   * Given multiple shapes; compute the broadcast shape.
   *
   * @param shapes the shapes.
   * @return the common broadcast shape.
   * @throws IndexOutOfBoundsException if the shapes are not compatible.
   */
  public static int[] commonBroadcastShape(int[]... shapes) {
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
            "cannot broadcast shapes: "
                + Arrays.stream(shapes).map(Arrays::toString).collect(Collectors.joining(", ")));
      }
    }

    return res;
  }
}
