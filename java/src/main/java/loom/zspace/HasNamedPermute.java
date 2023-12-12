package loom.zspace;

import java.util.Arrays;

/**
 * Interface for objects that can be permuted by dimension name.
 *
 * @param <T> the type of the object.
 */
public interface HasNamedPermute<T extends HasNamedPermute<T>> extends HasPermute<T> {
  /**
   * Returns the index of the given dimension name.
   *
   * @param name the dimension name
   * @return the index of the given dimension name.
   * @throws IndexOutOfBoundsException if the given dimension name is not in this dimension map.
   */
  int indexOf(String name);

  /**
   * Returns the name of the dimension at the given index.
   *
   * @param index the index.
   * @return the name of the dimension at the given index.
   * @throws IndexOutOfBoundsException if the given index is out of bounds.
   */
  String nameOf(int index);

  /**
   * Maps a permutation of names to a permutation of indices.
   *
   * @param names the names in the desired order.
   * @return the permutation of indices.
   * @throws IndexOutOfBoundsException if the given names are not a permutation of this dimension.
   */
  default int[] toPermutation(String... names) {
    return toPermutation(Arrays.asList(names));
  }

  /**
   * Maps a permutation of names to a permutation of indices.
   *
   * @param names the names in the desired order.
   * @return the permutation of indices.
   * @throws IndexOutOfBoundsException if the given names are not a permutation of this dimension.
   */
  default int[] toPermutation(Iterable<String> names) {
    var perm = new int[ndim()];
    int i = 0;
    for (var name : names) {
      perm[i++] = indexOf(name);
    }
    return IndexingFns.resolvePermutation(perm, ndim());
  }

  /**
   * Permute the dimensions of this object.
   *
   * <p>A permutation is an ordered list of distinct names, each of which is in the set of dimension
   * names for the object.
   *
   * @param permutation the permutation to apply.
   * @return the permuted object.
   */
  default T permute(String... permutation) {
    return permute(toPermutation(permutation));
  }

  /**
   * Permute the dimensions of this object.
   *
   * <p>A permutation is an ordered list of distinct names, each of which is in the set of dimension
   * names for the object.
   *
   * @param permutation the permutation to apply.
   * @return the permuted object.
   */
  default T permute(Iterable<String> permutation) {
    return permute(toPermutation(permutation));
  }
}
