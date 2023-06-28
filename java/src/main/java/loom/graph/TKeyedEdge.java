package loom.graph;

import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public abstract class TKeyedEdge<S extends TNodeInterface, T extends TNodeInterface>
        extends TEdgeBase<S, T> {

    @Nonnull
    @Getter
    protected final String key;

    public TKeyedEdge(
            @Nullable UUID id, @Nonnull UUID sourceId, @Nonnull UUID targetId, @Nonnull String key) {
        super(id, sourceId, targetId);
        this.key = key;
    }
}
