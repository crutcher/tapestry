package loom.graph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@JsonTypeName("Fusion")
@TNodeBase.NodeDisplayOptions.NodeAttributes(
    value = {
      @TNodeBase.NodeDisplayOptions.Attribute(name = "shape", value = "component"),
    })
public class TFusionOperator extends TAlignedOperatorBase {
  @JsonCreator
  public TFusionOperator(
      @Nullable @JsonProperty(value = "id", required = true) UUID id,
      @Nonnull @JsonProperty(value = "op", required = true) String op) {
    super(id, op);
  }

  public TFusionOperator(@Nonnull String op) {
    this(null, op);
  }

  public TFusionOperator(@Nonnull TFusionOperator source) {
    this(source.id, source.op);
  }

  @Override
  public TFusionOperator copy() {
    return new TFusionOperator(this);
  }
}
