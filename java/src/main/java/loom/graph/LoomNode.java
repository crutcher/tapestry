package loom.graph;

import com.fasterxml.jackson.annotation.*;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.common.HasToJsonString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@JsonTypeName("node")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonSubTypes(@JsonSubTypes.Type(value = TensorNode.class, name = "tensor"))
@Jacksonized
@SuperBuilder
@Data
public class LoomNode implements HasToJsonString {
    @Nullable
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    @Builder.Default
    @ToString.Exclude
    LoomGraph graph = null;

    @Nonnull
    @JsonProperty(required = true)
    @Builder.Default
    public final UUID id = UUID.randomUUID();

    @Nonnull
    @Builder.Default
    public final Map<String, Map<String, UUID>> deps = new HashMap<>();

    public boolean hasGraph() {
        return graph != null;
    }

    public LoomGraph assertGraph() {
        if (graph == null) {
            throw new IllegalStateException("Node is not part of a graph");
        }
        return graph;
    }

    public void addDep(String type, String name, UUID id) {
        deps.computeIfAbsent(type, k -> new HashMap<>()).put(name, id);
    }
}
