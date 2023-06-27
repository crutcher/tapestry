package loom.graph;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import java.util.UUID;
import javax.annotation.Nullable;

@JsonSubTypes(
    value = {
      @JsonSubTypes.Type(value = TBlockOperator.class),
      @JsonSubTypes.Type(value = TViewOperator.class),
      @JsonSubTypes.Type(value = TFusionOperator.class),
      @JsonSubTypes.Type(value = TCellwiseOperator.class),
    })
public abstract class TOperatorBase extends TNodeBase
    implements TParameters.THasParametersProperty,
        TTensor.THasInputsProperty,
        TTensor.TYieldsResultsProperty {

  protected TOperatorBase(@Nullable UUID id) {
    super(id);
  }
}
