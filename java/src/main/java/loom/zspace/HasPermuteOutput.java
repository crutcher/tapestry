package loom.zspace;

public interface HasPermuteOutput {
  /**
   * Permute the output dimensions of this object.
   *
   * @param permutation the permutation to apply.
   * @return a new permuted object.
   */
  HasPermuteOutput permuteOutput(int... permutation);
}
