package org.tensortapestry.loom.common.collections;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@FunctionalInterface
public interface IterableStreamable<T> extends Iterable<T> {
  default Stream<T> stream() {
    return StreamSupport.stream(spliterator(), false);
  }

  default List<T> toList() {
    return stream().toList();
  }

  default void forEach(Consumer<? super T> forEach) {
    stream().forEach(forEach);
  }
}
