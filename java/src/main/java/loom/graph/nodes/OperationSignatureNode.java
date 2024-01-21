package loom.graph.nodes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Comparator;
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
import loom.graph.constraints.OperationReferenceAgreementConstraint;

@Jacksonized
@SuperBuilder
@Getter
@Setter
@LoomEnvironment.WithConstraints({OperationReferenceAgreementConstraint.class})
public final class OperationSignatureNode
    extends LoomNode<OperationSignatureNode, OperationSignatureNode.Body> {
  public static final String TYPE = "OperationSignature";

  @SuppressWarnings("unused")
  public abstract static class OperationSignatureNodeBuilder<
          C extends OperationSignatureNode, B extends OperationSignatureNodeBuilder<C, B>>
      extends LoomNodeBuilder<OperationSignatureNode, Body, C, B> {
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
            "kernel": {
                "type": "string"
            },
            "params": {
                "type": "object",
                "patternProperties": {
                    "^[a-zA-Z_][a-zA-Z0-9_]*$": {}
                },
                "additionalProperties": false
            },
            "inputs": { "$ref": "#/definitions/TensorSelectionMap" },
            "outputs": { "$ref": "#/definitions/TensorSelectionMap" }
        },
        "required": ["kernel"],
        "additionalProperties": false,
        "definitions": {
            "TensorSelectionMap": {
                "type": "object",
                "patternProperties": {
                    "^[a-zA-Z_][a-zA-Z0-9_]*$": {
                        "type": "array",
                        "items": { "$ref": "#/definitions/TensorSelection" },
                        "minItems": 1
                    }
                },
                "additionalProperties": false
            },
            "TensorSelection": {
                "type": "object",
                "properties": {
                    "tensorId": {
                        "type": "string",
                        "format": "uuid"
                    },
                    "range": { "$ref": "#/definitions/ZRange" }
                },
                "required": ["tensorId", "range"],
                "additionalProperties": false
            },
            "ZRange": {
                "type": "object",
                "properties": {
                   "start": { "$ref": "#/definitions/ZPoint" },
                   "end": { "$ref": "#/definitions/ZPoint" }
                },
                "required": ["start", "end"]
            },
            "ZPoint": {
                "type": "array",
                "items": {
                    "type": "integer"
                }
            }
        }
    }
    """)
  @Value
  @Jacksonized
  @Builder
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @JsonPropertyOrder({"kernel", "params", "inputs", "outputs"})
  public static class Body implements HasToJsonString {
    @JsonProperty(required = true)
    @Nonnull
    String kernel;

    @Singular Map<String, Object> params;
    @Singular @Nonnull Map<String, List<TensorSelection>> inputs;
    @Singular @Nonnull Map<String, List<TensorSelection>> outputs;
  }

  public static OperationSignatureNodeBuilder<?, ?> withBody(Consumer<Body.BodyBuilder> cb) {
    var bodyBuilder = Body.builder();
    cb.accept(bodyBuilder);
    return builder().body(bodyBuilder.build());
  }

  @Delegate(excludes = {HasToJsonString.class})
  @Nonnull
  private Body body;

  @JsonIgnore
  public List<ApplicationNode> getApplicationNodes() {
    var id = getId();
    return assertGraph()
        .nodeScan()
        .type(ApplicationNode.TYPE)
        .nodeClass(ApplicationNode.class)
        .asStream()
        .filter(appNode -> appNode.getOperationId().equals(id))
        .sorted(Comparator.comparing(LoomNode::getId))
        .toList();
  }
}
