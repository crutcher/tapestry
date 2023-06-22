package loom.graph;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

public abstract class TSequencedBase extends TNode {

  TSequencedBase(@Nullable UUID id) {
    super(id);
  }

  TSequencedBase() {
    this((UUID) null);
  }

  TSequencedBase(TSequencedBase source) {
    this(source.id);
  }

  public List<UUID> barrierIds() {
    return assertGraph()
        .queryEdges(THappensAfter.class)
        .withSourceId(id)
        .toStream()
        .map(TEdge::getTargetId)
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
