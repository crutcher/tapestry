package loom.graph;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;

@JsonSubTypes(
    value = {
      @JsonSubTypes.Type(value = TBlockOperator.class),
      @JsonSubTypes.Type(value = TCellOperator.class),
      @JsonSubTypes.Type(value = TFusionOperator.class),
      @JsonSubTypes.Type(value = TMacroOperator.class),
      @JsonSubTypes.Type(value = TViewOperator.class),
    })
@TNodeBase.NodeDisplayOptions.NodeAttributes(
    value = {
      @TNodeBase.NodeDisplayOptions.Attribute(name = "shape", value = "cds"),
      @TNodeBase.NodeDisplayOptions.Attribute(name = "fillcolor", value = "#75DDDD"),
      @TNodeBase.NodeDisplayOptions.Attribute(name = "margin", value = "0.15")
    })
public abstract class TOperatorBase extends TNodeBase
    implements TParameters.THasParametersProperty,
        TTensor.THasInputsProperty,
        TTensor.THasResultsProperty {

  @Nonnull @Getter public final String op;

  protected TOperatorBase(
      @Nullable UUID id, @Nonnull @JsonProperty(value = "op", required = true) String op) {
    super(id);
    this.op = op;
  }
}
