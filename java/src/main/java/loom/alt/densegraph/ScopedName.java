package loom.alt.densegraph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Splitter;
import java.io.IOException;
import java.util.regex.Pattern;
import loom.common.HasToJsonString;
import loom.common.NamePatterns;

@JsonSerialize(using = ScopedName.JsonSupport.Serializer.class)
@JsonDeserialize(using = ScopedName.JsonSupport.Deserializer.class)
public record ScopedName(String scope, String name)
    implements HasToJsonString, Comparable<ScopedName> {
  public static final class JsonSupport {
    private JsonSupport() {}

    public static final class Deserializer
        extends com.fasterxml.jackson.databind.JsonDeserializer<ScopedName> {
      @Override
      public ScopedName deserialize(
          com.fasterxml.jackson.core.JsonParser p, DeserializationContext ctxt) throws IOException {
        return ScopedName.parse(p.getText());
      }
    }

    public static final class Serializer
        extends com.fasterxml.jackson.databind.JsonSerializer<ScopedName> {
      @Override
      public void serialize(
          ScopedName value,
          com.fasterxml.jackson.core.JsonGenerator gen,
          SerializerProvider serializers)
          throws IOException {
        gen.writeString(value.toString());
      }
    }

    public static final class KeyDeserializer
        extends com.fasterxml.jackson.databind.KeyDeserializer {
      @Override
      public Object deserializeKey(String key, DeserializationContext ctxt) {
        return ScopedName.parse(key);
      }
    }
  }

  public static final Pattern LEGAL_SCOPE = NamePatterns.DOTTED_IDENTIFIER;
  public static final Pattern LEGAL_NAME = NamePatterns.DOTTED_IDENTIFIER;

  @JsonCreator
  public ScopedName {
    if (!LEGAL_SCOPE.matcher(scope).matches()) {
      throw new IllegalArgumentException("Invalid scope: " + scope);
    }
    if (!LEGAL_NAME.matcher(name).matches()) {
      throw new IllegalArgumentException("Invalid name: " + name);
    }
  }

  @Override
  public int compareTo(ScopedName o) {
    var cmp = scope.compareTo(o.scope);
    if (cmp != 0) {
      return cmp;
    }
    return name.compareTo(o.name);
  }

  @Override
  public String toString() {
    return scope + "/" + name;
  }

  public static ScopedName parse(String str) {
    var parts = Splitter.on('/').splitToList(str);
    if (parts.size() != 2) {
      throw new IllegalArgumentException("Invalid operator name: " + str);
    }
    return new ScopedName(parts.get(0), parts.get(1));
  }
}
