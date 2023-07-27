package loom.alt.objgraph;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.JsonNodeDeserializer;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import loom.common.HasToJsonString;
import loom.common.JsonUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class FNode implements HasToJsonString {
  public static class FNodeBuilder {
    public FNodeBuilder attr(JNSName name, Object value) {
      if (this.attrs == null) {
        this.attrs = new java.util.HashMap<>();
      }
      JsonNode jvalue;
      if (value instanceof JsonNode node) {
        jvalue = node;
      } else {
        jvalue = JsonUtil.toTree(value);
      }
      this.attrs.put(name, jvalue);
      return this;
    }

    public FNodeBuilder attrs(Map<JNSName, ? extends Object> entries) {
      if (entries != null) {
        entries.forEach(this::attr);
      }
      return this;
    }
  }

  @Nonnull UUID id;

  @Nonnull JNSName type;

  @JsonDeserialize(
      keyUsing = JNSName.JsonSupport.KeyDeserializer.class,
      contentUsing = JsonNodeDeserializer.class)
  Map<JNSName, JsonNode> attrs;

  @Builder
  public FNode(@Nullable UUID id, @Nonnull JNSName type, @Nullable Map<JNSName, JsonNode> attrs) {
    if (id == null) {
      id = UUID.randomUUID();
    }
    this.id = id;
    this.type = type;
    if (attrs == null) {
      attrs = Map.of();
    }
    this.attrs = Map.copyOf(attrs);
  }
}
