package org.tensortapestry.loom.graph.dialects.tensorops;

import javax.annotation.Nonnull;
import lombok.experimental.Delegate;
import org.tensortapestry.loom.graph.AbstractNodeWrapper;
import org.tensortapestry.loom.graph.LoomNode;

public final class TensorNode extends AbstractNodeWrapper<TensorBody> {

  public static final String TYPE =
    "http://tensortapestry.org/schemas/loom/2024-01/node_types.jsd#/$defs/Tensor";

  public static final class Builder
    extends AbstractWrapperBuilder<TensorNode, Builder, TensorBody, TensorBody.TensorBodyBuilder> {

    private Builder() {
      super(TYPE, TensorBody::builder, TensorBody.TensorBodyBuilder::build, TensorNode::wrap);
    }
  }

  @Nonnull
  public static Builder builder() {
    return new Builder();
  }

  @Nonnull
  public static TensorNode wrap(@Nonnull LoomNode node) {
    return new TensorNode(node);
  }

  public TensorNode(@Nonnull LoomNode loomNode) {
    super(loomNode.assertType(TYPE), TensorBody.class);
  }

  @Delegate
  @SuppressWarnings("unused")
  private TensorBody delegateBodyMethods() {
    return getBody();
  }
}
