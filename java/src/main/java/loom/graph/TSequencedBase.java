package loom.graph;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

@JsonSubTypes(
    value = {
      @JsonSubTypes.Type(value = TOperatorBase.class),
      @JsonSubTypes.Type(value = TObserver.class),
      @JsonSubTypes.Type(value = TSequencePoint.class),
    })
public abstract class TSequencedBase extends TNodeBase {
  protected TSequencedBase(@Nullable UUID id) {
    super(id);
  }

  public final List<UUID> barrierIds() {
    return assertGraph()
        .queryEdges(TWaitsOnEdge.class)
        .withSourceId(getId())
        .toStream()
        .map(TEdgeBase::getTargetId)
        .toList();
  }

  public final List<TSequencePoint> barriers() {
    return assertGraph()
        .queryEdges(TWaitsOnEdge.class)
        .withSourceId(getId())
        .toStream()
        .map(TWaitsOnEdge::getTarget)
        .toList();
  }
}
