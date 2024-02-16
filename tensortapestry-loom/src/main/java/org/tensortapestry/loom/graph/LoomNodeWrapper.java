package org.tensortapestry.loom.graph;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.tensortapestry.common.collections.Wrapper;

/**
 * Wrapper for a LoomNode.
 */
public interface LoomNodeWrapper<WrapperT extends LoomNodeWrapper<WrapperT>>
  extends Wrapper<LoomNode> {
  /**
   * Get the ID of the node.
   *
   * @return the ID.
   */
  @Nonnull
  default UUID getId() {
    return unwrap().getId();
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  @CanIgnoreReturnValue
  default WrapperT withLabel(@Nonnull String label) {
    unwrap().setLabel(label);
    return (WrapperT) this;
  }
}
