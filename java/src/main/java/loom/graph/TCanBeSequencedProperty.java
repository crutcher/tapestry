package loom.graph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public interface TCanBeSequencedProperty extends TNodeInterface {
  @CanIgnoreReturnValue
  default TWaitsOnEdge waitOnBarrier(TCanBeSequencedProperty barrier) {
    return assertGraph().addNode(new TWaitsOnEdge(getId(), barrier.getId()));
  }

  default void markAsIO() {
    assertGraph().addNode(new TIOTag(getId()));
  }

  default TSequencePoint createBarrier() {
    var g = assertGraph();
    var sp = g.addNode(new TSequencePoint());
    g.addNode(new TWaitsOnEdge(sp.id, getId()));
    return sp;
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

  @JsonTypeName("IO")
  @TTagBase.SourceType(TCanBeSequencedProperty.class)
  @TNodeBase.NodeDisplayOptions.NodeAttributes(
          value = {
                  @TNodeBase.NodeDisplayOptions.Attribute(name = "fillcolor", value = "coral"),
          })
  class TIOTag extends TTagBase<TCanBeSequencedProperty> {
    @JsonCreator
    public TIOTag(
        @Nullable @JsonProperty(value = "id", required = true) UUID id,
        @Nonnull @JsonProperty(value = "sourceId", required = true) UUID sourceId) {
      super(id, sourceId);
    }

    public TIOTag(UUID sourceId) {
      this(null, sourceId);
    }

    public TIOTag(@Nonnull TIOTag source) {
      this(source.id, source.sourceId);
    }

    @Override
    public TIOTag copy() {
      return new TIOTag(this);
    }
  }
}
