package loom.graph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

@JsonTypeName("WaitsOn")
@TTagBase.SourceType(TSequencedBase.class)
@TEdgeBase.TargetType(TSequencedBase.class)
public final class TWaitsOnEdge extends TEdgeBase<TSequencedBase, TSequencePoint> {
    @JsonCreator
    public TWaitsOnEdge(
            @Nullable @JsonProperty(value = "id", required = true) UUID id,
            @Nonnull @JsonProperty(value = "sourceId", required = true) UUID sourceId,
            @Nonnull @JsonProperty(value = "targetId", required = true) UUID targetId) {
        super(id, sourceId, targetId);
    }

    public TWaitsOnEdge(@Nonnull UUID sourceId, @Nonnull UUID targetId) {
        this(null, sourceId, targetId);
    }

    public TWaitsOnEdge(@Nonnull TWaitsOnEdge source) {
        this(source.id, source.sourceId, source.targetId);
    }

    @Override
    public TWaitsOnEdge copy() {
        return new TWaitsOnEdge(this);
    }
}
