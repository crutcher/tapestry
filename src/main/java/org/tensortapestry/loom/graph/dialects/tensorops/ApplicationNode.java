package org.tensortapestry.loom.graph.dialects.tensorops;

import javax.annotation.Nonnull;
import lombok.experimental.Delegate;
import org.tensortapestry.loom.graph.AbstractNodeWrapper;
import org.tensortapestry.loom.graph.LoomNode;

public class ApplicationNode extends AbstractNodeWrapper<ApplicationBody> {

  public static final String TYPE =
    "http://tensortapestry.org/schemas/loom/2024-01/node_types.jsd#/$defs/Application";

  public static final class Builder
    extends AbstractWrapperBuilder<ApplicationNode, Builder, ApplicationBody, ApplicationBody.ApplicationBodyBuilder> {

    private Builder() {
      super(
        TYPE,
        ApplicationBody::builder,
        ApplicationBody.ApplicationBodyBuilder::build,
        ApplicationNode::wrap
      );
    }
  }

  @Nonnull
  public static Builder builder() {
    return new Builder();
  }

  @Nonnull
  public static ApplicationNode wrap(@Nonnull LoomNode node) {
    return new ApplicationNode(node);
  }

  public ApplicationNode(@Nonnull LoomNode node) {
    super(node.assertType(TYPE), ApplicationBody.class);
  }

  @Delegate
  @SuppressWarnings("unused")
  private ApplicationBody delegateBodyMethods() {
    return getBody();
  }

  public OperationNode getOperationNode() {
    return assertGraph().assertNode(OperationNode::wrap, getOperationId());
  }
}
