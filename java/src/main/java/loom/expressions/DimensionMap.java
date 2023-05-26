package loom.expressions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import loom.common.HasToJsonString;
import loom.common.JsonUtil;
import loom.zspace.HasDimension;
import loom.zspace.Indexing;
import loom.zspace.ZPoint;

@Immutable
@ThreadSafe
@JsonDeserialize(using = DimensionMap.Deserializer.class)
public class DimensionMap implements HasDimension, HasNamedPermute, HasToJsonString {
  static class Deserializer extends StdDeserializer<DimensionMap> {
    public Deserializer() {
      super(ZPoint.class);
    }

    @Override
    public DimensionMap deserialize(
        com.fasterxml.jackson.core.JsonParser p,
        com.fasterxml.jackson.databind.DeserializationContext ctxt)
        throws java.io.IOException {
      return new DimensionMap(p.readValueAs(String[].class));
    }
  }

  @JsonValue public final ImmutableList<String> names;

  @JsonCreator
  public DimensionMap(@JsonProperty("names") String... names) {
    this.names = ImmutableList.copyOf(names);
    for (var name : names) {
      if (name == null) {
        throw new IllegalArgumentException("name cannot be null");
      }
      if (Collections.frequency(this.names, name) != 1) {
        throw new IllegalArgumentException("duplicate name: " + name);
      }

      Identifiers.validAtomicIdentifier(name);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DimensionMap)) return false;
    DimensionMap that = (DimensionMap) o;
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

  @Override
  public String toJsonString() {
    return JsonUtil.toJson(this);
  }

  public static DimensionMap parseDimensionMap(String json) {
    return JsonUtil.fromJson(json, DimensionMap.class);
  }

  @Override
  public int ndim() {
    return names.size();
  }

  /**
   * Returns the index of the given dimension name.
   *
   * @param name the dimension name
   * @return the index of the given dimension name.
   * @throws IndexOutOfBoundsException if the given dimension name is not in this dimension map.
   */
  public int indexOf(String name) {
    var idx = names.indexOf(name);
    if (idx == -1) {
      throw new IndexOutOfBoundsException("no such dimension: " + name);
    }
    return idx;
  }

  /**
   * Returns the name of the dimension at the given index.
   *
   * @param index the index.
   * @return the name of the dimension at the given index.
   * @throws IndexOutOfBoundsException if the given index is out of bounds.
   */
  public String nameOf(int index) {
    return names.get(index);
  }

  /**
   * Maps a permutation of names to a permutation of indices.
   *
   * @param names the names in the desired order.
   * @return the permutation of indices.
   * @throws IndexOutOfBoundsException if the given names are not a permutation of this dimension.
   */
  public int[] toPermutation(String... names) {
    return toPermutation(Arrays.asList(names));
  }

  /**
   * Maps a permutation of names to a permutation of indices.
   *
   * @param names the names in the desired order.
   * @return the permutation of indices.
   * @throws IndexOutOfBoundsException if the given names are not a permutation of this dimension.
   */
  public int[] toPermutation(Iterable<String> names) {
    var perm = new int[ndim()];
    int i = 0;
    for (var name : names) {
      perm[i++] = indexOf(name);
    }
    return Indexing.resolvePermutation(perm, ndim());
  }

  @Override
  public DimensionMap permute(int... permutation) {
    var perm = Indexing.resolvePermutation(permutation, ndim());

    var names = new String[ndim()];
    for (int i = 0; i < ndim(); ++i) {
      names[i] = nameOf(perm[i]);
    }

    return new DimensionMap(names);
  }

  @Override
  public DimensionMap permute(String... permutation) {
    return permute(toPermutation(permutation));
  }
}
