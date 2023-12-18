package loom.graph.nodes;

import java.util.function.Consumer;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.graph.LoomGraph;

@Jacksonized
@SuperBuilder
@Getter
@Setter
public final class NoteNode extends LoomGraph.Node<NoteNode, NoteNode.Body> {
  public static final String TYPE = "NoteNode";

  public static final String BODY_SCHEMA =
      """
      {
          "type": "object",
          "properties": {
            "message": {
                "type": "string"
            }
          },
          "required": ["message"]
      }
      """;

  @Data
  @Jacksonized
  @Builder
  public static final class Body {

    @Nonnull private String message;
  }

  public abstract static class NoteNodeBuilder<C extends NoteNode, B extends NoteNodeBuilder<C, B>>
      extends NodeBuilder<NoteNode, Body, C, B> {
    {
      type(TYPE);
    }
  }

  @Builder
  @Getter
  public static final class Prototype extends LoomGraph.NodePrototype<NoteNode, Body> {

    @Builder
    public Prototype() {
      super(NoteNode.class, Body.class, BODY_SCHEMA);
    }
  }

  public static NoteNodeBuilder<?, ?> withBody(Consumer<Body.BodyBuilder> cb) {
    var bodyBuilder = Body.builder();
    cb.accept(bodyBuilder);
    return builder().body(bodyBuilder.build());
  }

  @Delegate @Nonnull private Body body;
}
