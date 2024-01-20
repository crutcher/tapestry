package loom.zspace;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Splitter;
import lombok.Builder;
import lombok.Value;
import loom.common.json.HasToJsonString;
import loom.common.json.JsonUtil;
import loom.common.runtime.CheckThat;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

/**
 * Represents a range of points in discrete space.
 *
 * <p>A simple definition is that a range {@code other} is contained if
 *
 * <pre>{@code this.start <= other.start && this.end >= other.end}</pre>
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
@Value
public class ZRange implements Cloneable, HasSize, HasPermute<ZRange>, HasToJsonString {
  /** ZRange builder. */
  @SuppressWarnings("unused")
  public static class ZRangeBuilder {
    private ZTensor shape;

    /**
     * Set the shape of the range.
     *
     * <p>If the start is not set, it will be set to zeros.
     *
     * @param shape the shape.
     * @return {@code this}
     */
    public ZRangeBuilder shape(@Nonnull HasZTensor shape) {
      this.shape = shape.asZTensor();
      if (start == null) {
        start = ZPoint.newZerosLike(shape);
      }
      return this;
    }

    /**
     * Set the shape of the range.
     *
     * <p>If the start is not set, it will be set to zeros.
     *
     * @param shape the shape.
     * @return {@code this}
     */
    public ZRangeBuilder shape(@Nonnull int... shape) {
      shape(new ZPoint(shape));
      return this;
    }

    /**
     * Set the start of the range.
     *
     * @param start the shape.
     * @return {@code this}
     */
    public ZRangeBuilder start(@Nonnull HasZTensor start) {
      this.start = start.asZTensor().newZPoint();
      return this;
    }

    /**
     * Set the start of the range.
     *
     * @param start the shape.
     * @return {@code this}
     */
    public ZRangeBuilder start(@Nonnull int... start) {
      start(new ZPoint(start));
      return this;
    }

    /**
     * Set the end of the range.
     *
     * @param end the shape.
     * @return {@code this}
     */
    public ZRangeBuilder end(@Nonnull HasZTensor end) {
      this.end = end.asZTensor().newZPoint();
      return this;
    }

    /**
     * Set the end of the range.
     *
     * @param end the shape.
     * @return {@code this}
     */
    public ZRangeBuilder end(@Nonnull int... end) {
      end(new ZPoint(end));
      return this;
    }

    /**
     * Build the range.
     *
     * @return the range.
     */
    public ZRange build() {
      var start =
          CheckThat.valueIsNotNull(this.start, IllegalArgumentException.class, "start is null");
      var end = this.end;
      if (shape != null) {
        if (end != null) {
          throw new IllegalArgumentException("Cannot set both shape and end");
        }
        end = start.coords.add(shape).newZPoint();
      }
      return new ZRange(start, end);
    }
  }

  /**
   * Private constructor.
   *
   * <p>This gives the {@code @Builder} concrete types, while permitting normal users to use the
   * {@code HasZTensor} constructor.
   *
   * @param start the start point.
   * @param end the exclusive end point.
   * @return a new range.
   */
  @JsonCreator
  @Builder
  private static ZRange privateBuilder(
      @Nonnull @JsonProperty(value = "start") ZPoint start,
      @Nonnull @JsonProperty(value = "end") ZPoint end) {
    return new ZRange(start, end);
  }

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
  public static ZRange fromShape(@Nonnull HasZTensor shape) {
    return new ZRange(ZPoint.newZerosLike(shape), shape);
  }

  @Nonnull
  public static ZRange fromStartWithShape(@Nonnull HasZTensor start, @Nonnull HasZTensor shape) {
    return fromShape(shape).translate(start);
  }

  /**
   * Shift the entire range by the given delta.
   *
   * @param delta the delta to shift by.
   * @return the shifted range.
   */
  @Nonnull
  public ZRange translate(@Nonnull HasZTensor delta) {
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
  public static ZRange of(@Nonnull HasZTensor start, @Nonnull HasZTensor end) {
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

  @Nonnull ZPoint start;
  @Nonnull ZPoint end;
  @JsonIgnore @Nonnull ZTensor shape;
  @JsonIgnore int size;

  /**
   * Construct a new ZRange of {@code [start, end)}.
   *
   * @param start the start point.
   * @param end the exclusive end point.
   */
  public ZRange(@Nonnull HasZTensor start, @Nonnull HasZTensor end) {
    var zstart = start.asZTensor().newZPoint();
    var zend = end.asZTensor().newZPoint();

    zstart.coords.assertMatchingShape(zend.coords);
    if (zstart.gt(end)) {
      throw new IllegalArgumentException("start %s must be <= end %s".formatted(zstart, zend));
    }
    this.start = zstart;
    this.end = zend;

    shape = zend.coords.sub(zstart.coords).asImmutable();
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
    return "zr" + toRangeString();
  }

  /**
   * Generate a pretty formatted range string.
   *
   * @return the range string.
   */
  public String toRangeString() {
    var b = new StringBuilder();
    b.append("[");
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

  /**
   * Generate a nicely formatted shape string.
   *
   * @return the shape string.
   */
  public String toShapeString() {
    var str = shape.toString();
    return "‖" + str.substring(1, str.length() - 1) + "‖";
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
   * <pre>{@code this.start <= other.start && this.end >= other.end}</pre>
   *
   * <p>But there are a number of special cases to consider:
   *
   * <ol>
   *   <li>Can an empty range be contained?
   *   <li>Can an empty range contain anything?
   *   <li>How do define the behavior at 0-dim?
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
  public boolean contains(@Nonnull HasZTensor p) {
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
