package loom.graph.nodes;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import loom.common.runtime.LazyString;
import loom.common.runtime.Thunk;
import loom.graph.LoomConstants;
import loom.graph.LoomEnvironment;
import loom.graph.LoomGraph;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;

/** Constraint that verifies that all inputs and outputs of OperationNodes are TensorNodes. */
public class OperationNodesSourcesAndResultsAreTensors implements LoomEnvironment.Constraint {
  @Override
  public void check(
      @SuppressWarnings("unused") LoomEnvironment env,
      LoomGraph graph,
      ValidationIssueCollector issueCollector) {

    for (var opNode : graph.iterableNodes(OperationNode.TYPE, OperationNode.class)) {
      checkOperation(graph, opNode, issueCollector);
    }
  }

  /**
   * Scan a single OperationNode for errors.
   *
   * @param graph the LoomGraph.
   * @param opNode the OperationNode to scan.
   * @param issueCollector the ValidationIssueCollector to add issues to.
   */
  @VisibleForTesting
  void checkOperation(
      LoomGraph graph, OperationNode opNode, ValidationIssueCollector issueCollector) {
    var lazyContexts = Thunk.of(() -> List.of(opNode.asContext("Operation")));
    checkIOMap(graph, opNode, "inputs", opNode.getInputs(), lazyContexts, issueCollector);
    checkIOMap(graph, opNode, "outputs", opNode.getOutputs(), lazyContexts, issueCollector);
  }

  /**
   * Scan a single input/output map for errors.
   *
   * @param graph the LoomGraph.
   * @param opNode the OperationNode that owns the map.
   * @param ioMapName the name of the map.
   * @param ioMap the map itself.
   * @param contextsSupplier a Supplier of a list of ValidationIssue.Contexts to add to each issue.
   * @param issueCollector the ValidationIssueCollector to add issues to.
   */
  @VisibleForTesting
  void checkIOMap(
      LoomGraph graph,
      OperationNode opNode,
      String ioMapName,
      Map<String, List<UUID>> ioMap,
      Supplier<List<ValidationIssue.Context>> contextsSupplier,
      ValidationIssueCollector issueCollector) {
    for (var entry : ioMap.entrySet()) {
      final var ioName = entry.getKey();
      final var refIds = entry.getValue();

      var relativeFieldPath = LazyString.format(".body.%s.%s", ioMapName, ioName);

      for (int itemIdx = 0; itemIdx < refIds.size(); ++itemIdx) {
        var itemId = refIds.get(itemIdx);

        var relItemPath = LazyString.format("%s[%d]", relativeFieldPath, itemIdx);

        if (!graph.hasNode(itemId)) {
          issueCollector.add(
              ValidationIssue.builder()
                  .type(LoomConstants.MISSING_NODE_ERROR)
                  .param("nodeType", OperationNode.TYPE)
                  .summary("Operation %s references non-existent node.".formatted(relItemPath))
                  .context(
                      ValidationIssue.Context.builder()
                          .name("Reference")
                          .message("Invalid reference to non-existent node.")
                          .jsonpath(opNode.getJsonPath(), relItemPath.get())
                          .data(itemId.toString()))
                  .withContexts(contextsSupplier.get()));
          continue;
        }

        var ioNode = graph.assertNode(itemId);
        if (!(ioNode instanceof TensorNode)) {
          issueCollector.add(
              ValidationIssue.builder()
                  .type(LoomConstants.NODE_VALIDATION_ERROR)
                  .summary("Operation %s references non-tensor node.".formatted(relItemPath))
                  .context(
                      ioNode.asContext(
                          "Reference Node", "The node referenced by %s".formatted(relItemPath)))
                  .withContexts(contextsSupplier.get()));
        }
      }
    }
  }
}
