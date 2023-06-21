package loom.graph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

@JsonTypeName("WaitsOn")
@TEdge.TargetType(TSequencePoint.class)
public class TWaitsOn extends TEdge {
    @JsonCreator
    TWaitsOn(
            @Nullable @JsonProperty(value = "id", required = true) UUID id,
            @Nonnull @JsonProperty(value = "sourceId", required = true) UUID sourceId,
            @Nonnull @JsonProperty(value = "targetId", required = true) UUID targetId) {
        super(id, sourceId, targetId);
    }

    TWaitsOn(@Nonnull UUID sourceId, @Nonnull UUID targetId) {
        this(null, sourceId, targetId);
    }

    TWaitsOn(@Nonnull TWaitsOn source) {
        this(source.id, source.sourceId, source.targetId);
    }

    @Override
    public TWaitsOn copy() {
        return new TWaitsOn(this);
    }
}
