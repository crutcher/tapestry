package org.tensortapestry.loom.graph.dialects.tensorops.constraints;

import java.util.HashMap;
import javax.annotation.Nonnull;
import org.tensortapestry.common.validation.ValidationIssue;
import org.tensortapestry.common.validation.ValidationIssueCollector;
import org.tensortapestry.loom.graph.*;
import org.tensortapestry.loom.graph.dialects.tensorops.OperationNode;
import org.tensortapestry.loom.graph.dialects.tensorops.TensorNode;

/**
 * This constraint checks for cycles in the graph that involve both Tensor and Operation nodes.
 */
public class NoTensorOperationCyclesConstraint implements LoomEnvironment.Constraint {

  @Override
  public void checkRequirements(LoomEnvironment env) {
    env.assertSupportsNodeType(TensorNode.TYPE);
    env.assertSupportsNodeType(OperationNode.TYPE);
    env.assertConstraint(TensorOperationAgreementConstraint.class);
  }

  @Override
  public void validateConstraint(
    @Nonnull @SuppressWarnings("unused") LoomEnvironment env,
    @Nonnull LoomGraph graph,
    @Nonnull ValidationIssueCollector issueCollector
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
