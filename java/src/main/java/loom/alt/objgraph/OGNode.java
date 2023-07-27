package loom.alt.objgraph;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import loom.common.HasToJsonString;
import loom.common.JsonUtil;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Value
@Jacksonized
@Builder
public class OGNode implements HasToJsonString {
  public static class OGNodeBuilder {
    public OGNodeBuilder attr(JNSName name, Object value) {
      if (this.attrs == null) {
        this.attrs = new java.util.HashMap<>();
      }
      this.attrs.put(name, JsonUtil.toJson(value));
      return this;
    }
  }

  @Getter @Nonnull @Builder.Default public UUID id = UUID.randomUUID();

  @Nonnull JNSName type;

  @JsonDeserialize(
      keyUsing = JNSName.JsonSupport.KeyDeserializer.class,
      contentUsing = StringDeserializer.class)
  Map<JNSName, String> attrs;

  /** Return a nested { urn: { attr: value } } Map of the attributes. */
  public Map<String, Map<String, String>> attrsByNamespace() {
    return attrs.entrySet().stream()
        .collect(
            Collectors.groupingBy(
                e -> e.getKey().urn(),
                Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue)));
  }
}
