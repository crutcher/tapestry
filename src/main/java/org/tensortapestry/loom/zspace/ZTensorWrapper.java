package org.tensortapestry.loom.zspace;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.annotation.Nonnull;
import org.tensortapestry.loom.zspace.indexing.IndexingFns;

/**
 * Objects which can be represented as a ZTensor.
 */
public interface ZTensorWrapper {
  /**
   * Returns the ZTensor representation of this object.
   * @return the ZTensor.
   */
  @JsonIgnore
  ZTensor unwrap();

  /**
   * Returns the common broadcast shape of this tensor and the given shape.
   * @param shape the shape.
   * @return the common broadcast shape.
   */
  @Nonnull
  default int[] commonBroadcastShape(@Nonnull int[] shape) {
    return IndexingFns.commonBroadcastShape(unwrap()._unsafeGetShape(), shape);
  }

  /**
   * Returns the common broadcast shape of this tensor and the given tensor.
   * @param other the tensor.
   * @return the common broadcast shape.
   */
  @Nonnull
  default int[] commonBroadcastShape(@Nonnull ZTensorWrapper other) {
    return commonBroadcastShape(other.unwrap()._unsafeGetShape());
  }
}
