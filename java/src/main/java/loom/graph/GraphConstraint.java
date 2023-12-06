package loom.graph;

import loom.validation.ValidationIssueCollector;

@FunctionalInterface
public interface GraphConstraint {
  void validate(LoomGraphEnv env, LoomGraph graph, ValidationIssueCollector issueCollector);
}
