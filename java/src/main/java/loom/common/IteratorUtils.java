package loom.common;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nonnull;

public class IteratorUtils {
  public static <T> Stream<T> iterableToStream(@Nonnull Iterable<T> iterable) {
    return StreamSupport.stream(iterable.spliterator(), false);
  }

  /**
   * Check if an Iterable is empty.
   *
   * @param iterable The Iterable to check.
   * @return True if the Iterable is empty, false otherwise.
   */
  public static boolean iterableIsNotEmpty(Iterable<?> iterable) {
    return iterable != null && iterable.iterator().hasNext();
  }
}
