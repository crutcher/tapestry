package loom.graph;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.UUID;
import javax.annotation.Nonnull;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@JsonTypeName("tag")
@Jacksonized
@SuperBuilder
public class LoomTag extends LoomNode {
  @Nonnull
  @JsonProperty(required = true)
  public final UUID sourceId;
}
