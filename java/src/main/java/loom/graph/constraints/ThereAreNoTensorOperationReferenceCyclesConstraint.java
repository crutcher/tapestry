package loom.graph.constraints;

import java.util.HashMap;
import loom.graph.*;
import loom.graph.nodes.OperationNode;
import loom.graph.nodes.TensorNode;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;

public class ThereAreNoTensorOperationReferenceCyclesConstraint implements LoomConstraint {

  @Override
  public void checkRequirements(LoomEnvironment env) {
    env.assertNodeTypeClass(TensorNode.TYPE, TensorNode.class);
    env.assertNodeTypeClass(OperationNode.TYPE, OperationNode.class);
    env.assertConstraint(AllTensorsHaveExactlyOneSourceOperationConstraint.class);
  }

  @Override
  public void validateConstraint(
      LoomEnvironment env, LoomGraph graph, ValidationIssueCollector issueCollector) {
    // Assuming TensorNode::AllTensorsHaveExactlyOneSourceOperation has already been run;
    // verify that there are no cycles in the graph.
    checkForCycles(graph, issueCollector);
  }

  public static void checkForCycles(LoomGraph graph, ValidationIssueCollector issueCollector) {
    for (var cycle : TraversalUtils.findOperationSimpleCycles(graph)) {
      var cycleDesc =
          cycle.stream()
              .map(
                  item -> {
                    var desc = new HashMap<>();
                    desc.put("id", item.getId());
                    desc.put("type", item.getType());
                    if (item.getLabel() != null) {
                      desc.put("label", item.getLabel());
                    }
                    return desc;
                  })
              .toList();

      issueCollector.addIssue(
          ValidationIssue.builder()
              .type(LoomConstants.REFERENCE_CYCLE_ERROR)
              .summary("Reference Cycle detected")
              .context(b -> b.name("Cycle").data(cycleDesc)));
    }
  }
}
