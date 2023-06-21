package loom.graph;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.annotation.Nonnull;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.zspace.ZPoint;

@JsonTypeName("tensor")
@Jacksonized
@SuperBuilder
public class LoomTensor extends LoomNode {

  @Nonnull
  @JsonProperty(required = true)
  public final String dtype;

  @Nonnull
  @JsonProperty(required = true)
  public final ZPoint shape;
}
