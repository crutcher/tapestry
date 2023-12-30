package loom.graph.nodes;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.*;
import lombok.experimental.Delegate;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.common.json.HasToJsonString;
import loom.common.json.WithSchema;
import loom.graph.LoomGraph;

/** Represents a node in the LoomGraph that represents an operation. */
@Jacksonized
@SuperBuilder
@Getter
@Setter
public final class OperationNode extends LoomGraph.Node<OperationNode, OperationNode.Body> {
  public static final String TYPE = "OperationNode";

  public abstract static class OperationNodeBuilder<
          C extends OperationNode, B extends OperationNodeBuilder<C, B>>
      extends NodeBuilder<OperationNode, Body, C, B> {
    {
      // Set the node type.
      type(TYPE);
    }
  }

  /**
   * Represents the body of an OperationNode. The body contains the operation name, parameters,
   * input nodes, and output nodes. This class is immutable and can be instantiated using the
   * OperationNode.BodyBuilder.
   */
  @Data
  @Builder
  @WithSchema(
      """
  {
      "type": "object",
      "opName": {
          "type": "string",
          "pattern": "^[a-zA-Z_][a-zA-Z0-9_]*$"
      },
      "properties": {
        "params": {
            "documentation": "Fixed operation parameters",
            "type": "object",
            "patternProperties": {
                "^[a-zA-Z_][a-zA-Z0-9_]*$": {}
            },
            "additionalProperties": false
        },
        "inputs": {
            "documentation": "Input tensors",
            "$ref": "#/definitions/NamedTensorIdListMap"
        },
        "outputs": {
            "documentation": "Output tensors",
            "$ref": "#/definitions/NamedTensorIdListMap"
        }
      },
      "required": ["opName", "inputs", "outputs"],
      "definitions": {
        "NamedTensorIdListMap": {
            "type": "object",
            "patternProperties": {
                "^[a-zA-Z_][a-zA-Z0-9_]*$": {
                    "type": "array",
                    "items": {
                        "type": "string",
                        "format": "uuid"
                    },
                    "minItems": 1
                }
            },
            "additionalProperties": false
        }
      }
  }
  """)
  public static final class Body implements HasToJsonString {
    @Nonnull private String opName;
    @Singular @Nullable private Map<String, Object> params;
    @Singular @Nonnull private Map<String, List<UUID>> inputs;
    @Singular @Nonnull private Map<String, List<UUID>> outputs;
  }

  /**
   * Get a resolved input nodes map.
   *
   * @return The map of input names to lists of nodes.
   */
  public Map<String, List<TensorNode>> getInputNodeListMap() {
    return GraphUtils.idListMapToNodeListMap(
        getGraph(), getInputs(), TensorNode.TYPE, TensorNode.class);
  }

  /**
   * Get a resolved output nodes map.
   *
   * @return The map of output names to lists of nodes.
   */
  public Map<String, List<TensorNode>> getOutputNodeListMap() {
    return GraphUtils.idListMapToNodeListMap(
        getGraph(), getOutputs(), TensorNode.TYPE, TensorNode.class);
  }

  public static OperationNodeBuilder<?, ?> withBody(Consumer<Body.BodyBuilder> cb) {
    var bodyBuilder = Body.builder();
    cb.accept(bodyBuilder);
    return builder().body(bodyBuilder.build());
  }

  @Delegate(excludes = {HasToJsonString.class})
  @Nonnull
  private Body body;
}
