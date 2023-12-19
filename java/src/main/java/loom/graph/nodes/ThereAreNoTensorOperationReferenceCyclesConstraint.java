package loom.graph.nodes;

import java.util.HashMap;
import loom.graph.CommonEnvironments;
import loom.graph.LoomConstants;
import loom.graph.LoomEnvironment;
import loom.graph.LoomGraph;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;

public class ThereAreNoTensorOperationReferenceCyclesConstraint
    implements LoomEnvironment.Constraint {

  @Override
  public void check(LoomEnvironment env, LoomGraph graph, ValidationIssueCollector issueCollector) {
    // Assuming TensorNode::AllTensorsHaveExactlyOneSourceOperation has already been run;
    // verify that there are no cycles in the graph.
    checkForCycles(graph, issueCollector);
  }

  public static void checkForCycles(LoomGraph graph, ValidationIssueCollector issueCollector) {
    for (var cycle : CommonEnvironments.findSimpleCycles(graph)) {
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

      issueCollector.add(
          ValidationIssue.builder()
              .type(LoomConstants.REFERENCE_CYCLE_ERROR)
              .summary("Reference Cycle detected")
              .context(b -> b.name("Cycle").data(cycleDesc)));
    }
  }
}
