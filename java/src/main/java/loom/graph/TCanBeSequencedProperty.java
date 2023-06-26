package loom.graph;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.List;
import java.util.UUID;

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
}
