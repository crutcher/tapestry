package loom.alt.linkgraph.expressions;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import loom.common.HasToJsonString;
import loom.common.JsonUtil;
import loom.zspace.HasDimension;

@Jacksonized
@Builder
public final class BoundSlice implements HasDimension, HasToJsonString {
  @Nonnull public final String name;
  @Nonnull public final String dtype;
  @Nonnull public final NamedZRange index;

  @Nonnull public final IndexProjectionFunction projection;

  @JsonCreator
  public BoundSlice(
      @Nonnull String name,
      @Nonnull String dtype,
      @Nonnull NamedZRange index,
      @Nonnull IndexProjectionFunction projection) {
    this.name = IdentifiersFns.validAtomicIdentifier(name);
    this.dtype = IdentifiersFns.validDottedIdentifier(dtype);
    this.index = index;
    this.projection = projection;

    if (!Set.of(index.dimensions).equals(Set.of(projection.output))) {
      throw new IllegalArgumentException(
          String.format(
              "index.dimensions must match projection.output: %s != %s",
              index.dimensions, projection.output));
    }

    index.assertNDim(projection.map.outputDim);
  }

  public void validateAgainstIndex(NamedZRange sourceIndex) {
    var expected =
        projection.projectIndex(sourceIndex).permute(index.dimensions.names.toArray(String[]::new));
    if (!expected.equals(index)) {
      throw new IllegalArgumentException(
          String.format(
              "Projected input %s:%s %s does not match index %s", name, dtype, expected, index));
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BoundSlice)) return false;
    BoundSlice that = (BoundSlice) o;
    return name.equals(that.name)
        && dtype.equals(that.dtype)
        && index.equals(that.index)
        && projection.equals(that.projection);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, dtype, index, projection);
  }

  @Override
  public String toString() {
    return String.format("b[%s:%s %s; %s]", name, dtype, index, projection);
  }

  @Override
  public String toJsonString() {
    return JsonUtil.toJson(this);
  }

  @Override
  public int ndim() {
    return index.ndim();
  }
}
