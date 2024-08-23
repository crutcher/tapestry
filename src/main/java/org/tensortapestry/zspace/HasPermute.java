package org.tensortapestry.zspace;

import java.util.List;
import javax.annotation.Nonnull;
import org.tensortapestry.zspace.indexing.IndexingFns;

/**
 * Interface for objects that can be permuted by dimension index.
 *
 * @param <T> the type of the object.
 */
public interface HasPermute<T extends HasPermute<T>> extends HasDimension {
  /**
   * Resolve the given permutation to the dimensions of this object.
   *
   * @param permutation the permutation to resolve.
   * @return the resolved permutation.
   */
  default int[] resolvePermutation(@Nonnull int... permutation) {
    return IndexingFns.resolvePermutation(permutation, getNDim());
  }

  /**
   * Permute the dimensions of this object.
   *
   * <p>A permutation is an ordered list of distinct integers, each of which is in the range
   * {@code
   * [0, ndim)}.
   *
   * @param permutation the permutation to apply.
   * @return the permuted object.
   */
  @Nonnull
  T permute(@Nonnull int... permutation);

  /**
   * Permute the dimensions of this object.
   *
   * <p>A permutation is an ordered list of distinct integers, each of which is in the range
   * {@code
   * [0, ndim)}.
   *
   * @param permutation the permutation to apply.
   * @return the permuted object.
   */
  @Nonnull
  default T permute(@Nonnull List<Integer> permutation) {
    return permute(permutation.stream().mapToInt(i -> i).toArray());
  }
}
