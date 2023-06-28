package loom.graph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@JsonTypeName("Macro")
@TNodeBase.NodeDisplayOptions.NodeAttributes(
    value = {
      @TNodeBase.NodeDisplayOptions.Attribute(name = "shape", value = "Msquare"),
    })
public class TMacroOperator extends TOperatorBase implements TCanBeSequencedProperty {

  @JsonCreator
  public TMacroOperator(
      @Nullable @JsonProperty(value = "id", required = true) UUID id,
      @Nonnull @JsonProperty(value = "op", required = true) String op) {
    super(id, op);
  }

  public TMacroOperator(@Nonnull String op) {
    this(null, op);
  }

  public TMacroOperator(@Nonnull TMacroOperator source) {
    this(source.id, source.op);
  }

  @Override
  public TMacroOperator copy() {
    return new TMacroOperator(this);
  }
}
