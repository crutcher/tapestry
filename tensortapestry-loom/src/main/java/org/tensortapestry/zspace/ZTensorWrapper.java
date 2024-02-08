package org.tensortapestry.zspace;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.annotation.Nonnull;
import org.tensortapestry.zspace.indexing.IndexingFns;

/**
 * Objects which can be represented as a ZTensor.
 */
public interface ZTensorWrapper {
  /**
   * Returns the ZTensor representation of this object.
   *
   * @return the ZTensor.
   */
  @JsonIgnore
  @Nonnull
  ZTensor unwrap();

  /**
   * Is this tensor all zero?
   *
   * @return true if all zero.
   */
  default boolean allZero() {
    return unwrap().allMatch(x -> x == 0);
  }

  /**
   * Returns the common broadcast shape of this tensor and the given shape.
   *
   * @param shape the shape.
   * @return the common broadcast shape.
   */
  @Nonnull
  default int[] commonBroadcastShape(@Nonnull int[] shape) {
    return IndexingFns.commonBroadcastShape(unwrap()._unsafeGetShape(), shape);
  }

  /**
   * Returns the common broadcast shape of this tensor and the given tensor.
   *
   * @param other the tensor.
   * @return the common broadcast shape.
   */
  @Nonnull
  default int[] commonBroadcastShape(@Nonnull ZTensorWrapper other) {
    return commonBroadcastShape(other.unwrap()._unsafeGetShape());
  }
}
