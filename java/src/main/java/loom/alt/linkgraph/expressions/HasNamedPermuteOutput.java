package loom.alt.linkgraph.expressions;

import loom.zspace.HasPermuteOutput;

public interface HasNamedPermuteOutput extends HasPermuteOutput {
  /**
   * Permute the output dimensions of this object by dimension name order.
   *
   * @param dimensions the dimension name order to apply.
   * @return a new object with the output dimensions permuted.
   */
  HasNamedPermuteOutput permuteOutput(String... dimensions);
}
