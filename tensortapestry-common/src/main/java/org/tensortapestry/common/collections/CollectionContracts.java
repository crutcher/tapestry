package org.tensortapestry.common.collections;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CollectionContracts {

  /**
   * Expects that the two collections are distinct, i.e. they have no elements in common.
   *
   * @param lhs the first collection.
   * @param rhs the second collection.
   * @param <T> the type of the elements in the collections.
   * @throws IllegalArgumentException if the collections are not distinct.
   */
  public <T> void expectDistinct(Collection<T> lhs, Collection<T> rhs) {
    expectDistinct(lhs, rhs, "items", "lhs", "rhs");
  }

  /**
   * Expects that the two collections are distinct, i.e. they have no elements in common.
   *
   * @param lhs the first collection.
   * @param rhs the second collection.
   * @param itemDesc a description of the items in the collections.
   * @param lhsDesc a description of the first collection.
   * @param rhsDesc a description of the second collection.
   * @param <T> the type of the elements in the collections.
   * @throws IllegalArgumentException if the collections are not distinct.
   */
  public <T> void expectDistinct(
    Collection<T> lhs,
    Collection<T> rhs,
    String itemDesc,
    String lhsDesc,
    String rhsDesc
  ) {
    for (var e : lhs) {
      if (rhs.contains(e)) {
        var overlap = new HashSet<>(lhs);
        overlap.retainAll(rhs);

        throw new IllegalArgumentException(
          "Overlapping %s between \"%s\" and \"%s\": %s".formatted(
              itemDesc,
              lhsDesc,
              rhsDesc,
              overlap
            )
        );
      }
    }
  }

  /**
   * Expect that the keys extracted from the items in the map match the keys in the map.
   *
   * @param map the map to check.
   * @param keyExtractor the function to extract the keys from the items in the map.
   * @param <K> the type of the keys in the map.
   * @param <V> the type of the items in the map.
   * @throws IllegalArgumentException if the keys do not match.
   */
  public <K, V> void expectMapKeysMatchItemKeys(Map<K, V> map, Function<V, K> keyExtractor) {
    for (var kv : map.entrySet()) {
      var key = kv.getKey();
      var item = kv.getValue();
      var extractedKey = keyExtractor.apply(item);
      if (!key.equals(extractedKey)) {
        throw new IllegalArgumentException("Key mismatch: %s != %s".formatted(key, extractedKey));
      }
    }
  }
}
