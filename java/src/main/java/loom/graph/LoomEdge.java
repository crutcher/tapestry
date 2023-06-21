package loom.graph;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.UUID;
import javax.annotation.Nonnull;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@JsonTypeName("edge")
@Jacksonized
@SuperBuilder
public class LoomEdge extends LoomTag {
  @Nonnull
  @JsonProperty(required = true)
  public final UUID targetId;
}
