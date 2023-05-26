package loom.common;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nonnull;

public class IteratorUtils {
  public static <T> Stream<T> iterableToStream(@Nonnull Iterable<T> iterable) {
    return StreamSupport.stream(iterable.spliterator(), false);
  }
}
