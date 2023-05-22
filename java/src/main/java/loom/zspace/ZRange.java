package loom.zspace;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Splitter;
import java.util.List;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import loom.common.HasToJsonString;
import loom.common.JsonUtil;

@ThreadSafe
@Immutable
public final class ZRange implements HasDimension, HasToJsonString {

  public final ZPoint start;
  public final ZPoint end;

  @JsonIgnore public final ZTensor shape;
  @JsonIgnore public final int size;

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
    if (o == null || getClass() != o.getClass()) return false;
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

    throw new IllegalArgumentException(String.format("invalid range: %s", str));
  }

  @Override
  public int ndim() {
    return start.ndim();
  }

  @JsonIgnore
  public boolean isEmpty() {
    return size == 0;
  }

  public boolean contains(ZRange other) {
    return !isEmpty() && start.le(other.start) && other.end.le(end);
  }

  public boolean contains(ZPoint p) {
    return !isEmpty() && start.le(p) && p.lt(end);
  }

  public ZPoint inclusiveEnd() {
    if (isEmpty()) {
      throw new IndexOutOfBoundsException("empty range");
    }

    return new ZPoint(end.coords.sub(1));
  }

  public ZRange translate(ZTensor delta) {
    return ZRange.of(start.coords.add(delta), end.coords.add(delta));
  }

  public ZRange mul(ZTensor factor) {
    return ZRange.of(start.coords.mul(factor), end.coords.mul(factor));
  }

  public ZRange div(ZTensor factor) {
    return ZRange.of(start.coords.div(factor), end.coords.div(factor));
  }
}
