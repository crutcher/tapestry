package loom.graph;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import javax.annotation.Nonnull;
import java.util.UUID;

@JsonTypeName("tag")
@Jacksonized
@SuperBuilder
public class TagNode extends LoomNode {
    @Nonnull
    @JsonProperty(required = true)
    public final UUID sourceId;
}
