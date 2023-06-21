package loom.graph;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.UUID;
import javax.annotation.Nullable;

@JsonTypeName("operator")
public abstract class TOperator extends TNode {
  TOperator(@Nullable UUID id) {
    super(id);
  }
}
