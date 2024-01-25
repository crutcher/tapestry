package org.tensortapestry.loom.zspace;

import javax.annotation.Nonnull;

public interface HasPermuteInput<T extends HasPermuteInput<T>> {
  /**
   * Permute the input dimensions of this object.
   *
   * @param permutation the permutation to apply.
   * @return a new permuted object.
   */
  @Nonnull
  T permuteInput(@Nonnull int... permutation);
}
