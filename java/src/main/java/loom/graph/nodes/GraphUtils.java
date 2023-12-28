package loom.graph.nodes;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.NoArgsConstructor;
import loom.graph.LoomGraph;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class GraphUtils {
  /**
   * Convert a map of input/output node lists to a map of input/output UUID lists.
   *
   * @param inputs The map of input/output names to lists of nodes.
   * @return The map of input/output names to lists of UUIDs.
   */
  public static Map<String, List<UUID>> nodeListMapToIdListMap(
      Map<String, List<LoomGraph.Node<?, ?>>> inputs) {
    return inputs.entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, e -> e.getValue().stream().map(LoomGraph.Node::getId).toList()));
  }

  /**
   * Convert a map of input/output UUID lists to a map of input/output node lists.
   *
   * @param graph The graph to use to resolve the UUIDs.
   * @param inputs The map of input/output names to lists of UUIDs.
   * @return The map of input/output names to lists of nodes.
   */
  public static <T extends LoomGraph.Node<T, ?>> Map<String, List<T>> idListMapToNodeListMap(
      LoomGraph graph,
      Map<String, List<UUID>> inputs,
      @Nullable String nodeType,
      Class<T> nodeClass) {
    return inputs.entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                e ->
                    e.getValue().stream()
                        .map(id -> graph.assertNode(id, nodeType, nodeClass))
                        .toList()));
  }

  /**
   * Get the operation node that produces this tensor.
   *
   * @return the operation node.
   */
  public static OperationNode getSourceNode(TensorNode tensorNode) {
    // This assumes that there is only one source operation node.
    var id = tensorNode.getId();
    return tensorNode.assertGraph().stream(OperationNode.TYPE, OperationNode.class)
        .filter(op -> op.getOutputs().values().stream().anyMatch(ids -> ids.contains(id)))
        .findFirst()
        .orElseThrow();
  }

  /**
   * Get the operation node ID that produces this tensor.
   *
   * @return the operation node ID.
   */
  public static UUID getSourceId(TensorNode tensorNode) {
    return getSourceNode(tensorNode).getId();
  }
}
