package loom.graph;

import loom.graph.validation.ValidationIssueCollector;

@FunctionalInterface
public interface GraphConstraint {
  void validate(LoomGraphEnv env, LoomGraph graph, ValidationIssueCollector issueCollector);
}
