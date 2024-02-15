package org.tensortapestry.loom.graph.dialects.tensorops;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.annotation.Nonnull;

public interface TensorSelectionSupplier {
  @Nonnull
  @JsonIgnore
  TensorSelection getTensorSelection();
}
