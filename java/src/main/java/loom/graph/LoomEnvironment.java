package loom.graph;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Data;
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
  @Builder.Default private final JsonSchemaManager jsonSchemaManager = new JsonSchemaManager();

  @Nullable private Class<? extends LoomGraph.Node<?, ?>> defaultNodeTypeClass;
  private final Map<String, Class<? extends LoomGraph.Node<?, ?>>> nodeTypeClasses =
      new HashMap<>();
  private final List<LoomConstraint> constraints = new ArrayList<>();

  /**
   * Add a node type class to the LoomEnvironment.
   *
   * @param type the type of the node.
   * @param nodeTypeClass the class representing the node type.
   * @return this LoomEnvironment.
   */
  public LoomEnvironment addNodeTypeClass(
      String type, Class<? extends LoomGraph.Node<?, ?>> nodeTypeClass) {
    this.nodeTypeClasses.put(type, nodeTypeClass);
    return this;
  }

  /**
   * Does this environment support the given node type?
   *
   * @param type the node type.
   * @return true if the node type is supported.
   */
  public boolean supportsNodeType(String type) {
    return defaultNodeTypeClass != null || nodeTypeClasses.containsKey(type);
  }

  /**
   * Assert that this environment supports the given node type.
   *
   * @param type the node type.
   * @throws IllegalStateException if the node type is not supported.
   */
  public void assertSupportsNodeType(String type) {
    if (!supportsNodeType(type)) {
      throw new IllegalStateException("Unsupported node type: " + type);
    }
  }

  @Nullable public Class<? extends LoomGraph.Node<?, ?>> classForType(String type) {
    var nodeTypeClass = nodeTypeClasses.get(type);
    if (nodeTypeClass == null) {
      nodeTypeClass = defaultNodeTypeClass;
    }
    return nodeTypeClass;
  }

  /**
   * Assert that a node type is present in this environment.
   *
   * @param type the node type.
   * @return the node type class.
   * @throws IllegalStateException if the node type is not present.
   */
  @Nonnull
  public Class<? extends LoomGraph.Node<?, ?>> assertClassForType(String type) {
    var nodeTypeClass = classForType(type);
    if (nodeTypeClass == null) {
      throw new IllegalStateException("Unknown node type: " + type);
    }
    return nodeTypeClass;
  }

  /**
   * Assert that a node type class is present in this environment.
   *
   * @param type the node type.
   * @param nodeTypeClass the node type class.
   * @throws IllegalStateException if the node type class is not present.
   */
  public void assertClassForType(String type, Class<? extends LoomGraph.Node<?, ?>> nodeTypeClass) {
    if (nodeTypeClass != assertClassForType(type)) {
      throw new IllegalStateException("Node type class mismatch: " + type + " is " + nodeTypeClass);
    }
  }

  /**
   * Add a constraint to the LoomEnvironment.
   *
   * @param constraint the constraint to add.
   * @return the modified LoomEnvironment with the added constraint.
   * @throws IllegalArgumentException if the constraint is not valid in this environment.
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
          graph.addNode(nodeTree);
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
    return graphBuilder().id(UUID.randomUUID()).build();
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
