package loom.alt.linkgraph.expressions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.common.HasToJsonString;
import loom.common.JsonUtil;
import loom.zspace.ZAffineMap;
import loom.zspace.ZPoint;
import loom.zspace.ZRange;
import loom.zspace.ZTensor;

@Immutable
@ThreadSafe
@Jacksonized
@SuperBuilder
public final class IndexProjectionFunction
    implements HasToJsonString, HasNamedPermuteInput, HasNamedPermuteOutput {
  @Nonnull public final DimensionMap input;
  @Nonnull public final DimensionMap output;
  @Nonnull public final ZAffineMap map;
  @Nonnull public final ZPoint shape;

  @JsonCreator
  public IndexProjectionFunction(
      @JsonProperty("input") DimensionMap input,
      @JsonProperty("output") DimensionMap output,
      @JsonProperty("map") ZAffineMap map,
      @JsonProperty("shape") ZPoint shape) {
    input.assertNDim(map.inputDim());
    output.assertNDim(map.outputDim());
    shape.assertNDim(map.outputDim());

    if (!shape.coords.isStrictlyPositive()) {
      throw new IllegalArgumentException(
          String.format("shape must be strictly positive: %s", shape));
    }

    this.input = input;
    this.output = output;
    this.map = map;
    this.shape = shape;
  }

  @Override
  @SuppressWarnings("EqualsGetClass")
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    IndexProjectionFunction that = (IndexProjectionFunction) o;
    return input.equals(that.input) && output.equals(that.output) && map.equals(that.map);
  }

  @Override
  public int hashCode() {
    return Objects.hash(input, output, map);
  }

  @Override
  public String toString() {
    // Ex: "p[a=x+4:+2, b=2y+5:+1, c=-x+2y+6:+4]"

    StringBuilder sb = new StringBuilder();
    sb.append("p[");
    for (int i = 0; i < map.outputDim(); ++i) {
      if (i > 0) sb.append(", ");

      sb.append(output.nameOf(i) + "=");
      boolean leading = false;

      for (int j = 0; j < map.inputDim(); ++j) {
        var f = map.a.get(i, j);
        if (f == 0) continue;

        if (f < 0) {
          sb.append("-");
        } else if (leading) {
          sb.append("+");
        }
        var af = Math.abs(f);
        if (af != 1) {
          sb.append(af);
        }
        sb.append(input.nameOf(j));
        leading = true;
      }

      var b = map.b.get(i);
      if (b != 0) {
        if (leading && b > 0) sb.append("+");
        sb.append(b);
      }

      sb.append(":");
      var s = shape.coords.get(i);
      sb.append(s < 0 ? "-" : "+");
      sb.append(Math.abs(s));
    }
    sb.append("]");
    return sb.toString();
  }

  @Override
  public String toJsonString() {
    return JsonUtil.toJson(this);
  }

  /**
   * Parse a string into a NamedZAffineMap.
   *
   * @param string the string to parse.
   * @return the parsed NamedZAffineMap.
   */
  public static IndexProjectionFunction parse(String string) {
    return JsonUtil.fromJson(string, IndexProjectionFunction.class);
  }

  @Override
  public IndexProjectionFunction permuteInput(int... permutation) {
    return new IndexProjectionFunction(
        input.permute(permutation), output, map.permuteInput(permutation), shape);
  }

  @Override
  public IndexProjectionFunction permuteInput(String... dimensions) {
    return permuteInput(input.toPermutation(dimensions));
  }

  @Override
  public IndexProjectionFunction permuteOutput(int... permutation) {
    return new IndexProjectionFunction(
        input,
        output.permute(permutation),
        map.permuteOutput(permutation),
        shape.permute(permutation));
  }

  @Override
  public IndexProjectionFunction permuteOutput(String... dimensions) {
    return permuteOutput(output.toPermutation(dimensions));
  }

  /**
   * Apply this map to the given input.
   *
   * @param inputDims the dimension names of the input.
   * @param input the input point.
   * @param outputDims the dimension names of the output.
   * @return the output point.
   */
  public ZPoint apply(DimensionMap inputDims, ZPoint input, DimensionMap outputDims) {
    // Todo: NamedZPoint?
    return new ZPoint(apply(inputDims, input.coords, outputDims));
  }

  /**
   * Apply this map to the given input.
   *
   * @param inputDims the dimension names of the input.
   * @param input the input point.
   * @param outputDims the dimension names of the output.
   * @return the output point.
   */
  public ZTensor apply(DimensionMap inputDims, ZTensor input, DimensionMap outputDims) {
    // Todo: NamedZTensor?
    var x = input.reorderDim(this.input.toPermutation(inputDims.names), 0);
    var y = map.apply(x);
    return y.reorderDim(this.output.toPermutation(outputDims.names), 0);
  }

  public NamedZRange projectIndex(NamedZRange index) {
    ZRange first =
        ZRange.fromStartWithShape(apply(index.dimensions, index.range.start, output), shape);
    ZRange last =
        ZRange.fromStartWithShape(
            apply(index.dimensions, index.range.inclusiveEnd(), output), shape);
    return new NamedZRange(output, ZRange.boundingRange(first, last));
  }
}
