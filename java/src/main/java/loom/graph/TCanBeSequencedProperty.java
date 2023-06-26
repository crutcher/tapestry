package loom.graph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface TCanBeSequencedProperty extends TNodeInterface {
  @CanIgnoreReturnValue
  default TWaitsOnEdge waitOnBarrier(TCanBeSequencedProperty barrier) {
    return assertGraph().addNode(new TWaitsOnEdge(getId(), barrier.getId()));
  }

  default List<UUID> barrierIds() {
    return assertGraph()
        .queryEdges(TWaitsOnEdge.class)
        .withSourceId(getId())
        .toStream()
        .map(TEdgeBase::getTargetId)
        .toList();
  }

  default List<TSequencePoint> barriers() {
    return assertGraph()
        .queryEdges(TWaitsOnEdge.class)
        .withSourceId(getId())
        .toStream()
        .map(TWaitsOnEdge::getTarget)
        .toList();
  }

  @JsonTypeName("WaitsOn")
  @TTagBase.SourceType(TCanBeSequencedProperty.class)
  @TEdgeBase.TargetType(TCanBeSequencedProperty.class)
  final class TWaitsOnEdge extends TEdgeBase<TCanBeSequencedProperty, TSequencePoint> {
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
}
