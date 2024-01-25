package org.tensortapestry.loom.zspace;

import java.util.List;
import javax.annotation.Nonnull;

public interface HasPermuteOutput<T extends HasPermuteOutput<T>> {
  /**
   * Get the number of output dimension.
   *
   * @return the output dimensions.
   */
  int outputNDim();

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
