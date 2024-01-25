package org.tensortapestry.loom.graph.dialects.tensorops;

import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.tensortapestry.loom.graph.LoomConstants;
import org.tensortapestry.loom.graph.LoomEnvironment;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.validation.ValidationIssue;
import org.tensortapestry.loom.validation.ValidationIssueCollector;

@Getter
@Builder
public class TensorDTypesAreValidConstraint implements LoomEnvironment.Constraint {

  @Singular
  private final Set<String> validDTypes;

  @Override
  public void checkRequirements(LoomEnvironment env) {
    env.assertClassForType(TensorOpNodes.TENSOR_NODE_TYPE, TensorNode.class);
  }

  @Override
  public void validateConstraint(
    LoomEnvironment env,
    LoomGraph graph,
    ValidationIssueCollector issueCollector
  ) {
    for (var tensorNode : graph
      .nodeScan()
      .type(TensorOpNodes.TENSOR_NODE_TYPE)
      .nodeClass(TensorNode.class)
      .asList()) {
      checkTensor(tensorNode, issueCollector);
    }
  }

  public void checkTensor(TensorNode tensorNode, ValidationIssueCollector issueCollector) {
    var dtype = tensorNode.getDtype();
    if (!validDTypes.contains(dtype)) {
      issueCollector.addIssue(
        ValidationIssue
          .builder()
          .type(LoomConstants.Errors.NODE_VALIDATION_ERROR)
          .param("nodeType", TensorOpNodes.TENSOR_NODE_TYPE)
          .context(
            ValidationIssue.Context
              .builder()
              .name("Tensor")
              .jsonpath(tensorNode.getJsonPath())
              .dataFromJson(tensorNode.toJsonString())
          )
          .summary("Tensor dtype (%s) not a recognized type", dtype)
          .build()
      );
    }
  }
}
