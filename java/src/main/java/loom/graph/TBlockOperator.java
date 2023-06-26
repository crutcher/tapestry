package loom.graph;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;

@JsonTypeName("BlockOperator")
@TNodeBase.DisplayOptions.NodeAttributes(
    value = {
      @TNodeBase.DisplayOptions.Attribute(name = "shape", value = "cds"),
      @TNodeBase.DisplayOptions.Attribute(name = "fillcolor", value = "#E5E8E8"),
      @TNodeBase.DisplayOptions.Attribute(name = "margin", value = "0.15")
    })
public class TBlockOperator extends TOperatorBase {
  @Nonnull @Getter public final String op;

  public TBlockOperator(@Nullable UUID id, @Nonnull String op) {
    super(id);
    this.op = op;
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
