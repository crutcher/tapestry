package org.tensortapestry.loom.zspace;

public interface HasPermuteInput<T extends HasPermuteInput<T>> {
  /**
   * Permute the input dimensions of this object.
   *
   * @param permutation the permutation to apply.
   * @return a new permuted object.
   */
  T permuteInput(int... permutation);
}
