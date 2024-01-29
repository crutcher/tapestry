package org.tensortapestry.loom.graph;

import java.util.UUID;
import javax.annotation.Nonnull;
import org.tensortapestry.loom.common.collections.Wrapper;

public interface NodeWrapper extends Wrapper<LoomNode> {
  @Nonnull
  default UUID getId() {
    return unwrap().getId();
  }

  @Nonnull
  LoomNode unwrap();
}
