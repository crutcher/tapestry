package org.tensortapestry.graphviz;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Data;

@Data
public abstract class AbstractGraphvizAttributeMap<
  T extends AbstractGraphvizAttributeMap<T, K>, K extends Enum<K>
> {

  @Nonnull
  private final Map<String, Object> items = new HashMap<>();

  public abstract void validateAttribute(@Nonnull String key, @Nonnull Object value);

  @Nonnull
  @CanIgnoreReturnValue
  public final K checkKey(
    @Nonnull String description,
    @Nonnull Class<K> enumClass,
    @Nonnull String key,
    @Nonnull Object value
  ) {
    try {
      return Enum.valueOf(enumClass, key.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
        "Illegal %s attribute, unknown key: %s=%s".formatted(description, key, value)
      );
    }
  }

  public final boolean isEmpty() {
    return items.isEmpty();
  }

  public final Set<Map.Entry<String, Object>> entrySet() {
    return items.entrySet();
  }

  @SuppressWarnings("unchecked")
  protected final T self() {
    return (T) this;
  }

  @Nonnull
  @CanIgnoreReturnValue
  public final T set(@Nonnull String key, @Nonnull Object value) {
    key = key.toLowerCase(Locale.ROOT);
    validateAttribute(key, value);
    items.put(key, value);
    return self();
  }

  @Nonnull
  @CanIgnoreReturnValue
  public final T set(@Nonnull Object key, @Nonnull Object value) {
    return set(key.toString(), value);
  }

  @Nonnull
  @CanIgnoreReturnValue
  public final T set(@Nonnull K key, @Nonnull Object value) {
    return set(key.toString(), value);
  }

  @Nonnull
  @CanIgnoreReturnValue
  public final T setAll(@Nullable Map<Object, Object> map) {
    if (map == null) {
      return self();
    }
    for (var e : map.entrySet()) {
      set(e.getKey(), e.getValue());
    }
    return self();
  }

  @Nonnull
  @CanIgnoreReturnValue
  public final T setAll(@Nullable Collection<Map.Entry<Object, Object>> entries) {
    if (entries == null) {
      return self();
    }
    for (var e : entries) {
      set(e.getKey(), e.getValue());
    }
    return self();
  }

  @Nonnull
  @CanIgnoreReturnValue
  public final T setAll(@Nullable AbstractGraphvizAttributeMap<?, K> other) {
    if (other == null) {
      return self();
    }
    for (var e : other.items.entrySet()) {
      set(e.getKey(), e.getValue());
    }
    return self();
  }

  @Nullable public final Object get(@Nonnull String key) {
    return items.get(key.toLowerCase(Locale.ROOT));
  }

  @Nullable public final Object get(@Nonnull K key) {
    return get(key.toString());
  }
}
