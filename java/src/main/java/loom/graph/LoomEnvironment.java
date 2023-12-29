package loom.graph;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import loom.common.json.JsonSchemaManager;
import loom.common.json.JsonUtil;
import loom.validation.ListValidationIssueCollector;
import loom.validation.ValidationIssueCollector;

/**
 * Loom Graph Environment.
 *
 * <p>Describes the parsing and validation environment for a graph.
 */
@Data
@Builder
public final class LoomEnvironment {

  @Nonnull private final LoomGraph.NodeMetaFactory nodeMetaFactory;

  @Builder.Default private final JsonSchemaManager jsonSchemaManager = new JsonSchemaManager();

  @Singular private final List<LoomConstraint> constraints = new ArrayList<>();

  /**
   * Add a constraint to the LoomEnvironment.
   *
   * @param constraint the constraint to add.
   * @return the modified LoomEnvironment with the added constraint.
   */
  public LoomEnvironment addConstraint(LoomConstraint constraint) {
    constraint.checkRequirements(this);
    constraints.add(constraint);
    return this;
  }

  /**
   * Lookup a constraint in this environment by class.
   *
   * @param constraintClass the constraint class.
   * @return the constraint, or null if not found.
   */
  public LoomConstraint lookupConstraint(Class<? extends LoomConstraint> constraintClass) {
    for (var constraint : constraints) {
      if (constraint.getClass().equals(constraintClass)) {
        return constraint;
      }
    }
    return null;
  }

  /**
   * Assert that a constraint is present in this environment.
   *
   * @param constraintClass the constraint class.
   * @return the constraint.
   * @throws IllegalStateException if the constraint is not present.
   */
  @CanIgnoreReturnValue
  public LoomConstraint assertConstraint(Class<? extends LoomConstraint> constraintClass) {
    var constraint = lookupConstraint(constraintClass);
    if (constraint == null) {
      throw new IllegalStateException("Required constraint not found: " + constraintClass);
    }
    return constraint;
  }

  /**
   * Assert that a node type class is present in this environment.
   *
   * @param type the node type.
   * @param nodeTypeClass the node type class.
   * @throws IllegalStateException if the node type class is not present.
   */
  public void assertNodeTypeClass(
      String type, Class<? extends LoomGraph.Node<?, ?>> nodeTypeClass) {
    var meta = nodeMetaFactory.getMetaForType(type);
    if (!meta.getNodeTypeClass().equals(nodeTypeClass)) {
      throw new IllegalStateException(
          "Node type class mismatch: " + type + " is " + meta.getNodeTypeClass());
    }
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
          var type = nodeTree.get("type").asText();
          var meta = getNodeMetaFactory().getMetaForType(type);
          var node = JsonUtil.convertValue(nodeTree, meta.getNodeTypeClass());
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
    var listCollector = new ListValidationIssueCollector();
    validateGraph(graph, listCollector);
    listCollector.check();
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
      constraint.validateConstraint(this, graph, issueCollector);
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
