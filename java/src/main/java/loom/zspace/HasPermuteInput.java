package loom.zspace;

public interface HasPermuteInput {
  /**
   * Permute the input dimensions of this object.
   *
   * @param permutation the permutation to apply.
   * @return a new permuted object.
   */
  HasPermuteInput permuteInput(int... permutation);
}
