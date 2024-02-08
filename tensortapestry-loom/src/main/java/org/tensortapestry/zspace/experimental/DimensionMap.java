package org.tensortapestry.zspace.experimental;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import org.tensortapestry.zspace.HasDimension;
import org.tensortapestry.zspace.ZPoint;
import org.tensortapestry.zspace.impl.HasJsonOutput;
import org.tensortapestry.zspace.impl.ZSpaceJsonUtil;
import org.tensortapestry.zspace.indexing.IndexingFns;

@Immutable
@ThreadSafe
@JsonDeserialize(using = DimensionMap.Deserializer.class)
public final class DimensionMap
  implements HasDimension, HasNamedPermute<DimensionMap>, HasJsonOutput {

  static final class Deserializer extends StdDeserializer<DimensionMap> {

    public Deserializer() {
      super(ZPoint.class);
    }

    @Override
    public DimensionMap deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      return new DimensionMap(p.readValueAs(String[].class));
    }
  }

  @JsonValue
  public final ImmutableList<String> names;

  public DimensionMap(@Nonnull String... names) {
    for (int i = 0; i < names.length; ++i) {
      var name = names[i];
      if (name == null) {
        throw new IllegalArgumentException("name cannot be null");
      }
      for (int j = 0; j < i; ++j) {
        if (names[j].equals(name)) {
          throw new IllegalArgumentException("duplicate name: " + name);
        }
      }
      IdentifiersFns.validAtomicIdentifier(name);
    }
    this.names = ImmutableList.copyOf(names);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DimensionMap that)) return false;
    return names.equals(that.names);
  }

  @Override
  public int hashCode() {
    return Objects.hash(names);
  }

  @Override
  public String toString() {
    return toJsonString();
  }

  @Nonnull
  public static DimensionMap parseDimensionMap(@Nonnull String json) {
    return Objects.requireNonNull(ZSpaceJsonUtil.fromJson(json, DimensionMap.class));
  }

  @Override
  public int getNDim() {
    return names.size();
  }

  @Override
  public int indexOfName(@Nonnull String name) {
    var idx = names.indexOf(name);
    if (idx == -1) {
      throw new IndexOutOfBoundsException("no such dimension: " + name);
    }
    return idx;
  }

  @Override
  @Nonnull
  public String nameOfIndex(int index) {
    return names.get(index);
  }

  @Override
  @Nonnull
  public DimensionMap permute(@Nonnull int... permutation) {
    var perm = IndexingFns.resolvePermutation(permutation, getNDim());

    var names = new String[getNDim()];
    for (int i = 0; i < getNDim(); ++i) {
      names[i] = nameOfIndex(perm[i]);
    }

    return new DimensionMap(names);
  }
}
