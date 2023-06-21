package loom.graph;

import com.fasterxml.jackson.annotation.*;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.common.HasToJsonString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

@JsonTypeName("node")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonSubTypes(
        value = {
                @JsonSubTypes.Type(value = TTag.class, name = "tag"),
                @JsonSubTypes.Type(value = TEdge.class, name = "edge"),
                @JsonSubTypes.Type(value = TOperator.class, name = "operator"),
                @JsonSubTypes.Type(value = TTensor.class, name = "tensor")
        })
@Jacksonized
@SuperBuilder
public class TNode implements HasToJsonString {
    @Nullable
    @JsonIgnore
    @Builder.Default
    TGraph graph = null;

    @Nonnull
    @JsonProperty(required = true)
    @Builder.Default
    public final UUID id = UUID.randomUUID();

    public boolean hasGraph() {
        return graph != null;
    }

    public TGraph assertGraph() {
        if (graph == null) {
            throw new IllegalStateException("Node is not part of a graph");
        }
        return graph;
    }
}
