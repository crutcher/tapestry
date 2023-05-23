package loom.zspace;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Splitter;
import loom.common.HasToJsonString;
import loom.common.JsonUtil;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.Objects;

@ThreadSafe
@Immutable
public final class ZRange implements HasDimension, HasToJsonString {

    public final ZPoint start;
    public final ZPoint end;

    @JsonIgnore
    public final ZTensor shape;
    @JsonIgnore
    public final int size;

    public static ZRange fromShape(int... shape) {
        return fromShape(new ZPoint(shape));
    }

    public static ZRange fromShape(ZTensor shape) {
        return fromShape(new ZPoint(shape));
    }

    public static ZRange fromShape(ZPoint shape) {
        return new ZRange(ZPoint.zeros(shape.ndim()), shape);
    }

    public static ZRange of(ZPoint start, ZPoint end) {
        return new ZRange(start, end);
    }

    public static ZRange of(ZTensor start, ZTensor end) {
        return new ZRange(start, end);
    }

    @JsonCreator
    public ZRange(@JsonProperty("start") ZPoint start, @JsonProperty("end") ZPoint end) {
        start.coords.assertMatchingShape(end.coords);
        if (start.gt(end)) {
            throw new IllegalArgumentException("start must be <= end");
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

    public ZRange(ZTensor start, ZTensor end) {
        this(new ZPoint(start), new ZPoint(end));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ZRange)) return false;
        ZRange zRange = (ZRange) o;
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

    public static ZRange parseZRange(String str) {
        if (str.startsWith("{")) {
            return JsonUtil.fromJson(str, ZRange.class);
        }

        if (str.startsWith("zr[") && str.endsWith("]")) {
            var t = str.substring(3, str.length() - 1).trim();
            if (t.isEmpty()) {
                return new ZRange(new ZPoint(), new ZPoint());
            }

            List<String> parts = Splitter.on(",").splitToList(t);
            int[] start = new int[parts.size()];
            int[] end = new int[parts.size()];

            for (int i = 0; i < parts.size(); ++i) {
                var rangeParts = Splitter.on(':').splitToList(parts.get(i));
                if (rangeParts.size() != 2) {
                    throw new IllegalArgumentException(String.format("invalid range: %s", str));
                }

                start[i] = Integer.parseInt(rangeParts.get(0).trim());
                end[i] = Integer.parseInt(rangeParts.get(1).trim());
            }

            return new ZRange(new ZPoint(start), new ZPoint(end));
        }

        throw new IllegalArgumentException(String.format("Invalid ZRange: %s", str));
    }

    @Override
    public int ndim() {
        return start.ndim();
    }

    @JsonIgnore
    public boolean isEmpty() {
        return size == 0;
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
     * <p>
     *
     * @param other the other range
     * @return true if this range contains the other range.
     */
    public boolean contains(ZRange other) {
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
    public boolean contains(ZPoint p) {
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
    public boolean contains(ZTensor p) {
        return !isEmpty() && (ndim() == 0 || (start.le(p) && ZPoint.Ops.lt(p, end)));
    }

    public ZPoint inclusiveEnd() {
        if (isEmpty()) {
            throw new IndexOutOfBoundsException("empty range");
        }

        return new ZPoint(end.coords.sub(1));
    }

    public ZRange translate(ZPoint delta) {
        return translate(delta.coords);
    }

    public ZRange translate(ZTensor delta) {
        return ZRange.of(start.coords.add(delta), end.coords.add(delta));
    }
}
