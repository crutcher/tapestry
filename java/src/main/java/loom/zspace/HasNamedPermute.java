package loom.zspace;

public interface HasNamedPermute extends HasPermute {
  HasNamedPermute permute(String... permutation);
}
