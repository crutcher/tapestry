package org.tensortapestry.zspace.impl;

import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@FunctionalInterface
public interface StreamableIterable<T> extends Iterable<T> {
  default Stream<T> stream() {
    return StreamSupport.stream(spliterator(), false);
  }

  default List<T> toList() {
    return stream().toList();
  }
}
