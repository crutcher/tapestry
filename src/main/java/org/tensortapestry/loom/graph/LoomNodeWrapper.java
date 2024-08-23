package org.tensortapestry.loom.graph;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.tensortapestry.common.collections.Wrapper;

/**
 * Wrapper for a LoomNode.
 */
public interface LoomNodeWrapper<WrapperT extends LoomNodeWrapper<WrapperT>>
  extends Wrapper<LoomNode> {
  @Nonnull
  default LoomGraph assertGraph() {
    return unwrap().assertGraph();
  }

  @Nonnull
  default LoomEnvironment assertEnvironment() {
    return unwrap().assertEnvironment();
  }

  /**
   * Get the ID of the node.
   *
   * @return the ID.
   */
  @Nonnull
  default UUID getId() {
    return unwrap().getId();
  }

  /**
   * Get the label of the node.
   *
   * @return the label.
   */
  @Nullable default String getLabel() {
    return unwrap().getLabel();
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  @CanIgnoreReturnValue
  default WrapperT withLabel(@Nonnull String label) {
    unwrap().setLabel(label);
    return (WrapperT) this;
  }
}
