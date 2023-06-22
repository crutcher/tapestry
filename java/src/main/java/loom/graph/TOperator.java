package loom.graph;

import java.util.UUID;
import javax.annotation.Nullable;

public abstract class TOperator extends TSequencedBase {
  TOperator(@Nullable UUID id) {
    super(id);
  }
}
