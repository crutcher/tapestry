package loom.graph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeName;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

@JsonTypeName("BlockOperator")
@TNodeBase.NodeDisplayOptions.NodeAttributes(
    value = {
      @TNodeBase.NodeDisplayOptions.Attribute(name = "shape", value = "rarrow"),
    })
@JsonSubTypes(
    value = {
      @JsonSubTypes.Type(value = TMacroOperator.class),
    })
public class TBlockOperator extends TOperatorBase
    implements TCanBeSequencedProperty, TBlockIndex.THasBlockIndexProperty {

  @JsonCreator
  public TBlockOperator(
      @Nullable @JsonProperty(value = "id", required = true) UUID id,
      @Nonnull @JsonProperty(value = "op", required = true) String op) {
    super(id, op);
  }

  public TBlockOperator(@Nonnull String op) {
    this(null, op);
  }

  public TBlockOperator(@Nonnull TBlockOperator source) {
    this(source.id, source.op);
  }

  @Override
  public TBlockOperator copy() {
    return new TBlockOperator(this);
  }
}
