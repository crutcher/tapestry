package loom.graph.nodes;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Singular;
import lombok.experimental.Delegate;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.graph.LoomGraph;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Jacksonized
@SuperBuilder
public final class OperationNode extends LoomGraph.Node<OperationNode, OperationNode.Body> {

  /**
   * Convert a map of input/output node lists to a map of input/output UUID lists.
   *
   * @param inputs The map of input/output names to lists of nodes.
   * @return The map of input/output names to lists of UUIDs.
   */
  public static Map<String, List<UUID>> nodeMapToIdMap(Map<String, List<TensorNode>> inputs) {
    return inputs.entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, e -> e.getValue().stream().map(TensorNode::getId).toList()));
  }

  /**
   * Convert a map of input/output UUID lists to a map of input/output node lists.
   *
   * @param graph The graph to use to resolve the UUIDs.
   * @param inputs The map of input/output names to lists of UUIDs.
   * @return The map of input/output names to lists of nodes.
   */
  public static Map<String, List<TensorNode>> idMapToNodeMap(
      LoomGraph graph, Map<String, List<UUID>> inputs) {
    return inputs.entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                e ->
                    e.getValue().stream()
                        .map(id -> graph.assertNode(id, TensorNode.Meta.TYPE, TensorNode.class))
                        .toList()));
  }

  @Data
  @SuperBuilder
  public static final class Body {
    @Nonnull private String opName;
    @Singular @Nonnull private Map<String, List<UUID>> inputs;
    @Singular @Nonnull private Map<String, List<UUID>> outputs;
  }

  @Override
  public Class<Body> getBodyClass() {
    return Body.class;
  }

  @Builder
  @Getter
  public static final class Meta extends LoomGraph.NodeMeta<OperationNode, Body> {
    public static final String TYPE = "OperationNode";

    public static final String BODY_SCHEMA =
        """
                {
                    "type": "object",
                    "opName": {
                        "type": "string",
                        "pattern": "^[a-zA-Z_][a-zA-Z0-9_]*$"
                    },
                    "properties": {
                      "inputs": {
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
                      },
                      "outputs": {
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
                    },
                    "required": ["opName", "inputs", "outputs"]
                }
                """;

    @Builder
    public Meta() {
      super(OperationNode.class, Body.class, BODY_SCHEMA);
    }
  }

  /** Exists to support {@code @Delegate} for {@code getBody()}. */
  @SuppressWarnings("unused")
  @Delegate
  private Body delegateProvider() {
    return getBody();
  }

  /**
   * Get a resolved input nodes map.
   *
   * @return The map of input names to lists of nodes.
   */
  public Map<String, List<TensorNode>> getInputNodes() {
    return idMapToNodeMap(getGraph(), getInputs());
  }

  /**
   * Get a resolved output nodes map.
   *
   * @return The map of output names to lists of nodes.
   */
  public Map<String, List<TensorNode>> getOutputNodes() {
    return idMapToNodeMap(getGraph(), getOutputs());
  }
}
