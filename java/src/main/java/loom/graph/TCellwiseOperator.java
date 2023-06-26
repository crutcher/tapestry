package loom.graph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;

@JsonTypeName("Cellwise")
@TNodeBase.DisplayOptions.NodeAttributes(
    value = {
      @TNodeBase.DisplayOptions.Attribute(name = "shape", value = "cds"),
      @TNodeBase.DisplayOptions.Attribute(name = "fillcolor", value = "#a0d0d0"),
      @TNodeBase.DisplayOptions.Attribute(name = "margin", value = "0.15")
    })
public class TCellwiseOperator extends TOperatorBase {
  @Nonnull @Getter public final String op;

  @JsonCreator
  public TCellwiseOperator(
      @Nullable @JsonProperty(value = "id", required = true) UUID id,
      @Nonnull @JsonProperty(value = "op", required = true) String op) {
    super(id);
    this.op = op;
  }

  public TCellwiseOperator(@Nonnull String op) {
    this(null, op);
  }

  public TCellwiseOperator(@Nonnull TCellwiseOperator source) {
    this(source.id, source.op);
  }

  @Override
  public TCellwiseOperator copy() {
    return new TCellwiseOperator(this);
  }
}
