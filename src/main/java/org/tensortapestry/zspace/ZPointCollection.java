package org.tensortapestry.zspace;

import java.util.Iterator;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

import org.tensortapestry.common.collections.IteratorUtils;
import org.tensortapestry.common.json.HasToJsonString;
import org.tensortapestry.zspace.indexing.BufferOwnership;
import org.tensortapestry.zspace.indexing.IterableCoordinates;

@ThreadSafe
@Immutable
public interface ZPointCollection<T extends ZPointCollection<T>>
  extends Cloneable, HasSize, HasToJsonString, HasPermute<T>, Iterable<ZPoint> {
  /**
   * Does this range contain the given point?
   *
   * <p>To contain a point, a range must be non-empty, and {@code start <= p < end}.
   *
   * <p>A 0-dim range contains all 0-dim points.
   *
   * @param p the point.
   * @return true if this range contains the point.
   */
  boolean contains(@Nonnull ZTensorWrapper p);

  /**
   * Returns an {@code Iterable<ZPoint>} over the points of this ZRange.
   *
   * <p>Empty ranges will return an empty iterable.
   *
   * <p>Scalar ranges (ranges where the start and end are zero dimensional ZPoints) will return
   * an iterable with a single empty coordinate point.
   *
   * @return an iterable over the points of this tensor.
   */
  @Nonnull
  Iterator<ZPoint> iterator();

  /**
   * Returns a stream of the points of this ZRange.
   *
   * <p>Empty ranges will return an empty stream.
   *
   * <p>Scalar ranges (ranges where the start and end are zero dimensional ZPoints) will return
   * a stream with a single empty coordinate point.
   *
   * @return a stream over the points of this tensor.
   */
  @Nonnull
  default Stream<ZPoint> stream() {
    return IteratorUtils.iterableToStream(this);
  }
}
