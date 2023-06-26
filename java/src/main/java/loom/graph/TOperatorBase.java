package loom.graph;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import java.util.UUID;
import javax.annotation.Nullable;

@JsonSubTypes(
    value = {
      @JsonSubTypes.Type(value = TBlockOperator.class),
    })
public abstract class TOperatorBase extends TSequencedBase {
  protected TOperatorBase(@Nullable UUID id) {
    super(id);
  }
}
