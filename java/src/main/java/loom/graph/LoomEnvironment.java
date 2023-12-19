package loom.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import loom.common.json.JsonSchemaManager;
import loom.common.json.JsonUtil;
import loom.validation.ValidationIssueCollector;

/**
 * Loom Graph Environment.
 *
 * <p>Describes the parsing and validation environment for a graph.
 */
@Data
@Builder
public final class LoomEnvironment {
  @FunctionalInterface
  public interface Constraint {
    void check(LoomEnvironment env, LoomGraph graph, ValidationIssueCollector issueCollector);
  }

  @Nonnull private final LoomGraph.NodeMetaFactory nodeMetaFactory;

  @Builder.Default private final JsonSchemaManager jsonSchemaManager = new JsonSchemaManager();

  @Singular private final List<Constraint> constraints = new ArrayList<>();

  /**
   * Add a constraint to the LoomEnvironment.
   *
   * @param constraint the constraint to add.
   * @return the modified LoomEnvironment with the added constraint.
   */
  public LoomEnvironment addConstraint(Constraint constraint) {
    constraints.add(constraint);
    return this;
  }

  /**
   * Load a graph from a JSON string in this environment.
   *
   * @param json the JSON string.
   * @return the graph.
   */
  public LoomGraph graphFromJson(String json) {
    var tree = JsonUtil.parseToJsonNodeTree(json);

    var graph = LoomGraph.builder().env(this).build();

    for (var entry : tree.properties()) {
      var key = entry.getKey();
      if (key.equals("id")) {
        graph.setId(UUID.fromString(entry.getValue().asText()));

      } else if (key.equals("nodes")) {
        for (var nodeTree : entry.getValue()) {
          var node = getNodeMetaFactory().nodeFromTree(nodeTree);
          graph.addNode(node);
        }
      } else {
        throw new IllegalArgumentException("Unknown property: " + key);
      }
    }

    return graph;
  }

  /**
   * Validate a graph in this environment.
   *
   * @param graph the graph to validate.
   * @throws loom.validation.LoomValidationError if the graph is invalid.
   */
  public void validateGraph(LoomGraph graph) {
    ValidationIssueCollector issueCollector = new ValidationIssueCollector();
    validateGraph(graph, issueCollector);
    issueCollector.check();
  }

  /**
   * Validate a graph in this environment.
   *
   * @param graph the graph to validate.
   * @param issueCollector the ValidationIssueCollector.
   */
  public void validateGraph(LoomGraph graph, ValidationIssueCollector issueCollector) {
    for (var node : graph.getNodes().values()) {
      var prototype = nodeMetaFactory.getMetaForType(node.getType());
      prototype.validate(node, issueCollector);
    }

    for (var constraint : constraints) {
      constraint.check(this, graph, issueCollector);
    }
  }

  /**
   * Create a new graph with this environment.
   *
   * @return the graph.
   */
  public LoomGraph createGraph() {
    return graphBuilder().id(UUID.randomUUID()).env(this).build();
  }

  /**
   * Create a new graph builder with this environment.
   *
   * @return the builder.
   */
  public LoomGraph.LoomGraphBuilder graphBuilder() {
    return LoomGraph.builder().env(this);
  }
}
