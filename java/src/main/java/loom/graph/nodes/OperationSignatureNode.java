package loom.graph.nodes;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.Delegate;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.common.json.HasToJsonString;
import loom.common.json.WithSchema;
import loom.graph.LoomNode;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

@Jacksonized
@SuperBuilder
@Getter
@Setter
public class OperationSignatureNode
    extends LoomNode<OperationSignatureNode, OperationSignatureNode.Body> {
  public static final String TYPE = "OperationSignature";

  public abstract static class OperationSignatureNodeBuilder<
          C extends OperationSignatureNode, B extends OperationSignatureNodeBuilder<C, B>>
      extends LoomNodeBuilder<OperationSignatureNode, Body, C, B> {
    {
      // Set the node type.
      type(TYPE);
    }
  }

  @Value
  @Jacksonized
  @Builder
  @WithSchema(
      """
    {
        "type": "object",
        "properties": {
            "name": {
                "type": "string",
                "format": "uuid"
            }
        },
        "required": ["name"],
    }
    """)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class Body implements HasToJsonString {
    String name;
  }

  public static OperationSignatureNodeBuilder<?, ?> withBody(Consumer<Body.BodyBuilder> cb) {
    var bodyBuilder = Body.builder();
    cb.accept(bodyBuilder);
    return builder().body(bodyBuilder.build());
  }

  @Delegate(excludes = {HasToJsonString.class})
  @Nonnull
  private Body body;
}
