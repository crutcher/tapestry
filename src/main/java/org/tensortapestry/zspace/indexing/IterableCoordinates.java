package org.tensortapestry.zspace.indexing;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;
import org.tensortapestry.zspace.impl.ZSpaceIteratorUtils;

/** An iterable and streamable view over coordinates in a range. */
public final class IterableCoordinates implements Iterable<int[]> {

  /**
   * An Iterator over coordinates.
   *
   * <p>When the buffer mode is {@link BufferMode#REUSED}, the buffer is shared between subsequent
   * calls to {@link Iterator#next()}. When the buffer mode is {@link BufferMode#SAFE}, the buffer
   * is not shared between subsequent calls to {@link Iterator#next()}.
   */
  public final class CoordIterator implements Iterator<int[]> {

    private int remaining = size;

    @Nullable private int[] current = null;

    @Nonnull
    public BufferMode getBufferMode() {
      return bufferMode;
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

      if (bufferMode == BufferMode.SAFE) {
        return current.clone();
      }

      return current;
    }
  }

  @Nonnull
  @Getter
  private final BufferMode bufferMode;

  @Nonnull
  private final int[] start;

  @Nonnull
  private final int[] end;

  @Getter
  private final int size;

  /**
   * Construct an iterable view over coordinates in a range.
   *
   * @param bufferMode the buffer mode.
   * @param end the end coordinates.
   */
  public IterableCoordinates(@Nonnull BufferMode bufferMode, @Nonnull int[] end) {
    this(bufferMode, new int[end.length], end);
  }

  /**
   * Construct an iterable view over coordinates in a range.
   *
   * @param bufferMode the buffer mode.
   * @param start the start coordinates.
   * @param end the end coordinates.
   */
  public IterableCoordinates(
    @Nonnull BufferMode bufferMode,
    @Nonnull int[] start,
    @Nonnull int[] end
  ) {
    this.bufferMode = bufferMode;
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

  @Nonnull
  public Stream<int[]> stream() {
    return ZSpaceIteratorUtils.iterableToStream(this);
  }
}
