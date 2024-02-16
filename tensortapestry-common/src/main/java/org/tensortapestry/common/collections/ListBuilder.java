package org.tensortapestry.common.collections;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ListBuilder<T> {

  public static <T> ListBuilder<T> builder() {
    return wrap(new ArrayList<>());
  }

  public static <T> ListBuilder<T> wrap(List<T> list) {
    return new ListBuilder<>(list);
  }

  private final List<T> list;

  @Nonnull
  @CanIgnoreReturnValue
  public ListBuilder<T> add(@Nonnull T item) {
    list.add(item);
    return this;
  }

  @Nonnull
  @CanIgnoreReturnValue
  public ListBuilder<T> addNonNull(@Nullable T item) {
    if (item != null) {
      add(item);
    }
    return this;
  }

  @Nonnull
  @CanIgnoreReturnValue
  public ListBuilder<T> addAll(@Nonnull List<T> items) {
    list.addAll(items);
    return this;
  }

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

  @Nonnull
  @CanIgnoreReturnValue
  public ListBuilder<T> addAll(T... items) {
    for (var item : items) {
      add(item);
    }
    return this;
  }

  @Nonnull
  @CanIgnoreReturnValue
  public ListBuilder<T> addAllNonNull(T... items) {
    for (var item : items) {
      addNonNull(item);
    }
    return this;
  }

  public List<T> build() {
    return list;
  }
}
