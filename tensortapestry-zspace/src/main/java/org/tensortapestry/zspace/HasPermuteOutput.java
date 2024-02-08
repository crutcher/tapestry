package org.tensortapestry.zspace;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import javax.annotation.Nonnull;
import org.tensortapestry.zspace.indexing.IndexingFns;

public interface HasPermuteOutput<T extends HasPermuteOutput<T>> {
  /**
   * Get the number of output dimension.
   *
   * @return the output dimensions.
   */
  @JsonIgnore
  int getOutputNDim();

  /**
   * Resolve the given permutation to the output dimensions of this object.
   *
   * @param permutation the permutation to resolve.
   * @return the resolved permutation.
   */
  @Nonnull
  default int[] resolveOutputPermutation(@Nonnull int... permutation) {
    return IndexingFns.resolvePermutation(permutation, getOutputNDim());
  }

  /**
   * Permute the output dimensions of this object.
   *
   * @param permutation the permutation to apply.
   * @return a new permuted object.
   */
  @Nonnull
  T permuteOutput(@Nonnull int... permutation);

  /**
   * Permute the input dimensions of this object.
   *
   * @param permutation the permutation to apply.
   * @return a new permuted object.
   */
  @Nonnull
  default T permuteOutput(@Nonnull List<Integer> permutation) {
    return permuteOutput(permutation.stream().mapToInt(i -> i).toArray());
  }
}
