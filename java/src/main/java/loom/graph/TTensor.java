package loom.graph;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.zspace.ZPoint;

import javax.annotation.Nonnull;

@JsonTypeName("tensor")
@Jacksonized
@SuperBuilder
public class TTensor extends TNode {

    @Nonnull
    @JsonProperty(required = true)
    public final String dtype;

    @Nonnull
    @JsonProperty(required = true)
    public final ZPoint shape;
}
