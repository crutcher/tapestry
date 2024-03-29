package org.tensortapestry.zspace;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import javax.annotation.Nonnull;
import org.tensortapestry.zspace.indexing.IndexingFns;

public interface HasPermuteInput<T extends HasPermuteInput<T>> {
  /**
   * Get the number of input dimension.
   *
   * @return the input dimensions.
   */
  @JsonIgnore
  int getInputNDim();

  /**
   * Resolve the given permutation to the input dimensions of this object.
   *
   * @param permutation the permutation to resolve.
   * @return the resolved permutation.
   */
  @Nonnull
  default int[] resolveInputPermutation(@Nonnull int... permutation) {
    return IndexingFns.resolvePermutation(permutation, getInputNDim());
  }

  /**
   * Permute the input dimensions of this object.
   *
   * @param permutation the permutation to apply.
   * @return a new permuted object.
   */
  @Nonnull
  T permuteInput(@Nonnull int... permutation);

  /**
   * Permute the input dimensions of this object.
   *
   * @param permutation the permutation to apply.
   * @return a new permuted object.
   */
  @Nonnull
  default T permuteInput(@Nonnull List<Integer> permutation) {
    return permuteInput(permutation.stream().mapToInt(i -> i).toArray());
  }
}
