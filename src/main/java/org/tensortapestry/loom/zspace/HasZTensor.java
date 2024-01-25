package org.tensortapestry.loom.zspace;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.annotation.Nonnull;
import org.tensortapestry.loom.zspace.indexing.IndexingFns;

public interface HasZTensor {
  @JsonIgnore
  ZTensor getTensor();

  @Nonnull
  default int[] commonBroadcastShape(@Nonnull int[] shape) {
    return IndexingFns.commonBroadcastShape(getTensor()._unsafeGetShape(), shape);
  }

  @Nonnull
  default int[] commonBroadcastShape(@Nonnull HasZTensor other) {
    return commonBroadcastShape(other.getTensor()._unsafeGetShape());
  }
}
