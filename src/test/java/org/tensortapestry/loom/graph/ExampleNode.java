package org.tensortapestry.loom.graph;

import java.util.function.Consumer;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.Delegate;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Jacksonized
@SuperBuilder
public final class ExampleNode extends LoomNode<ExampleNode, ExampleNode.Body> {

  public static final String TYPE = "ExampleNode";

  @SuppressWarnings("unused")
  public abstract static class ExampleNodeBuilder<
    C extends ExampleNode, B extends ExampleNodeBuilder<C, B>
  >
    extends LoomNodeBuilder<ExampleNode, Body, C, B> {
    {
      // Set the node type.
      type(TYPE);
    }
  }

  @Value
  @Jacksonized
  @Builder
  public static class Body {

    @Nonnull
    String foo;
  }

  public static ExampleNodeBuilder<?, ?> withBody(Consumer<Body.BodyBuilder> cb) {
    var bodyBuilder = Body.builder();
    cb.accept(bodyBuilder);
    return builder().body(bodyBuilder.build());
  }

  @Delegate
  @Nonnull
  private Body body;
}
