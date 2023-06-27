package loom.graph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

@JsonTypeName("CellOperator")
@TNodeBase.DisplayOptions.NodeAttributes(
        value = {
                @TNodeBase.DisplayOptions.Attribute(name = "shape", value = "cds"),
                @TNodeBase.DisplayOptions.Attribute(name = "fillcolor", value = "#a0d0d0"),
                @TNodeBase.DisplayOptions.Attribute(name = "margin", value = "0.15")
        })
public class TCellOperator extends TOperatorBase {
    @Nonnull
    @Getter
    public final String op;

    @JsonCreator
    public TCellOperator(
            @Nullable @JsonProperty(value = "id", required = true) UUID id,
            @Nonnull @JsonProperty(value = "op", required = true) String op) {
        super(id);
        this.op = op;
    }

    public TCellOperator(@Nonnull String op) {
        this(null, op);
    }

    public TCellOperator(@Nonnull TCellOperator source) {
        this(source.id, source.op);
    }

    @Override
    public TCellOperator copy() {
        return new TCellOperator(this);
    }
}
