package loom.graph;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

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

    @CanIgnoreReturnValue
    public TWaitsOnEdge waitOnBarrier(TSequencedBase barrier) {
        return assertGraph().addNode(new TWaitsOnEdge(getId(), barrier.getId()));
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
