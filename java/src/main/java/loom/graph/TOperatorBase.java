package loom.graph;

import java.util.UUID;
import javax.annotation.Nullable;

public abstract class TOperatorBase extends TSequencedBase {
  TOperatorBase(@Nullable UUID id) {
    super(id);
  }
}
