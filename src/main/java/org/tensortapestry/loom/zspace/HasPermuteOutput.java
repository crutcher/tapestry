package org.tensortapestry.loom.zspace;

public interface HasPermuteOutput<T extends HasPermuteOutput<T>> {
  /**
   * Permute the output dimensions of this object.
   *
   * @param permutation the permutation to apply.
   * @return a new permuted object.
   */
  T permuteOutput(int... permutation);
}
