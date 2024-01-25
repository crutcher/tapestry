package org.tensortapestry.loom.zspace;

import javax.annotation.Nonnull;

public interface HasPermuteOutput<T extends HasPermuteOutput<T>> {
  /**
   * Permute the output dimensions of this object.
   *
   * @param permutation the permutation to apply.
   * @return a new permuted object.
   */
  @Nonnull
  T permuteOutput(@Nonnull int... permutation);
}
