package org.tensortapestry.loom.graph.dialects.tensorops;

import java.util.HashMap;
import org.tensortapestry.common.validation.ValidationIssue;
import org.tensortapestry.common.validation.ValidationIssueCollector;
import org.tensortapestry.loom.graph.*;

public class NoTensorOperationCyclesConstraint implements LoomEnvironment.Constraint {

  @Override
  public void checkRequirements(LoomEnvironment env) {
    env.assertSupportsNodeType(TensorNode.TYPE);
    env.assertSupportsNodeType(OperationNode.TYPE);
    env.assertConstraint(TensorOperationAgreementConstraint.class);
  }

  @Override
  public void validateConstraint(
    @SuppressWarnings("unused") LoomEnvironment env,
    LoomGraph graph,
    ValidationIssueCollector issueCollector
  ) {
    boolean valid = true;

    if (valid) {
      for (var cycle : TraversalUtils.findOperationSimpleCycles(graph)) {
        var cycleDesc = cycle
          .stream()
          .map(item -> {
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
          ValidationIssue
            .builder()
            .type(LoomConstants.Errors.REFERENCE_CYCLE_ERROR)
            .summary("Reference Cycle detected")
            .context(b -> b.name("Cycle").data(cycleDesc))
        );
      }
    }
  }
}