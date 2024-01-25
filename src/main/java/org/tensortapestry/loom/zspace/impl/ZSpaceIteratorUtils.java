package org.tensortapestry.loom.zspace.impl;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nonnull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ZSpaceIteratorUtils {

  /**
   * Convert an Iterable to a Stream.
   *
   * @param iterable The Iterable to convert.
   * @return The Stream.
   * @param <T> The type of the Iterable.
   */
  public <T> Stream<T> iterableToStream(@Nonnull Iterable<T> iterable) {
    return StreamSupport.stream(iterable.spliterator(), false);
  }
}
