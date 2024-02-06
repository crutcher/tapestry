package org.tensortapestry.common.collections;

import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nonnull;
import lombok.Getter;
import lombok.experimental.UtilityClass;

@UtilityClass
public class EnumerationUtils {

  /**
   * A streamable iterable that enumerates the elements of another iterable.
   *
   * @param <T> The type of the elements.
   */
  @Getter
  public static class EnumeratedIterable<T> implements StreamableIterable<Map.Entry<Integer, T>> {

    /**
     * The wrapped iterable to enumerate.
     */
    private final Iterable<T> iterable;
    /**
     * The index to start enumerating from.
     */
    private final int offset;

    /**
     * Create a new EnumeratedIterable, enumerating from 0.
     *
     * @param iterable The iterable to enumerate.
     */
    public EnumeratedIterable(Iterable<T> iterable) {
      this(iterable, 0);
    }

    /**
     * Create a new EnumeratedIterable.
     *
     * @param iterable The iterable to enumerate.
     * @param offset The index to start enumerating from.
     */
    public EnumeratedIterable(Iterable<T> iterable, int offset) {
      this.iterable = iterable;
      this.offset = offset;
    }

    @Override
    @Nonnull
    public Iterator<Map.Entry<Integer, T>> iterator() {
      return new EnumeratedIterator<>(iterable.iterator(), offset);
    }

    /**
     * Create a new child EnumeratedIterable, shifted by the given offset.
     *
     * @param offset The offset to shift by.
     * @return The new EnumeratedIterable.
     */
    public EnumeratedIterable<T> withOffset(int offset) {
      return new EnumeratedIterable<>(iterable, offset + this.offset);
    }
  }

  /**
   * An iterator that enumerates the elements of another iterator.
   *
   * @param <T> The type of the elements.
   */
  @Getter
  public static class EnumeratedIterator<T> implements Iterator<Map.Entry<Integer, T>> {

    private int index = 0;
    private final int offset;
    private final Iterator<T> iterator;

    /**
     * Create a new EnumeratedIterator, enumerating from 0.
     *
     * @param iterator The iterator to enumerate.
     */
    public EnumeratedIterator(Iterator<T> iterator) {
      this(iterator, 0);
    }

    /**
     * Create a new EnumeratedIterator.
     *
     * @param iterator The iterator to enumerate.
     * @param offset The index to start enumerating from.
     */
    public EnumeratedIterator(Iterator<T> iterator, int offset) {
      this.iterator = iterator;
      this.offset = offset;
    }

    @Override
    public boolean hasNext() {
      return iterator.hasNext();
    }

    @Override
    public Map.Entry<Integer, T> next() {
      return Map.entry(index++ + offset, iterator.next());
    }
  }

  /**
   * Enumerate the elements of an Iterable.
   *
   * @param iterable The Iterable to enumerate.
   * @param <T> The type of the elements.
   * @return The EnumeratedIterable.
   */
  public <T> EnumeratedIterable<T> enumerate(Iterable<T> iterable) {
    return enumerate(iterable, 0);
  }

  /**
   * Enumerate the elements of an Iterable, starting from the given offset.
   *
   * @param iterable The Iterable to enumerate.
   * @param offset The index to start enumerating from.
   * @param <T> The type of the elements.
   * @return The EnumeratedIterable.
   */
  public <T> EnumeratedIterable<T> enumerate(Iterable<T> iterable, int offset) {
    return new EnumeratedIterable<>(iterable, offset);
  }
}
