package loom.graph.nodes;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import lombok.*;
import lombok.experimental.Delegate;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.common.json.HasToJsonString;
import loom.common.json.WithSchema;
import loom.graph.LoomNode;

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
            },
            "params": {
                "type": "object",
                "patternProperties": {
                    "^[a-zA-Z_][a-zA-Z0-9_]*$": {}
                },
                "additionalProperties": false
            }
        },
        "required": ["name"],
        "additionalProperties": false
    }
    """)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class Body implements HasToJsonString {
    String name;
    @Singular Map<String, Object> params;
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
