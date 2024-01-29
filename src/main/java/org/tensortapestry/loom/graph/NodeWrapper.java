package org.tensortapestry.loom.graph;

import java.util.UUID;
import javax.annotation.Nonnull;

public interface NodeWrapper {
  @Nonnull
  UUID getId();

  @Nonnull
  LoomNode unwrap();
}
