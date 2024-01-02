package loom.graph.nodes;

import java.util.function.Consumer;
import javax.annotation.Nonnull;
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
import loom.zspace.ZRange;

@Jacksonized
@SuperBuilder
@Getter
@Setter
public class IPFIndexNode extends LoomNode<IPFIndexNode, IPFIndexNode.Body> {
  public static final String TYPE = "IPFIndexNode";

  public abstract static class IPFIndexNodeBuilder<
          C extends IPFIndexNode, B extends IPFIndexNodeBuilder<C, B>>
      extends LoomNodeBuilder<IPFIndexNode, Body, C, B> {
    {
      // Set the node type.
      type(TYPE);
    }
  }

  @WithSchema(
      """
    {
        "type": "object",
        "properties": {
            "range": { "$ref": "#/definitions/ZRange" }
        },
        "required": ["range"],
        "additionalProperties": false,
        "definitions": {
            "ZRange": {
                "type": "object",
                "properties": {
                    "start": { "$ref": "#/definitions/ZPoint" },
                    "end": { "$ref": "#/definitions/ZPoint" }
                },
                "required": ["start", "end"],
                "additionalProperties": false
            },
            "ZPoint": {
                "type": "array",
                "items": { "type": "number" },
            }
        }
    }
    """)
  @Value
  @Jacksonized
  @Builder
  public static class Body implements HasToJsonString {
    @Nonnull ZRange range;
  }

  public static IPFIndexNodeBuilder<?, ?> withBody(Consumer<Body.BodyBuilder> cb) {
    var bodyBuilder = Body.builder();
    cb.accept(bodyBuilder);
    return builder().body(bodyBuilder.build());
  }

  @Delegate(excludes = {HasToJsonString.class})
  private Body body;
}
