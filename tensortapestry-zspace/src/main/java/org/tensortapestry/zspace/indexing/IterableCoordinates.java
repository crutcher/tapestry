package org.tensortapestry.zspace.indexing;

import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;

/**
 * An iterable and streamable view over coordinates in a range.
 */
public final class IterableCoordinates
  implements org.tensortapestry.common.collections.StreamableIterable<int[]> {

  /**
   * An Iterator over coordinates.
   *
   * <p>When the buffer mode is {@link BufferOwnership#REUSED}, the buffer is shared between
   * subsequent calls to {@link Iterator#next()}. When the buffer mode is
   * {@link BufferOwnership#CLONED}, the buffer is not shared between subsequent calls to
   * {@link Iterator#next()}.
   */
  public final class CoordIterator implements Iterator<int[]> {

    private int remaining = size;

    @Nullable private int[] current = null;

    @Nonnull
    public BufferOwnership getBufferOwnership() {
      return bufferOwnership;
    }

    @Override
    public boolean hasNext() {
      return remaining > 0;
    }

    @Override
    @Nonnull
    public int[] next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      // Decrement remaining.
      remaining--;

      if (current == null) {
        // First call to next(); initialize coords to start.
        current = start.clone();
      } else {
        // Increment coords least-significant first.
        current[current.length - 1]++;

        // Propagate carry; rollover at end.
        for (int i = current.length - 1; i >= 0; --i) {
          if (current[i] == end[i]) {
            current[i] = start[i];
            current[i - 1]++;
          }
        }
      }

      if (bufferOwnership == BufferOwnership.CLONED) {
        return current.clone();
      }

      return current;
    }
  }

  @Nonnull
  @Getter
  private final BufferOwnership bufferOwnership;

  @Nonnull
  private final int[] start;

  @Nonnull
  private final int[] end;

  @Getter
  private final int size;

  /**
   * Construct an iterable view over coordinates in a range.
   *
   * @param bufferOwnership the buffer mode.
   * @param end the end coordinates.
   */
  public IterableCoordinates(@Nonnull BufferOwnership bufferOwnership, @Nonnull int[] end) {
    this(bufferOwnership, new int[end.length], end);
  }

  /**
   * Construct an iterable view over coordinates in a range.
   *
   * @param bufferOwnership the buffer mode.
   * @param start the start coordinates.
   * @param end the end coordinates.
   */
  @SuppressWarnings("InconsistentOverloads")
  public IterableCoordinates(
    @Nonnull BufferOwnership bufferOwnership,
    @Nonnull int[] start,
    @Nonnull int[] end
  ) {
    this.bufferOwnership = bufferOwnership;
    this.start = start;
    this.end = end;

    int acc = 1;
    for (int i = 0; i < start.length; ++i) {
      acc *= end[i] - start[i];
    }
    this.size = acc;
  }

  @Override
  @Nonnull
  public CoordIterator iterator() {
    return new CoordIterator();
  }
}
