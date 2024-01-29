package org.tensortapestry.loom.graph.dialects.common;

import javax.annotation.Nonnull;
import lombok.Value;
import lombok.experimental.Delegate;
import lombok.extern.jackson.Jacksonized;
import org.tensortapestry.loom.graph.AbstractNodeWrapper;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.LoomNode;

public final class NoteNode extends AbstractNodeWrapper<NoteNode.Body> {

  public static final String TYPE =
    "http://tensortapestry.org/schemas/loom/2024-01/node_types.jsd#/$defs/Note";

  public static final class Builder
    extends AbstractNodeWrapperBuilder<NoteNode, Builder, Body, Body.BodyBuilder> {

    private Builder() {
      super(TYPE, Body::builder, Body.BodyBuilder::build, NoteNode::wrap);
    }
  }

  @Value
  @Jacksonized
  @lombok.Builder
  @JsdType(TYPE)
  public static class Body {

    @Nonnull
    String message;
  }

  @Nonnull
  public static Builder builder() {
    return new Builder();
  }

  @Nonnull
  public static Builder builder(LoomGraph graph) {
    return new Builder().graph(graph);
  }

  @Nonnull
  public static NoteNode wrap(@Nonnull LoomNode node) {
    return new NoteNode(node);
  }

  public NoteNode(@Nonnull LoomNode node) {
    super(node.assertType(TYPE), Body.class);
  }

  @Delegate
  @SuppressWarnings("unused")
  private Body delegateBodyMethods() {
    return getBody();
  }
}
