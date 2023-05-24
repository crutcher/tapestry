package loom.zspace;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import loom.common.HasToJsonString;
import loom.common.JsonUtil;

@Immutable
@ThreadSafe
@Jacksonized
@Builder
public final class NamedZAffineMap
    implements HasToJsonString, HasNamedPermuteInput, HasNamedPermuteOutput {
  public final DimensionMap input;
  public final DimensionMap output;
  public final ZAffineMap map;

  @JsonCreator
  public NamedZAffineMap(
      @JsonProperty("input") DimensionMap input,
      @JsonProperty("output") DimensionMap output,
      @JsonProperty("map") ZAffineMap map) {
    input.assertNDim(map.inputDim);
    output.assertNDim(map.outputDim);
    this.input = input;
    this.output = output;
    this.map = map;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    NamedZAffineMap that = (NamedZAffineMap) o;
    return input.equals(that.input) && output.equals(that.output) && map.equals(that.map);
  }

  @Override
  public int hashCode() {
    return Objects.hash(input, output, map);
  }

  @Override
  public String toString() {
    return toJsonString();
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
  public static NamedZAffineMap parse(String string) {
    return JsonUtil.fromJson(string, NamedZAffineMap.class);
  }

  @Override
  public NamedZAffineMap permuteInput(int... permutation) {
    return new NamedZAffineMap(input.permute(permutation), output, map.permuteInput(permutation));
  }

  @Override
  public NamedZAffineMap permuteInput(String... dimensions) {
    return permuteInput(input.toPermutation(dimensions));
  }

  @Override
  public NamedZAffineMap permuteOutput(int... permutation) {
    return new NamedZAffineMap(input, output.permute(permutation), map.permuteOutput(permutation));
  }

  @Override
  public NamedZAffineMap permuteOutput(String... dimensions) {
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
}
