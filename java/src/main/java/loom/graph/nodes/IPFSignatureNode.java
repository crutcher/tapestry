package loom.graph.nodes;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import lombok.*;
import lombok.experimental.Delegate;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.common.json.HasToJsonString;
import loom.common.json.WithSchema;
import loom.graph.LoomEnvironment;
import loom.graph.LoomNode;
import loom.graph.constraints.IPFSignatureAgreementConstraint;
import loom.polyhedral.IndexProjectionFunction;

@Jacksonized
@SuperBuilder
@Getter
@Setter
@LoomEnvironment.WithConstraints({IPFSignatureAgreementConstraint.class})
public class IPFSignatureNode extends LoomNode<IPFSignatureNode, IPFSignatureNode.Body> {
  public static final String TYPE = "IPFSignatureNode";

  public abstract static class IPFSignatureNodeBuilder<
          C extends IPFSignatureNode, B extends IPFSignatureNodeBuilder<C, B>>
      extends LoomNodeBuilder<IPFSignatureNode, Body, C, B> {
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
            "inputs": { "$ref": "#/definitions/IPFMap" },
            "outputs": { "$ref": "#/definitions/IPFMap" }
        },
        "required": ["inputs", "outputs"],
        "additionalProperties": false,
        "definitions": {
            "IPFMap": {
                "type": "object",
                "patternProperties": {
                  "^[a-zA-Z_][a-zA-Z0-9_]*$": {
                        "type": "array",
                        "items": { "$ref": "#/definitions/IndexProjectionFunction" }
                    }
                }
            },
            "IndexProjectionFunction": {
                "type": "object",
                "properties": {
                    "affineMap": { "$ref": "#/definitions/ZAffineMap" },
                    "shape": { "$ref": "#/definitions/ZVector" }
                },
                "required": ["affineMap", "shape"],
                "additionalProperties": false
            },
            "ZAffineMap": {
                "type": "object",
                "properties": {
                    "A": { "$ref": "#/definitions/ZMatrix" },
                    "b": { "$ref": "#/definitions/ZVector" }
                },
                "required": ["A", "b"],
                "additionalProperties": false
            },
            "ZVector": {
                "type": "array",
                "items": {
                  "type": "integer"
                }
            },
            "ZMatrix": {
                "description": "A matrix of integers; must be non-ragged",
                "type": "array",
                "items": {
                    "type": "array",
                    "items": {
                        "type": "integer"
                    }
                }
            }
          }
        }
    }
    """)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class Body implements HasToJsonString {
    @Singular @Nonnull Map<String, List<IndexProjectionFunction>> inputs;
    @Singular @Nonnull Map<String, List<IndexProjectionFunction>> outputs;
  }

  public static IPFSignatureNodeBuilder<?, ?> withBody(Consumer<Body.BodyBuilder> cb) {
    var bodyBuilder = Body.builder();
    cb.accept(bodyBuilder);
    return builder().body(bodyBuilder.build());
  }

  @Delegate(excludes = {HasToJsonString.class})
  @Nonnull
  private Body body;
}
