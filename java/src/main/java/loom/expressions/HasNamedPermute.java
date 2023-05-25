package loom.expressions;

import loom.zspace.HasPermute;

public interface HasNamedPermute extends HasPermute {
  HasNamedPermute permute(String... permutation);
}
