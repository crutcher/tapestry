package org.tensortapestry.loom.graph;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@FunctionalInterface
public interface IterableStreamable<T> extends Iterable<T> {
  default Stream<T> stream() {
    return StreamSupport.stream(spliterator(), false);
  }
}
