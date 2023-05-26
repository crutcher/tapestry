package loom.expressions;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Objects;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import loom.common.HasToJsonString;
import loom.common.JsonUtil;
import loom.zspace.HasDimension;

@Jacksonized
@Builder
public final class BlockSlice implements HasDimension, HasToJsonString {
  @Nonnull public final String name;
  @Nonnull public final String dtype;
  @Nonnull public final NamedZRange index;

  @JsonCreator
  public BlockSlice(@Nonnull String name, @Nonnull String dtype, @Nonnull NamedZRange index) {
    this.name = IdentifiersFns.validAtomicIdentifier(name);
    this.dtype = IdentifiersFns.validDottedIdentifier(dtype);
    this.index = index;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BlockSlice)) return false;
    BlockSlice that = (BlockSlice) o;
    return name.equals(that.name) && dtype.equals(that.dtype) && index.equals(that.index);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, dtype, index);
  }

  @Override
  public String toString() {
    return String.format("%s:%s=%s", name, dtype, index);
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
