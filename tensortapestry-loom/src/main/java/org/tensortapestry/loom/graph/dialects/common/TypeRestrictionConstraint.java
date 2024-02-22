package org.tensortapestry.loom.graph.dialects.common;

import java.util.Set;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.tensortapestry.common.validation.ValidationIssue;
import org.tensortapestry.common.validation.ValidationIssueCollector;
import org.tensortapestry.loom.graph.LoomEnvironment;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.LoomNode;

@Value
@Builder(toBuilder = true)
public class TypeRestrictionConstraint
  implements LoomEnvironment.Constraint, LoomEnvironment.TypeSupportProvider {

  @Nonnull
  @Singular
  Set<String> nodeTypes;

  @Nonnull
  @Singular
  Set<String> tagTypes;

  @Override
  public boolean supportsNodeType(String type) {
    return nodeTypes.contains(type);
  }

  @Override
  public boolean supportsTagType(String type) {
    return tagTypes.contains(type);
  }

  @Override
  public void validateConstraint(
    LoomEnvironment env,
    LoomGraph graph,
    ValidationIssueCollector issueCollector
  ) {
    graph.getNodes().values().forEach(node -> checkNode(node, issueCollector));
  }

  private void checkNode(LoomNode node, ValidationIssueCollector issueCollector) {
    checkType(node, "node", node.getType(), nodeTypes, issueCollector);
    for (var type : node.getTags().keySet()) {
      checkType(node, "tag", type, tagTypes, issueCollector);
    }
  }

  private void checkType(
    LoomNode node,
    String description,
    String type,
    Set<String> validTypes,
    ValidationIssueCollector issueCollector
  ) {
    if (!validTypes.contains(type)) {
      issueCollector.addIssue(
        ValidationIssue
          .builder()
          .summary("Illegal %s type".formatted(description))
          .param("type", type)
          .context(ValidationIssue.Context.builder().name("Valid Types").data(nodeTypes))
          .context(node.asValidationContext("Node"))
      );
    }
  }
}
