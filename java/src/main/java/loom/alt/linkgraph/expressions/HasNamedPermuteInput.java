package loom.alt.linkgraph.expressions;

import loom.zspace.HasPermuteInput;

public interface HasNamedPermuteInput extends HasPermuteInput {

  /**
   * Permute the input dimensions of this object by dimension name order.
   *
   * @param dimensions the dimension name order to apply.
   * @return a new object with the input dimensions permuted.
   */
  HasNamedPermuteInput permuteInput(String... dimensions);
}
