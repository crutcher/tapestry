package loom.graph.nodes;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import loom.graph.LoomConstants;
import loom.graph.LoomEnvironment;
import loom.graph.LoomGraph;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;

/** LoomEnvironment validation rule: All tensors must have exactly one source operation. */
public class AllTensorsHaveExactlyOneSourceOperationConstraint
    implements LoomEnvironment.Constraint {

  @Override
  public void checkRequirements(LoomEnvironment env) {
    if (env.lookupConstraint(OperationNodesSourcesAndResultsAreTensors.class) == null) {
      throw new IllegalStateException(
          "AllTensorsHaveExactlyOneSourceOperation requires OperationNodesSourcesAndResultsAreTensors");
    }
  }

  @Override
  public void checkConstraint(
      @SuppressWarnings("unused") LoomEnvironment env,
      LoomGraph graph,
      ValidationIssueCollector issueCollector) {
    for (var tensorNode : graph.iterableNodes(TensorNode.TYPE, TensorNode.class)) {
      checkTensor(tensorNode, issueCollector);
    }
  }

  /**
   * Check that a tensor has exactly one source operation.
   *
   * @param tensorNode the tensor node.
   * @param issueCollector the ValidationIssueCollector.
   */
  @VisibleForTesting
  static void checkTensor(TensorNode tensorNode, ValidationIssueCollector issueCollector) {
    final var nodeId = tensorNode.getId();

    List<OperationNode> operationSourceNodes =
        tensorNode.assertGraph().stream(OperationNode.TYPE, OperationNode.class)
            .filter(op -> op.getOutputs().values().stream().anyMatch(ids -> ids.contains(nodeId)))
            .toList();

    if (operationSourceNodes.size() == 1) {
      // This is the expected case.
      return;
    }

    String desc = "Tensor";
    if (tensorNode.getLabel() != null) {
      desc = "%s (%s)".formatted(desc, tensorNode.getLabel());
    }

    var issueBuilder =
        ValidationIssue.builder()
            .type(LoomConstants.NODE_VALIDATION_ERROR)
            .param("nodeType", TensorNode.TYPE)
            .context(tensorNode.asContext("Tensor"));

    if (operationSourceNodes.isEmpty()) {
      issueBuilder.summary("%s has no Operation source".formatted(desc));

    } else {
      issueBuilder
          .summary(
              "%s has too many Operation sources: %d".formatted(desc, operationSourceNodes.size()))
          .message("Tensor id: %s".formatted(tensorNode.getId()));

      // Sort the sources by ID so that the order is deterministic.
      operationSourceNodes = new ArrayList<>(operationSourceNodes);
      operationSourceNodes.sort(
          Comparator.comparing(n -> n.getLabel() == null ? n.getId().toString() : n.getLabel()));

      for (int idx = 0; idx < operationSourceNodes.size(); idx++) {
        var operationNode = operationSourceNodes.get(idx);

        var name = "Source Operation #" + idx;
        if (operationNode.getLabel() != null) {
          name = "%s (%s)".formatted(name, operationNode.getLabel());
        }

        issueBuilder.context(operationNode.asContext(name));
      }
    }

    issueCollector.add(issueBuilder);
  }
}
