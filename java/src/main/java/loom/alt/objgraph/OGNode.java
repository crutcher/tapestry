package loom.alt.objgraph;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.common.HasToJsonString;

@Value
@Jacksonized
@SuperBuilder
public class OGNode implements HasToJsonString {

  @Getter @Nonnull @Builder.Default public UUID id = UUID.randomUUID();

  @Nonnull JNSName type;

  @JsonDeserialize(
      keyUsing = JNSName.JsonSupport.KeyDeserializer.class,
      contentUsing = StringDeserializer.class)
  @Singular
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
