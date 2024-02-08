package org.tensortapestry.zspace;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.tensortapestry.zspace.impl.HasJsonOutput;
import org.tensortapestry.zspace.impl.ParseUtil;
import org.tensortapestry.zspace.impl.ZSpaceJsonUtil;
import org.tensortapestry.zspace.indexing.BufferOwnership;
import org.tensortapestry.zspace.indexing.IterableCoordinates;
import org.tensortapestry.zspace.ops.DominanceOrderingOps;
import org.tensortapestry.zspace.ops.RangeOps;

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
@EqualsAndHashCode(cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY)
public class ZRange implements Cloneable, HasSize, HasPermute<ZRange>, HasJsonOutput {

  /**
   * ZRange builder.
   */
  @SuppressWarnings("unused")
  public static final class ZRangeBuilder {

    private ZTensor shape;

    /**
     * Set the shape of the range.
     *
     * <p>If the start is not set, it will be set to zeros.
     *
     * @param shape the shape.
     * @return {@code this}
     */
    @Nonnull
    @CanIgnoreReturnValue
    public ZRangeBuilder shape(@Nonnull ZTensorWrapper shape) {
      this.shape = shape.unwrap();
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
    @Nonnull
    @CanIgnoreReturnValue
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
    @Nonnull
    @CanIgnoreReturnValue
    public ZRangeBuilder start(@Nonnull ZTensorWrapper start) {
      this.start = ZPoint.of(start);
      return this;
    }

    /**
     * Set the start of the range.
     *
     * @param start the shape.
     * @return {@code this}
     */
    @Nonnull
    @CanIgnoreReturnValue
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
    @Nonnull
    @CanIgnoreReturnValue
    public ZRangeBuilder end(@Nonnull ZTensorWrapper end) {
      this.end = ZPoint.of(end);
      return this;
    }

    /**
     * Set the end of the range.
     *
     * @param end the shape.
     * @return {@code this}
     */
    @Nonnull
    @CanIgnoreReturnValue
    public ZRangeBuilder end(@Nonnull int... end) {
      end(new ZPoint(end));
      return this;
    }

    /**
     * Build the range.
     *
     * @return the range.
     */
    @Nonnull
    public ZRange build() {
      Objects.requireNonNull(this.start, "start is null");
      var end = this.end;
      if (shape != null) {
        if (end != null) {
          throw new IllegalArgumentException("Cannot set both shape and end");
        }
        end = ZPoint.of(start.tensor.add(shape));
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
  @Nonnull
  private static ZRange privateCreator(
    @Nonnull @JsonProperty(value = "start") ZPoint start,
    @Nonnull @JsonProperty(value = "end") ZPoint end
  ) {
    return new ZRange(start, end);
  }

  /**
   * Build a range from {@code [0, shape)}.
   *
   * @param shape the shape of the range.
   * @return a new range.
   */
  @Nonnull
  public static ZRange newFromShape(@Nonnull int... shape) {
    return newFromShape(new ZPoint(shape));
  }

  /**
   * Build a range from {@code [0, shape)}.
   *
   * @param shape the shape of the range.
   * @return a new range.
   */
  @Nonnull
  public static ZRange newFromShape(@Nonnull ZTensorWrapper shape) {
    return new ZRange(ZPoint.newZerosLike(shape), shape);
  }

  /**
   * Construct a new ZRange of {@code [start, end)}.
   *
   * @param start the start point.
   * @param end the exclusive end point.
   * @return a new range.
   */
  @Nonnull
  public static ZRange of(@Nonnull ZTensorWrapper start, @Nonnull ZTensorWrapper end) {
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
    return RangeOps.boundingRange(ranges);
  }

  /**
   * Compute the minimum bounding range of a set of ranges.
   *
   * @param ranges the ranges to bound.
   * @return the minimum bounding range.
   */
  @Nonnull
  public static ZRange boundingRange(@Nonnull Iterable<ZRange> ranges) {
    return RangeOps.boundingRange(ranges);
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
      return Objects.requireNonNull(ZSpaceJsonUtil.fromJson(str, ZRange.class));
    }

    if (str.startsWith("zr[") && str.endsWith("]")) {
      var t = str.substring(3, str.length() - 1).trim();
      if (t.isEmpty()) {
        return new ZRange(new ZPoint(), new ZPoint());
      }

      var parts = ParseUtil.splitCommas(t);
      var start = new int[parts.size()];
      var end = new int[parts.size()];

      for (int i = 0; i < parts.size(); ++i) {
        var rangeParts = ParseUtil.splitColons(parts.get(i));
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

  @Nonnull
  @EqualsAndHashCode.Include
  ZPoint start;

  @Nonnull
  @EqualsAndHashCode.Include
  ZPoint end;

  @JsonIgnore
  @Nonnull
  @EqualsAndHashCode.Exclude
  ZPoint shape;

  @JsonIgnore
  @EqualsAndHashCode.Exclude
  int size;

  /**
   * Construct a new ZRange of {@code [start, end)}.
   *
   * @param start the start point.
   * @param end the exclusive end point.
   */
  public ZRange(@Nonnull ZTensorWrapper start, @Nonnull ZTensorWrapper end) {
    var zstart = ZPoint.of(start);
    var zend = ZPoint.of(end);

    zstart.tensor.assertMatchingShape(zend);
    if (zstart.gt(end)) {
      throw new IllegalArgumentException("start %s must be <= end %s".formatted(zstart, zend));
    }
    this.start = zstart;
    this.end = zend;

    shape = zend.sub(zstart.tensor);
    size = shape.prodAsInt();
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
  @Nonnull
  public String toRangeString() {
    var b = new StringBuilder();
    b.append("[");
    for (int i = 0; i < getNDim(); ++i) {
      if (i > 0) {
        b.append(", ");
      }
      b.append(start.get(i));
      b.append(":");
      b.append(end.get(i));
    }
    b.append("]");
    return b.toString();
  }

  /**
   * Generate a nicely formatted shape string.
   *
   * @return the shape string.
   */
  @Nonnull
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
   * <p>When the buffer mode is {@link BufferOwnership#REUSED}, the buffer is shared between
   * subsequent calls to {@link Iterator#next()}. When the buffer mode is
   * {@link BufferOwnership#CLONED}, the buffer is not shared between subsequent calls to
   * {@link Iterator#next()}.
   *
   * <p>Empty ranges will return an empty iterable.
   *
   * <p>Scalar ranges (ranges where the start and end are zero dimensional ZPoints) will return
   * an iterable with a single empty coordinate array.
   *
   * @param bufferOwnership the buffer mode.
   * @return an iterable over the coordinates of this tensor.
   */
  @Nonnull
  public IterableCoordinates byCoords(@Nonnull BufferOwnership bufferOwnership) {
    return new IterableCoordinates(bufferOwnership, start.toArray(), end.toArray());
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
  public boolean contains(@Nonnull ZTensorWrapper p) {
    return (!isEmpty() && (getNDim() == 0 || (start.le(p) && DominanceOrderingOps.lt(p, end))));
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
    return end.sub(1);
  }

  /**
   * Shift the entire range by the given delta.
   *
   * @param delta the delta to shift by.
   * @return the shifted range.
   */
  @Nonnull
  public ZRange translate(@Nonnull ZTensorWrapper delta) {
    return ZRange.of(start.tensor.add(delta), end.tensor.add(delta));
  }

  /**
   * Return the intersection of this range with another.
   *
   * @param other the other range.
   * @return the intersection of this range with another, null if there is no intersection.
   */
  @Nullable public ZRange intersection(@Nonnull ZRange other) {
    return RangeOps.intersection(this, other);
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
    dim = resolveDim(dim);
    int dimSize = shape.get(dim);

    if (chunkSize <= 0) {
      throw new IllegalArgumentException("chunk size must be > 0: " + chunkSize);
    }

    if (chunkSize >= dimSize) {
      return new ZRange[] { this };
    }

    int numChunks = (dimSize / chunkSize) + (dimSize % chunkSize);

    var chunks = new int[numChunks];
    for (int i = 0; i < numChunks - 1; ++i) {
      chunks[i] = chunkSize;
    }
    chunks[numChunks - 1] = dimSize - (numChunks - 1) * chunkSize;

    return splitImpl(dim, chunks);
  }

  /**
   * Split this range into a number of sub-ranges.
   *
   * @param dim the dimension to split on, resolved.
   * @param chunks the size of each chunk, must sum to {@code shape[dim]}.
   * @return the sub-ranges.
   */
  @Nonnull
  public ZRange[] split(int dim, List<Integer> chunks) {
    return split(dim, chunks.stream().mapToInt(i -> i).toArray());
  }

  /**
   * Split this range into a number of sub-ranges.
   *
   * @param dim the dimension to split on, resolved.
   * @param chunks the size of each chunk, must sum to {@code shape[dim]}.
   * @return the sub-ranges.
   */
  @Nonnull
  public ZRange[] split(int dim, int... chunks) {
    dim = resolveDim(dim);
    int dimSize = shape.get(dim);

    int chunkTotalSize = 0;
    for (int chunk : chunks) {
      if (chunk <= 0) {
        throw new IllegalArgumentException("chunk size must be > 0: " + Arrays.toString(chunks));
      }
      chunkTotalSize += chunk;
    }
    if (chunkTotalSize != dimSize) {
      throw new IllegalArgumentException(
        "total chunk size (%d) must be equal to dim size (%d): %s".formatted(
            chunkTotalSize,
            dimSize,
            Arrays.toString(chunks)
          )
      );
    }

    return splitImpl(dim, chunks);
  }

  /**
   * Split this range into a number of sub-ranges.
   *
   * <p>Unchecked, dim and chunk sizes must be correct.</p>
   *
   * @param dim the dimension to split on.
   * @param chunks the size of each chunk, must sum to {@code shape[dim]}.
   * @return the sub-ranges.
   */
  @Nonnull
  private ZRange[] splitImpl(int dim, int... chunks) {
    int numChunks = chunks.length;

    if (numChunks == 1) {
      return new ZRange[] { this };
    }

    var ranges = new ZRange[numChunks];

    var startArr = start.toArray();
    var endArr = end.toArray();
    endArr[dim] = startArr[dim];

    for (int i = 0; i < numChunks; ++i) {
      int k = chunks[i];
      endArr[dim] += k;
      ranges[i] = new ZRange(new ZPoint(startArr), new ZPoint(endArr));
      startArr[dim] += k;
    }
    return ranges;
  }

  /**
   * Embed this range in a new space.
   * <p>
   * The resulting range will have dimensionality the sum of the two ranges.
   *
   * @param embedRange the range to embed in.
   * @return the new range.
   */
  @Nonnull
  public ZRange cartesianProduct(@Nonnull ZRange embedRange) {
    return RangeOps.cartesianProduct(this, embedRange);
  }

  /**
   * Embed this range in a new space.
   * <p>
   * The resulting range will have dimensionality the sum of the two ranges.
   *
   * @param shape the shape to embed in.
   * @return the new range.
   */
  @Nonnull
  public ZRange cartesianProduct(@Nonnull ZTensorWrapper shape) {
    return RangeOps.cartesianProduct(this, shape);
  }
}
