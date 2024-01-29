package org.tensortapestry.loom.graph.dialects.common;

import javax.annotation.Nonnull;
import lombok.experimental.Delegate;
import org.tensortapestry.loom.graph.AbstractNodeWrapper;
import org.tensortapestry.loom.graph.LoomNode;

public class NoteNode extends AbstractNodeWrapper<NoteBody> {

  public static final String TYPE =
    "http://tensortapestry.org/schemas/loom/2024-01/node_types.jsd#/$defs/Note";

  public static final class Builder
    extends AbstractWrapperBuilder<NoteNode, Builder, NoteBody, NoteBody.NoteBodyBuilder> {

    private Builder() {
      super(TYPE, NoteBody::builder, NoteBody.NoteBodyBuilder::build, NoteNode::wrap);
    }
  }

  @Nonnull
  public static Builder builder() {
    return new Builder();
  }

  @Nonnull
  public static NoteNode wrap(@Nonnull LoomNode node) {
    return new NoteNode(node);
  }

  public NoteNode(@Nonnull LoomNode node) {
    super(node.assertType(TYPE), NoteBody.class);
  }

  @Delegate
  @SuppressWarnings("unused")
  private NoteBody delegateBodyMethods() {
    return getBody();
  }
}
