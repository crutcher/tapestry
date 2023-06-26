package loom.graph;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import loom.zspace.ZPoint;

import javax.annotation.Nullable;
import java.util.UUID;

@JsonSubTypes(
        value = {
                @JsonSubTypes.Type(value = TBlockOperator.class),
        })
public abstract class TOperatorBase extends TSequencedBase {

    protected TOperatorBase(@Nullable UUID id) {
        super(id);
    }

    @CanIgnoreReturnValue
    public TConsumesEdge bindInput(TTensor tensor, String label) {
        return assertGraph().addNode(new TConsumesEdge(this.id, tensor.id, label));
    }

    public TTensor bindResult(ZPoint shape, String dtype, String label) {
        var g = assertGraph();
        var t = g.addNode(new TTensor(shape, dtype));
        g.addNode(new TResultEdge(t.id, this.id, label));
        return t;
    }
}
