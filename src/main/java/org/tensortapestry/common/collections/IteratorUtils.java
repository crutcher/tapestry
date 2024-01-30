package org.tensortapestry.common.collections;

import java.util.Iterator;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nonnull;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;

@UtilityClass
public class IteratorUtils {

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

  public <T> Stream<T> iteratorToStream(@Nonnull Iterator<T> iterator) {
    return StreamSupport.stream(supplierToIterable(() -> iterator).spliterator(), false);
  }

  /**
   * Convert a Supplier of Iterators to an Iterable.
   *
   * @param supplier The Supplier to convert.
   * @return The Iterable.
   * @param <T> The type of the Supplier.
   */
  public <T> Iterable<T> supplierToIterable(@Nonnull Supplier<Iterator<T>> supplier) {
    return supplier::get;
  }

  /**
   * Check if an Iterable is empty.
   *
   * @param iterable The Iterable to check.
   * @return True if the Iterable is empty, false otherwise.
   */
  public boolean iterableIsNotEmpty(Iterable<?> iterable) {
    return iterable != null && iterable.iterator().hasNext();
  }

  public static class EnumerateIterable<T> implements Iterable<Pair<Integer, T>> {

    private final Iterable<T> iterable;

    public EnumerateIterable(Iterable<T> iterable) {
      this.iterable = iterable;
    }

    @Override
    @Nonnull
    public Iterator<Pair<Integer, T>> iterator() {
      return new EnumerateIterator<>(iterable.iterator());
    }
  }

  public static class EnumerateIterator<T> implements Iterator<Pair<Integer, T>> {

    private int index = 0;
    private final Iterator<T> iterator;

    public EnumerateIterator(Iterator<T> iterator) {
      this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
      return iterator.hasNext();
    }

    @Override
    public Pair<Integer, T> next() {
      return Pair.of(index++, iterator.next());
    }
  }

  public <T> Iterable<Pair<Integer, T>> enumerate(Iterable<T> iterator) {
    return new EnumerateIterable<>(iterator);
  }
}
