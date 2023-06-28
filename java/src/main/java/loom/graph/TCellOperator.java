package loom.graph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@JsonTypeName("CellOperator")
public class TCellOperator extends TAlignedOperatorBase {
  @JsonCreator
  public TCellOperator(
      @Nullable @JsonProperty(value = "id", required = true) UUID id,
      @Nonnull @JsonProperty(value = "op", required = true) String op) {
    super(id, op);
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
