package org.tensortapestry.loom.graph.dialects.tensorops.constraints;

import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.tensortapestry.common.validation.ValidationIssue;
import org.tensortapestry.common.validation.ValidationIssueCollector;
import org.tensortapestry.loom.graph.LoomConstants;
import org.tensortapestry.loom.graph.LoomEnvironment;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.LoomNode;
import org.tensortapestry.loom.graph.dialects.tensorops.TensorNode;

/**
 * This constraint checks that the dtype of a Tensor node is a valid type.
 */
@Getter
@Builder
public class TensorDTypesAreValidConstraint implements LoomEnvironment.Constraint {

  @Singular
  private final Set<String> validDTypes;

  @Override
  public void checkRequirements(LoomEnvironment env) {
    env.supportsNodeType(TensorNode.TYPE);
  }

  @Override
  public void validateConstraint(
    LoomEnvironment env,
    LoomGraph graph,
    ValidationIssueCollector issueCollector
  ) {
    for (var node : graph) {
      if (node.getType().equals(TensorNode.TYPE)) {
        checkTensor(node, issueCollector);
      }
    }
  }

  public void checkTensor(LoomNode tensor, ValidationIssueCollector issueCollector) {
    var tensorBody = tensor.viewBodyAs(TensorNode.Body.class);
    var dtype = tensorBody.getDtype();
    if (!validDTypes.contains(dtype)) {
      issueCollector.addIssue(
        ValidationIssue
          .builder()
          .type(LoomConstants.Errors.NODE_VALIDATION_ERROR)
          .param("nodeType", TensorNode.TYPE)
          .context(
            ValidationIssue.Context
              .builder()
              .name("Tensor")
              .jsonpath(tensor.getJsonPath())
              .dataFromJson(tensor.toJsonString())
          )
          .summary("Tensor dtype (%s) not a recognized type", dtype)
          .build()
      );
    }
  }
}
