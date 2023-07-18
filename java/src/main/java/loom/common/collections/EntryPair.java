package loom.common.collections;

import java.util.Map;
import java.util.Objects;

/**
 * Simple utility implementation of {@link java.util.Map.Entry}.
 *
 * @param <K> the type of the key.
 * @param <V> the type of the value.
 */
public final class EntryPair<K, V> implements Map.Entry<K, V> {
  private final K key;
  private V value;

  public static <X, Y> EntryPair<X, Y> of(X key, Y value) {
    return new EntryPair<X, Y>(key, value);
  }

  public EntryPair(K key, V value) {
    this.key = key;
    this.value = value;
  }

  @Override
  public K getKey() {
    return key;
  }

  @Override
  public V getValue() {
    return value;
  }

  @Override
  public V setValue(V newValue) {
    V oldValue = value;
    value = newValue;
    return oldValue;
  }

  @Override
  public String toString() {
    return key + "=" + value;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(key) ^ Objects.hashCode(value);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;

    return o instanceof Map.Entry<?, ?> e
        && Objects.equals(key, e.getKey())
        && Objects.equals(value, e.getValue());
  }
}
