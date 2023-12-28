package loom.graph;

import loom.common.runtime.ExcludeFromJacocoGeneratedReport;
import loom.validation.ValidationIssueCollector;

@FunctionalInterface
public interface LoomConstraint {
  @ExcludeFromJacocoGeneratedReport
  default void checkRequirements(LoomEnvironment env) {}

  void validateConstraint(
      LoomEnvironment env, LoomGraph graph, ValidationIssueCollector issueCollector);
}
