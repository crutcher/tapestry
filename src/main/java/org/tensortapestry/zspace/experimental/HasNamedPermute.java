package org.tensortapestry.zspace.experimental;

import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import org.tensortapestry.zspace.HasPermute;
import org.tensortapestry.zspace.indexing.IndexingFns;

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
  int indexOfName(@Nonnull String name);

  /**
   * Returns the name of the dimension at the given index.
   *
   * @param index the index.
   * @return the name of the dimension at the given index.
   * @throws IndexOutOfBoundsException if the given index is out of bounds.
   */
  @Nonnull
  String nameOfIndex(int index);

  /**
   * Maps a permutation of names to a permutation of indices.
   *
   * @param names the names in the desired order.
   * @return the permutation of indices.
   * @throws IndexOutOfBoundsException if the given names are not a permutation of this dimension.
   */
  @Nonnull
  default int[] toPermutation(@Nonnull String... names) {
    return toPermutation(Arrays.asList(names));
  }

  /**
   * Maps a permutation of names to a permutation of indices.
   *
   * @param names the names in the desired order.
   * @return the permutation of indices.
   * @throws IndexOutOfBoundsException if the given names are not a permutation of this dimension.
   */
  @Nonnull
  default int[] toPermutation(@Nonnull List<String> names) {
    var perm = new int[getNDim()];
    int i = 0;
    for (var name : names) {
      perm[i++] = indexOfName(name);
    }
    return IndexingFns.resolvePermutation(perm, getNDim());
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
  @Nonnull
  default T permuteByNames(@Nonnull String... permutation) {
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
  @Nonnull
  default T permuteByNames(@Nonnull List<String> permutation) {
    return permute(toPermutation(permutation));
  }
}
