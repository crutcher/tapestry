package org.tensortapestry.loom.graph.dialects.tensorops;

import javax.annotation.Nonnull;
import lombok.experimental.Delegate;
import org.tensortapestry.loom.graph.AbstractNodeWrapper;
import org.tensortapestry.loom.graph.IterableStreamable;
import org.tensortapestry.loom.graph.LoomNode;

public final class OperationNode extends AbstractNodeWrapper<OperationBody> {

  public static final String TYPE =
    "http://tensortapestry.org/schemas/loom/2024-01/node_types.jsd#/$defs/Operation";

  public static final class Builder
    extends AbstractWrapperBuilder<OperationNode, Builder, OperationBody, OperationBody.OperationBodyBuilder> {

    private Builder() {
      super(
        TYPE,
        OperationBody::builder,
        OperationBody.OperationBodyBuilder::build,
        OperationNode::wrap
      );
    }
  }

  @Nonnull
  public static Builder builder() {
    return new Builder();
  }

  @Nonnull
  public static OperationNode wrap(@Nonnull LoomNode node) {
    return new OperationNode(node);
  }

  public OperationNode(@Nonnull LoomNode node) {
    super(node.assertType(TYPE), OperationBody.class);
  }

  @Delegate
  @SuppressWarnings("unused")
  private OperationBody delegateBodyMethods() {
    return getBody();
  }

  public IterableStreamable<ApplicationNode> getApplicationNodes() {
    var id = getId();
    return () ->
      assertGraph()
        .byType(ApplicationNode.TYPE, ApplicationNode::wrap)
        .stream()
        .filter(n -> n.getOperationId().equals(id))
        .iterator();
  }
}
