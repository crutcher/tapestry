package loom.alt.objgraph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Splitter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;
import loom.alt.densegraph.NamePatterns;
import loom.common.HasToJsonString;

/**
 * JSON Namespace Names.
 *
 * @see <a href="https://datatracker.ietf.org/doc/id/draft-saintandre-json-namespaces-00.html">JSON
 *     Namespaces</a>
 * @param urn namespace urn
 * @param name name
 */
@JsonSerialize(using = JNSName.JsonSupport.Serializer.class)
@JsonDeserialize(using = JNSName.JsonSupport.Deserializer.class)
public record JNSName(String urn, String name) implements HasToJsonString, Comparable<JNSName> {
  public static final class JsonSupport {
    private JsonSupport() {}

    public static final class Deserializer extends JsonDeserializer<JNSName> {
      @Override
      public JNSName deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return JNSName.parse(p.getText());
      }
    }

    public static final class Serializer extends JsonSerializer<JNSName> {
      @Override
      public void serialize(JNSName value, JsonGenerator gen, SerializerProvider serializers)
          throws IOException {
        gen.writeString(value.toString());
      }
    }

    public static final class KeyDeserializer
        extends com.fasterxml.jackson.databind.KeyDeserializer {
      @Override
      public Object deserializeKey(String key, DeserializationContext ctxt) {
        return JNSName.parse(key);
      }
    }
  }

  public static final Pattern LEGAL_NAME = NamePatterns.DOTTED_IDENTIFIER;

  @Override
  public int compareTo(JNSName o) {
    var cmp = urn.compareTo(o.urn);
    if (cmp != 0) {
      return cmp;
    }
    return name.compareTo(o.name);
  }

  @JsonCreator
  public JNSName {
    try {
      new URL(urn);
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Invalid urn: " + urn);
    }
    if (!LEGAL_NAME.matcher(name).matches()) {
      throw new IllegalArgumentException("Invalid name: " + name);
    }
  }

  @Override
  public String toString() {
    return String.format("{%s}%s", urn, name);
  }

  public static JNSName parse(String str) {
    if (!str.startsWith("{")) {
      throw new IllegalArgumentException("Invalid JNSName: " + str);
    }
    str = str.substring(1);
    var parts = Splitter.on('}').limit(2).splitToList(str);
    if (parts.size() != 2) {
      throw new IllegalArgumentException("Invalid JNSName: " + str);
    }
    return new JNSName(parts.get(0), parts.get(1));
  }
}
