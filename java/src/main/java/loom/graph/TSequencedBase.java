package loom.graph;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

public abstract class TSequencedBase extends TNodeBase {

  protected TSequencedBase(@Nullable UUID id) {
    super(id);
  }

  public List<UUID> barrierIds() {
    return assertGraph()
        .queryEdges(THappensAfter.class)
        .withSourceId(id)
        .toStream()
        .map(TEdgeBase::getTargetId)
        .toList();
  }

  public List<TSequencePoint> barriers() {
    return assertGraph()
        .queryEdges(THappensAfter.class)
        .withSourceId(id)
        .toStream()
        .map(THappensAfter::getTarget)
        .toList();
  }
}
