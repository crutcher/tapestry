package loom.graph;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public abstract class TSequencedBase extends TNodeBase {
    TSequencedBase(@Nullable UUID id) {
        super(id);
    }

    public final List<UUID> barrierIds() {
        return assertGraph()
                .queryEdges(THappensAfterEdge.class)
                .withSourceId(getId())
                .toStream()
                .map(TEdgeBase::getTargetId)
                .toList();
    }

    public final List<TSequencePoint> barriers() {
        return assertGraph()
                .queryEdges(THappensAfterEdge.class)
                .withSourceId(getId())
                .toStream()
                .map(THappensAfterEdge::getTarget)
                .toList();
    }
}
