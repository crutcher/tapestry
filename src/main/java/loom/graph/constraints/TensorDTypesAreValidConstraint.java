package loom.graph.constraints;

import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import loom.graph.LoomConstants;
import loom.graph.LoomEnvironment;
import loom.graph.LoomGraph;
import loom.graph.nodes.TensorNode;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;

@Getter
@Builder
public class TensorDTypesAreValidConstraint implements LoomEnvironment.Constraint {

  @Singular
  private final Set<String> validDTypes;

  @Override
  public void checkRequirements(LoomEnvironment env) {
    env.assertClassForType(TensorNode.TYPE, TensorNode.class);
  }

  @Override
  public void validateConstraint(
    LoomEnvironment env,
    LoomGraph graph,
    ValidationIssueCollector issueCollector
  ) {
    for (var tensorNode : graph
      .nodeScan()
      .type(TensorNode.TYPE)
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
          .type(LoomConstants.NODE_VALIDATION_ERROR)
          .param("nodeType", TensorNode.TYPE)
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
