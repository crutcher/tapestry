package loom.expressions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Splitter;
import java.util.List;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import loom.common.HasToJsonString;
import loom.common.JsonUtil;
import loom.zspace.*;

@Immutable
@ThreadSafe
@Jacksonized
@Builder
public final class NamedZRange implements HasDimension, HasSize, HasNamedPermute, HasToJsonString {
  public final DimensionMap dimensions;
  public final ZRange range;

  @JsonCreator
  public NamedZRange(
      @JsonProperty(value = "dimensions", required = true) DimensionMap dimensions,
      @JsonProperty(value = "slice", required = true) ZRange slice) {
    HasDimension.assertSameNDim(dimensions, slice);
    this.dimensions = dimensions;
    this.range = slice;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof NamedZRange)) return false;
    NamedZRange that = (NamedZRange) o;
    return dimensions.equals(that.dimensions) && range.equals(that.range);
  }

  @Override
  public int hashCode() {
    return Objects.hash(dimensions, range);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append("i[");

    for (int i = 0; i < ndim(); ++i) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(
          String.format(
              "%s=%d:%d",
              dimensions.nameOf(i), range.start.coords.get(i), range.end.coords.get(i)));
    }
    sb.append("]");

    return sb.toString();
  }

  @Override
  public String toJsonString() {
    return JsonUtil.toJson(this);
  }

  /**
   * Parse a string into a NamedZRange.
   *
   * <p>Supports two formats:
   *
   * <ul>
   *   <li>JSON
   *   <li>pretty string
   * </ul>
   *
   * @param str the string to parse.
   * @return the parsed NamedZRange.
   * @throws IllegalArgumentException if the string is not a valid NamedZRange.
   */
  public static NamedZRange parse(String str) {
    if (str.startsWith("{")) {
      return JsonUtil.fromJson(str, NamedZRange.class);
    }

    if (str.startsWith("i[") && str.endsWith("]")) {
      var t = str.substring(2, str.length() - 1).trim();
      if (t.isEmpty()) {
        return new NamedZRange(new DimensionMap(), ZRange.fromShape(ZTensor.vector()));
      }

      List<String> parts = Splitter.on(",").splitToList(t);
      var dims = new String[parts.size()];
      var start = new int[parts.size()];
      var end = new int[parts.size()];

      for (int i = 0; i < parts.size(); ++i) {
        var nameParts = Splitter.on('=').splitToList(parts.get(i));
        if (nameParts.size() != 2) {
          throw new IllegalArgumentException(String.format("invalid range: %s", str));
        }
        String name = nameParts.get(0).trim();
        dims[i] = name;

        String rangeStr = nameParts.get(1).trim();

        var rangeParts = Splitter.on(':').splitToList(rangeStr);
        if (rangeParts.size() != 2) {
          throw new IllegalArgumentException(String.format("invalid range: %s", str));
        }

        start[i] = Integer.parseInt(rangeParts.get(0).trim());
        end[i] = Integer.parseInt(rangeParts.get(1).trim());
      }

      return new NamedZRange(
          new DimensionMap(dims),
          new ZRange(new ZPoint(ZTensor.from(start)), new ZPoint(ZTensor.from(end))));
    }

    throw new IllegalArgumentException(String.format("Invalid ZRange: %s", str));
  }

  @Override
  public int ndim() {
    return dimensions.ndim();
  }

  @Override
  public int size() {
    return range.size();
  }

  @Override
  public NamedZRange permute(int... permutation) {
    return new NamedZRange(dimensions.permute(permutation), range.permute(permutation));
  }

  @Override
  public NamedZRange permute(String... dimensions) {
    return permute(this.dimensions.toPermutation(dimensions));
  }
}
