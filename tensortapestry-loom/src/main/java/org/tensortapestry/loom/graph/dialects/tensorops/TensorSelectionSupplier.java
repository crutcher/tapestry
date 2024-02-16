package org.tensortapestry.loom.graph.dialects.tensorops;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.annotation.Nonnull;
import org.tensortapestry.zspace.HasDimension;

public interface TensorSelectionSupplier extends HasDimension {
  @Nonnull
  @JsonIgnore
  TensorSelection getTensorSelection();
}
