package org.tensortapestry.loom.zspace;

import java.util.List;
import javax.annotation.Nonnull;

public interface HasPermuteInput<T extends HasPermuteInput<T>> {
  /**
   * Get the number of input dimension.
   *
   * @return the input dimensions.
   */
  int inputNDim();

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
