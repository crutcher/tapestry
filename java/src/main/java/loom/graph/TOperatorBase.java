package loom.graph;

import com.fasterxml.jackson.annotation.JsonSubTypes;

import javax.annotation.Nullable;
import java.util.UUID;

@JsonSubTypes(
        value = {
                @JsonSubTypes.Type(value = TBlockOperator.class),
        })
public abstract class TOperatorBase extends TNodeBase
        implements TParameters.THasParametersProperty,
        TTensor.TConsumesInputsProperty,
        TTensor.TYieldsResultsProperty {

    protected TOperatorBase(@Nullable UUID id) {
        super(id);
    }
}
