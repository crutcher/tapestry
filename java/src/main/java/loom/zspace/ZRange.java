package loom.zspace;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Splitter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import lombok.Getter;
import loom.common.HasToJsonString;
import loom.common.IteratorUtils;
import loom.common.serialization.JsonUtil;

/**
 * Represents a range of points in discrete space.
 *
 * <p>A simple definition is that a range {@code other} is contained if
 *
 * <pre>
 * this.start <= other.start && this.end >= other.end
 * </pre>
 *
 * <p>But there are a number of special cases to consider:
 *
 * <ol>
 *   <li>Can an empty range be contained?
 *   <li>Can an empty range contain anything?
 *   <li>How do define the behavior at 0-dim?
 * </ol>
 *
 * <p>Empty ranges can have utility in a number of algorithms; they can describe partition surfaces;
 * so we may wish to define containment in a way which permits them to be contained.
 *
 * <p>Empty ranges could only possibly *contain* other empty ranges; describing point-less boundary
 * surfaces; so permitting them to contain other empty ranges seems acceptable.
 *
 * <p>In the case of 0-dim spaces; no point is {@code < end}, as there's exactly one point in the
 * space. So either all 0-dim ranges contain all other 0-dim ranges; or none do. We select the
 * former; as it seems more useful.
 */
@ThreadSafe
@Immutable
public final class ZRange implements HasSize, HasPermute<ZRange>, HasToJsonString {

  /** An Iterable view of the coordinates of the range. */
  @Getter
  public final class IterableCoords implements Iterable<int[]> {
    private final CoordsBufferMode bufferMode;

    IterableCoords(CoordsBufferMode bufferMode) {
      this.bufferMode = bufferMode;
    }

    @Override
    public @Nonnull CoordsIterator iterator() {
      return new CoordsIterator(bufferMode);
    }

    public @Nonnull Stream<int[]> stream() {
      return IteratorUtils.iterableToStream(this);
    }
  }

  /**
   * An Iterator over the coordinates of this ZRange.
   *
   * <p>When the buffer mode is {@link CoordsBufferMode#REUSED}, the buffer is shared between
   * subsequent calls to {@link Iterator#next()}. When the buffer mode is {@link
   * CoordsBufferMode#SAFE}, the buffer is not shared between subsequent calls to {@link
   * Iterator#next()}.
   */
  public final class CoordsIterator implements Iterator<int[]> {
    @Getter private final CoordsBufferMode bufferMode;

    private int remaining = size();
    @Nullable private int[] coords = null;

    public CoordsIterator(CoordsBufferMode bufferMode) {
      this.bufferMode = bufferMode;
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
      remaining--;

      if (coords == null) {
        coords = start.toArray();
      } else {
        coords[coords.length - 1]++;
        for (int i = coords.length - 1; i >= 0; --i) {
          if (coords[i] == end.get(i)) {
            coords[i] = start.get(i);
            coords[i - 1]++;
          }
        }
      }

      if (bufferMode == CoordsBufferMode.SAFE) {
        return coords.clone();
      }

      return coords;
    }
  }

  @Nonnull public final ZPoint start;

  @Nonnull public final ZPoint end;

  @JsonIgnore public final ZTensor shape;
  @JsonIgnore public final int size;

  /**
   * Build a range from {@code [0, shape)}.
   *
   * @param shape the shape of the range.
   * @return a new range.
   */
  public static @Nonnull ZRange fromShape(@Nonnull int... shape) {
    return fromShape(new ZPoint(shape));
  }

  /**
   * Build a range from {@code [0, shape)}.
   *
   * @param shape the shape of the range.
   * @return a new range.
   */
  public static @Nonnull ZRange fromShape(@Nonnull ZTensor shape) {
    return fromShape(new ZPoint(shape));
  }

  /**
   * Build a range from {@code [0, shape)}.
   *
   * @param shape the shape of the range.
   * @return a new range.
   */
  public static @Nonnull ZRange fromShape(@Nonnull ZPoint shape) {
    return new ZRange(ZPoint.zeros_like(shape), shape);
  }

  public static @Nonnull ZRange fromStartWithShape(@Nonnull ZPoint start, @Nonnull ZPoint shape) {
    return fromShape(shape).translate(start);
  }

  /**
   * Construct a new ZRange of {@code [start, end)}.
   *
   * @param start the start point.
   * @param end the exclusive end point.
   * @return a new range.
   */
  public static @Nonnull ZRange of(@Nonnull ZPoint start, @Nonnull ZPoint end) {
    return new ZRange(start, end);
  }

  /**
   * Construct a new ZRange of {@code [start, end)}.
   *
   * @param start the start point.
   * @param end the exclusive end point.
   * @return a new range.
   */
  public static @Nonnull ZRange of(@Nonnull ZTensor start, @Nonnull ZTensor end) {
    return new ZRange(start, end);
  }

  /**
   * Compute the minimum bounding range of a set of ranges.
   *
   * @param ranges the ranges to bound.
   * @return the minimum bounding range.
   */
  public static @Nonnull ZRange boundingRange(@Nonnull ZRange... ranges) {
    return boundingRange(Arrays.asList(ranges));
  }

  /**
   * Compute the minimum bounding range of a set of ranges.
   *
   * @param ranges the ranges to bound.
   * @return the minimum bounding range.
   */
  public static @Nonnull ZRange boundingRange(@Nonnull Iterable<ZRange> ranges) {
    ZRange first = null;
    ZTensor start = null;
    ZTensor end = null;

    for (var r : ranges) {
      if (start == null) {
        first = r;
        start = r.start.coords;
        end = r.end.coords;
      } else {
        HasDimension.assertSameNDim(first, r);

        start = ZTensor.Ops.min(start, r.start.coords);
        end = ZTensor.Ops.max(end, r.end.coords);
      }
    }

    assert start != null : "no ranges";

    return new ZRange(start, end);
  }

  @JsonCreator
  public ZRange(
      @Nonnull @JsonProperty(value = "start", required = true) ZPoint start,
      @Nonnull @JsonProperty(value = "end", required = true) ZPoint end) {
    start.coords.assertMatchingShape(end.coords);
    if (start.gt(end)) {
      throw new IllegalArgumentException("start %s must be <= end %s".formatted(start, end));
    }
    this.start = start;
    this.end = end;

    shape = end.coords.sub(start.coords).immutable();
    {
      int acc = 1;
      for (var coords : shape.toT1()) {
        acc *= coords;
      }
      size = acc;
    }
  }

  public ZRange(@Nonnull ZTensor start, @Nonnull ZTensor end) {
    this(new ZPoint(start), new ZPoint(end));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ZRange zRange)) return false;
    return Objects.equals(start, zRange.start) && Objects.equals(end, zRange.end);
  }

  @Override
  public int hashCode() {
    return Objects.hash(start, end);
  }

  @Override
  public String toString() {
    var b = new StringBuilder();
    b.append("zr[");
    for (int i = 0; i < ndim(); ++i) {
      if (i > 0) {
        b.append(", ");
      }
      b.append(start.coords.get(i));
      b.append(":");
      b.append(end.coords.get(i));
    }
    b.append("]");
    return b.toString();
  }

  /**
   * Parse a range from a string.
   *
   * <p>Supports both the JSON and pretty {@code zr[0:1, 0:1]} format.
   *
   * @param str the string to parse.
   * @return the parsed range.
   * @throws IllegalArgumentException if the string is not a valid range.
   */
  public static @Nonnull ZRange parse(@Nonnull String str) {
    if (str.startsWith("{")) {
      return JsonUtil.fromJson(str, ZRange.class);
    }

    if (str.startsWith("zr[") && str.endsWith("]")) {
      var t = str.substring(3, str.length() - 1).trim();
      if (t.isEmpty()) {
        return new ZRange(new ZPoint(), new ZPoint());
      }

      var parts = Splitter.on(",").splitToList(t);
      var start = new int[parts.size()];
      var end = new int[parts.size()];

      for (int i = 0; i < parts.size(); ++i) {
        var rangeParts = Splitter.on(':').splitToList(parts.get(i));
        if (rangeParts.size() != 2) {
          throw new IllegalArgumentException(String.format("Invalid ZRange: \"%s\"", str));
        }

        start[i] = Integer.parseInt(rangeParts.get(0).trim());
        end[i] = Integer.parseInt(rangeParts.get(1).trim());
      }

      return new ZRange(new ZPoint(start), new ZPoint(end));
    }

    throw new IllegalArgumentException(String.format("Invalid ZRange: \"%s\"", str));
  }

  @Override
  public int ndim() {
    return start.ndim();
  }

  @Override
  public int size() {
    return size;
  }

  /**
   * Returns an {@code Iterable<int[]>} over the coordinates of this ZRange.
   *
   * <p>When the buffer mode is {@link CoordsBufferMode#REUSED}, the buffer is shared between
   * subsequent calls to {@link Iterator#next()}. When the buffer mode is {@link
   * CoordsBufferMode#SAFE}, the buffer is not shared between subsequent calls to {@link
   * Iterator#next()}.
   *
   * <p>Empty ranges will return an empty iterable.
   *
   * <p>Scalar ranges (ranges where the start and end are zero dimensional ZPoints) will return an
   * iterable with a single empty coordinate array.
   *
   * @param bufferMode the buffer mode.
   * @return an iterable over the coordinates of this tensor.
   */
  public @Nonnull IterableCoords byCoords(CoordsBufferMode bufferMode) {
    return new IterableCoords(bufferMode);
  }

  @Override
  public @Nonnull ZRange permute(@Nonnull int... permutation) {
    return new ZRange(start.permute(permutation), end.permute(permutation));
  }

  /**
   * Does this range entirely contain the other range?
   *
   * <p>A simple definition is that a range {@code other} is contained if
   *
   * <pre>
   * this.start <= other.start && this.end >= other.end
   * </pre>
   *
   * <p>But there are a number of special cases to consider:
   *
   * <ol>
   *   Can an empty range be contained?
   * </ol>
   *
   * <ol>
   *   Can an empty range contain anything?
   * </ol>
   *
   * <ol>
   *   How do define the behavior at 0-dim?
   * </ol>
   *
   * <p>Empty ranges can have utility in a number of algorithms; they can describe partition
   * surfaces; so we may wish to define containment in a way which permits them to be contained.
   *
   * <p>Empty ranges could only possibly *contain* other empty ranges; describing point-less
   * boundary surfaces; so permitting them to contain other empty ranges seems acceptable.
   *
   * <p>In the case of 0-dim spaces; no point is {@code < end}, as there's exactly one point in the
   * space. So either all 0-dim ranges contain all other 0-dim ranges; or none do. We select the
   * former; as it seems more useful.
   *
   * @param other the other range
   * @return true if this range contains the other range.
   */
  public boolean contains(@Nonnull ZRange other) {
    return ndim() == 0 || (start.le(other.start) && other.end.le(end));
  }

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
  public boolean contains(@Nonnull ZPoint p) {
    return contains(p.coords);
  }

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
  public boolean contains(@Nonnull ZTensor p) {
    return !isEmpty() && (ndim() == 0 || (start.le(p) && ZPoint.Ops.lt(p, end)));
  }

  /**
   * If non-empty, returns the greatest point in the range.
   *
   * @return the greatest point in the range.
   * @throws IndexOutOfBoundsException if the range is empty.
   */
  public @Nonnull ZPoint inclusiveEnd() {
    if (isEmpty()) {
      throw new IndexOutOfBoundsException("Empty range");
    }

    return new ZPoint(end.coords.sub(1));
  }

  /**
   * Shift the entire range by the given delta.
   *
   * @param delta the delta to shift by.
   * @return the shifted range.
   */
  public @Nonnull ZRange translate(@Nonnull ZPoint delta) {
    return translate(delta.coords);
  }

  /**
   * Shift the entire range by the given delta.
   *
   * @param delta the delta to shift by.
   * @return the shifted range.
   */
  public @Nonnull ZRange translate(@Nonnull ZTensor delta) {
    return ZRange.of(start.coords.add(delta), end.coords.add(delta));
  }

  /**
   * Return the intersection of this range with another.
   *
   * @param other the other range.
   * @return the intersection of this range with another, null if there is no intersection.
   */
  public @Nullable ZRange intersection(@Nonnull ZRange other) {
    var s = ZTensor.Ops.binOp(Math::max, start.coords, other.start.coords).zpoint();
    var e = ZTensor.Ops.binOp(Math::min, end.coords, other.end.coords).zpoint();
    if (s.le(e)) {
      return new ZRange(s, e);
    } else {
      return null;
    }
  }
}
