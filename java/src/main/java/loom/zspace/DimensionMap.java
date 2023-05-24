package loom.zspace;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.annotation.concurrent.Immutable;
import loom.common.HasToJsonString;
import loom.common.JsonUtil;

@Immutable
@JsonDeserialize(using = DimensionMap.Deserializer.class)
public class DimensionMap implements HasDimension, HasToJsonString {
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

  public static Pattern DIMENSION_NAME_PATTERN = Pattern.compile("[a-zA-Z][a-zA-Z0-9_]*");

  public static void checkName(String name) {
    if (!DIMENSION_NAME_PATTERN.matcher(name).matches()) {
      throw new IllegalArgumentException("invalid name: " + name);
    }
  }

  @JsonValue public final ImmutableList<String> names;

  @JsonCreator
  public DimensionMap(@JsonProperty("names") String[] names) {
    this.names = ImmutableList.copyOf(names);
    for (var name : names) {
      if (name == null) {
        throw new IllegalArgumentException("name cannot be null");
      }
      if (Collections.frequency(this.names, name) != 1) {
        throw new IllegalArgumentException("duplicate name: " + name);
      }

      checkName(name);
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
}
