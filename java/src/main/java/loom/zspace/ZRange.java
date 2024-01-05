package loom.zspace;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Splitter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import lombok.Builder;
import lombok.Getter;
import loom.common.json.HasToJsonString;
import loom.common.json.JsonUtil;

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
@Getter
public final class ZRange implements Cloneable, HasSize, HasPermute<ZRange>, HasToJsonString {

  /**
   * Build a range from {@code [0, shape)}.
   *
   * @param shape the shape of the range.
   * @return a new range.
   */
  @Nonnull
  public static ZRange fromShape(@Nonnull int... shape) {
    return fromShape(new ZPoint(shape));
  }

  /**
   * Build a range from {@code [0, shape)}.
   *
   * @param shape the shape of the range.
   * @return a new range.
   */
  @Nonnull
  public static ZRange fromShape(@Nonnull ZPoint shape) {
    return new ZRange(ZPoint.newZerosLike(shape), shape);
  }

  /**
   * Build a range from {@code [0, shape)}.
   *
   * @param shape the shape of the range.
   * @return a new range.
   */
  @Nonnull
  public static ZRange fromShape(@Nonnull ZTensor shape) {
    return fromShape(new ZPoint(shape));
  }

  /**
   * Build a range from {@code [start, start + shape)}.
   *
   * @param start the start point.
   * @param shape the shape of the range.
   * @return a new range.
   */
  @Nonnull
  public static ZRange fromStartWithShape(@Nonnull ZPoint start, @Nonnull ZPoint shape) {
    return fromShape(shape).translate(start);
  }

  /**
   * Shift the entire range by the given delta.
   *
   * @param delta the delta to shift by.
   * @return the shifted range.
   */
  @Nonnull
  public ZRange translate(@Nonnull ZPoint delta) {
    return translate(delta.coords);
  }

  /**
   * Shift the entire range by the given delta.
   *
   * @param delta the delta to shift by.
   * @return the shifted range.
   */
  @Nonnull
  public ZRange translate(@Nonnull ZTensor delta) {
    return ZRange.of(start.coords.add(delta), end.coords.add(delta));
  }

  /**
   * Construct a new ZRange of {@code [start, end)}.
   *
   * @param start the start point.
   * @param end the exclusive end point.
   * @return a new range.
   */
  @Nonnull
  public static ZRange of(@Nonnull ZTensor start, @Nonnull ZTensor end) {
    return new ZRange(start, end);
  }

  /**
   * Construct a new ZRange of {@code [start, end)}.
   *
   * @param start the start point.
   * @param end the exclusive end point.
   * @return a new range.
   */
  @Nonnull
  public static ZRange of(@Nonnull ZPoint start, @Nonnull ZPoint end) {
    return new ZRange(start, end);
  }

  /**
   * Compute the minimum bounding range of a set of ranges.
   *
   * @param ranges the ranges to bound.
   * @return the minimum bounding range.
   */
  @Nonnull
  public static ZRange boundingRange(@Nonnull ZRange... ranges) {
    return boundingRange(Arrays.asList(ranges));
  }

  /**
   * Compute the minimum bounding range of a set of ranges.
   *
   * @param ranges the ranges to bound.
   * @return the minimum bounding range.
   */
  @Nonnull
  public static ZRange boundingRange(@Nonnull Iterable<ZRange> ranges) {
    var it = ranges.iterator();
    if (!it.hasNext()) {
      throw new IllegalArgumentException("no ranges");
    }

    var first = it.next();
    var start = first.start.coords;
    var end = first.end.coords;

    while (it.hasNext()) {
      var r = it.next();
      HasDimension.assertSameNDim(first, r);

      start = ZTensor.Ops.minimum(start, r.start.coords);
      end = ZTensor.Ops.maximum(end, r.end.coords);
    }

    return new ZRange(start, end);
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
  @Nonnull
  public static ZRange parse(@Nonnull String str) {
    str = str.strip();

    if (str.startsWith("{")) {
      return JsonUtil.fromJson(str, ZRange.class);
    }

    if (str.startsWith("zr[") && str.endsWith("]")) {
      var t = str.substring(3, str.length() - 1).trim();
      if (t.isEmpty()) {
        return new ZRange(new ZPoint(), new ZPoint());
      }

      var parts = COMMA_SPLITTER.splitToList(t);
      var start = new int[parts.size()];
      var end = new int[parts.size()];

      for (int i = 0; i < parts.size(); ++i) {
        var rangeParts = COLON_SPLITTER.splitToList(parts.get(i));
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

  private static final Splitter COMMA_SPLITTER = Splitter.on(",");
  private static final Splitter COLON_SPLITTER = Splitter.on(':');

  @Nonnull private final ZPoint start;
  @Nonnull private final ZPoint end;
  @JsonIgnore @Nonnull private final ZTensor shape;
  @JsonIgnore private final int size;

  /**
   * Construct a new ZRange of {@code [start, end)}.
   *
   * @param start the start point.
   * @param end the exclusive end point.
   */
  public ZRange(@Nonnull ZTensor start, @Nonnull ZTensor end) {
    this(new ZPoint(start), new ZPoint(end));
  }

  @SuppressWarnings("unused")
  public static class ZRangeBuilder {
    private ZPoint shape;

    public ZRangeBuilder shape(@Nonnull ZPoint shape) {
      this.shape = shape;
      return this;
    }

    public ZRangeBuilder shape(@Nonnull ZTensor shape) {
      shape(shape.newZPoint());
      return this;
    }

    public ZRange build() {
      if (shape != null) {
        if (end != null) {
          throw new IllegalStateException("Cannot set both shape and end");
        }
        return ZRange.fromStartWithShape(start, shape);
      } else {
        return new ZRange(start, end);
      }
    }
  }

  /**
   * Construct a new ZRange of {@code [start, end)}.
   *
   * @param start the start point.
   * @param end the exclusive end point.
   */
  @JsonCreator
  @Builder
  public ZRange(
      @Nonnull @JsonProperty(value = "start") ZPoint start,
      @Nonnull @JsonProperty(value = "end") ZPoint end) {
    start.coords.assertMatchingShape(end.coords);
    if (start.gt(end)) {
      throw new IllegalArgumentException("start %s must be <= end %s".formatted(start, end));
    }
    this.start = start;
    this.end = end;

    shape = end.coords.sub(start.coords).asImmutable();
    size = shape.prodAsInt();
  }

  @Override
  public int hashCode() {
    return Objects.hash(start, end);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ZRange zRange)) return false;
    return Objects.equals(start, zRange.start) && Objects.equals(end, zRange.end);
  }

  @Override
  @SuppressWarnings("MethodDoesntCallSuperMethod")
  public ZRange clone() {
    // ZRange is immutable, so we can just return this.
    return this;
  }

  @Override
  public String toString() {
    var b = new StringBuilder();
    b.append("zr[");
    for (int i = 0; i < getNDim(); ++i) {
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

  @Override
  public int getNDim() {
    return start.getNDim();
  }

  @Override
  public int getSize() {
    return size;
  }

  /**
   * Resolve a dimension index.
   *
   * <p>Negative dimension indices are resolved relative to the number of dimensions.
   *
   * @param dim the dimension index.
   * @return the resolved dimension index.
   * @throws IndexOutOfBoundsException if the index is out of range.
   */
  public int resolveDim(int dim) {
    return start.resolveDim(dim);
  }

  /**
   * Returns an {@code Iterable<int[]>} over the coordinates of this ZRange.
   *
   * <p>When the buffer mode is {@link BufferMode#REUSED}, the buffer is shared between subsequent
   * calls to {@link Iterator#next()}. When the buffer mode is {@link BufferMode#SAFE}, the buffer
   * is not shared between subsequent calls to {@link Iterator#next()}.
   *
   * <p>Empty ranges will return an empty iterable.
   *
   * <p>Scalar ranges (ranges where the start and end are zero dimensional ZPoints) will return an
   * iterable with a single empty coordinate array.
   *
   * @param bufferMode the buffer mode.
   * @return an iterable over the coordinates of this tensor.
   */
  @Nonnull
  public IterableCoordinates byCoords(@Nonnull BufferMode bufferMode) {
    return new IterableCoordinates(bufferMode, start.toArray(), end.toArray());
  }

  @Override
  @Nonnull
  public ZRange permute(@Nonnull int... permutation) {
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
    return getNDim() == 0 || (start.le(other.start) && other.end.le(end));
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
    return !isEmpty() && (getNDim() == 0 || (start.le(p) && ZPoint.Ops.lt(p, end)));
  }

  /**
   * If non-empty, returns the greatest point in the range.
   *
   * @return the greatest point in the range.
   * @throws IndexOutOfBoundsException if the range is empty.
   */
  @JsonIgnore
  @Nonnull
  public ZPoint getInclusiveEnd() {
    if (isEmpty()) {
      throw new IndexOutOfBoundsException("Empty range");
    }

    return new ZPoint(end.coords.sub(1));
  }

  /**
   * Return the intersection of this range with another.
   *
   * @param other the other range.
   * @return the intersection of this range with another, null if there is no intersection.
   */
  @Nullable public ZRange intersection(@Nonnull ZRange other) {
    var iStart = ZTensor.Ops.maximum(start.coords, other.start.coords).newZPoint();
    var iEnd = ZTensor.Ops.minimum(end.coords, other.end.coords).newZPoint();
    if (iStart.le(iEnd)) {
      return new ZRange(iStart, iEnd);
    } else {
      return null;
    }
  }

  /**
   * Split this range into a number of sub-ranges.
   *
   * <p>The sub-ranges will be non-overlapping, and will cover the entire range.
   *
   * <p>The last sub-range may be smaller than the others.
   *
   * @param dim the dimension to split on.
   * @param chunkSize the size of each chunk.
   * @return the sub-ranges.
   */
  @Nonnull
  public ZRange[] split(int dim, int chunkSize) {
    if (chunkSize <= 0) {
      throw new IllegalArgumentException("chunkSize must be > 0: " + chunkSize);
    }

    int d = resolveDim(dim);
    ZTensor shape = getShape();
    int dimSize = shape.get(d);

    if (chunkSize >= dimSize) {
      return new ZRange[] {this};
    }

    int n = dimSize / chunkSize;
    if (dimSize % chunkSize != 0) {
      ++n;
    }

    var startArr = start.coords.toT1();
    var endArr = end.coords.toT1();
    var ranges = new ZRange[n];

    for (int i = 0; i < n; ++i) {
      int dimStart = i * chunkSize;
      int dimEnd = Math.min(dimStart + chunkSize, dimSize);
      startArr[d] = dimStart;
      endArr[d] = dimEnd;
      ranges[i] = new ZRange(new ZPoint(startArr), new ZPoint(endArr));
    }
    return ranges;
  }
}
