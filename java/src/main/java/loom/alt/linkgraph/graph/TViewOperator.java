package loom.alt.linkgraph.graph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@JsonTypeName("View")
@TNodeBase.NodeDisplayOptions.NodeAttributes(
    value = {
      @TNodeBase.NodeDisplayOptions.Attribute(name = "shape", value = "parallelogram"),
      @TNodeBase.NodeDisplayOptions.Attribute(name = "margin", value = "0"),
    })
public class TViewOperator extends TAlignedOperatorBase {
  @JsonCreator
  public TViewOperator(
      @Nullable @JsonProperty(value = "id", required = true) UUID id,
      @Nonnull @JsonProperty(value = "op", required = true) String op) {
    super(id, op);
  }

  public TViewOperator(@Nonnull String op) {
    this(null, op);
  }

  public TViewOperator(@Nonnull TViewOperator source) {
    this(source.id, source.op);
  }

  @Override
  public TViewOperator copy() {
    return new TViewOperator(this);
  }
}
