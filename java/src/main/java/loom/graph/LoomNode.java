package loom.graph;

import com.fasterxml.jackson.annotation.*;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.common.HasToJsonString;

@JsonTypeName("node")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonSubTypes(
    value = {
      @JsonSubTypes.Type(value = TagNode.class, name = "tag"),
      @JsonSubTypes.Type(value = TensorNode.class, name = "tensor")
    })
@Jacksonized
@SuperBuilder
public class LoomNode implements HasToJsonString {
  @Nullable @EqualsAndHashCode.Exclude @JsonIgnore @Builder.Default @ToString.Exclude
  LoomGraph graph = null;

  @Nonnull
  @JsonProperty(required = true)
  @Builder.Default
  public final UUID id = UUID.randomUUID();

  public boolean hasGraph() {
    return graph != null;
  }

  public LoomGraph assertGraph() {
    if (graph == null) {
      throw new IllegalStateException("Node is not part of a graph");
    }
    return graph;
  }
}
