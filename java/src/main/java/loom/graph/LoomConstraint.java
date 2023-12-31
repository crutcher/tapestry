package loom.graph;

import loom.common.runtime.ExcludeFromJacocoGeneratedReport;
import loom.validation.ValidationIssueCollector;

@FunctionalInterface
public interface LoomConstraint {
  /**
   * Check that the environment supports the requirements of this constraint.
   *
   * @param env the LoomEnvironment.
   * @throws IllegalStateException if the environment does not support the requirements of this
   */
  @ExcludeFromJacocoGeneratedReport
  default void checkRequirements(LoomEnvironment env) {}

  void validateConstraint(
      LoomEnvironment env, LoomGraph graph, ValidationIssueCollector issueCollector);
}
