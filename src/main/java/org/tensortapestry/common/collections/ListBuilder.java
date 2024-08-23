package org.tensortapestry.common.collections;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * A builder for lists, similar to {@link StringBuilder}.
 *
 * @param <T> the type of the list.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ListBuilder<T> {

  /**
   * Create a new list builder.
   *
   * @param <T> the type of the list.
   * @return the new list builder.
   */
  @Nonnull
  public static <T> ListBuilder<T> builder() {
    return wrap(new ArrayList<>());
  }

  /**
   * Wrap an existing list in a builder.
   *
   * @param list the list to wrap.
   * @param <T> the type of the list.
   * @return the new list builder.
   */
  @Nonnull
  public static <T> ListBuilder<T> wrap(@Nonnull List<T> list) {
    return new ListBuilder<>(list);
  }

  @Nonnull
  private final List<T> list;

  /**
   * Add an item.
   *
   * @param item the item.
   * @return {@code this}
   */
  @Nonnull
  @CanIgnoreReturnValue
  public ListBuilder<T> add(@Nonnull T item) {
    list.add(item);
    return this;
  }

  /**
   * Add an item if it is not null.
   *
   * @param item the item.
   * @return {@code this}
   */
  @Nonnull
  @CanIgnoreReturnValue
  public ListBuilder<T> addNonNull(@Nullable T item) {
    if (item != null) {
      add(item);
    }
    return this;
  }

  /**
   * Add all items.
   *
   * @param items the items.
   * @return {@code this}
   */
  @Nonnull
  @CanIgnoreReturnValue
  public ListBuilder<T> addAll(@Nonnull List<T> items) {
    list.addAll(items);
    return this;
  }

  /**
   * Add all items.
   *
   * @param items the items.
   * @return {@code this}
   */
  @SafeVarargs
  @Nonnull
  @CanIgnoreReturnValue
  public final ListBuilder<T> addAll(T... items) {
    for (var item : items) {
      add(item);
    }
    return this;
  }

  /**
   * Add all non-null items.
   *
   * @param items the items.
   * @return {@code this}
   */
  @Nonnull
  @CanIgnoreReturnValue
  public ListBuilder<T> addAllNonNull(@Nullable List<T> items) {
    if (items != null) {
      for (var item : items) {
        addNonNull(item);
      }
    }
    return this;
  }

  /**
   * Add all non-null items.
   *
   * @param items the items.
   * @return {@code this}
   */
  @SafeVarargs
  @Nonnull
  @CanIgnoreReturnValue
  public final ListBuilder<T> addAllNonNull(T... items) {
    for (var item : items) {
      addNonNull(item);
    }
    return this;
  }

  /**
   * Return the list.
   *
   * @return the list.
   */
  @Nonnull
  public List<T> build() {
    return list;
  }
}
