package loom.graph;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;
import loom.common.LookupError;
import loom.common.serialization.JsonUtil;
import loom.graph.nodes.NodeTypeBindings;
import loom.validation.LoomValidationError;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;

/**
 * LoomGraph environment.
 *
 * <p>Provides node type operations and schema validation.
 */
public class LoomGraphEnv {

  public static final String UNKNOWN_NODE_TYPE = "UnknownNodeType";

  @Nonnull @Getter private final JsdManager jsdManager;

  private final Map<String, NodeTypeBindings> typeOpsMap = new HashMap<>();

  private final List<GraphConstraint> constraints = new ArrayList<>();

  /**
   * Create a new environment.
   *
   * <p>The environment will have an empty schema store.
   */
  public LoomGraphEnv() {
    this(null);
  }

  /**
   * Create a new environment.
   *
   * @param jsdManager the JSD manager.
   */
  public LoomGraphEnv(@Nullable JsdManager jsdManager) {
    this.jsdManager = jsdManager != null ? jsdManager : new JsdManager(null);
  }

  /**
   * Register a node type operations.
   *
   * @param ops the node type operations.
   * @return the node type operations.
   * @param <T> the type of the node type operations.
   */
  @CanIgnoreReturnValue
  public <T extends NodeTypeBindings> T addNodeTypeBindings(T ops) {
    typeOpsMap.put(ops.getType(), ops);
    return ops;
  }

  /**
   * Add a graph constraint.
   *
   * @param constraint the constraint.
   * @return the constraint.`
   * @param <T> the type of the constraint.
   */
  @CanIgnoreReturnValue
  public <T extends GraphConstraint> T addConstraint(T constraint) {
    constraints.add(constraint);
    return constraint;
  }

  /**
   * Check if the environment has node type operations for a type.
   *
   * @param type the type.
   * @return true if the environment has node type operations for the type.
   */
  public boolean hasNodeTypeOps(String type) {
    return typeOpsMap.containsKey(type);
  }

  /**
   * Get the node type operations for a type.
   *
   * @param type the type.
   * @return the node type operations, or null if the environment does not have node type
   *     operations.
   */
  public NodeTypeBindings getNodeTypeOps(String type) {
    return typeOpsMap.get(type);
  }

  /**
   * Get the node type operations for a type.
   *
   * @param type the type.
   * @return the node type operations.
   * @throws LookupError if the environment does not have node type operations.
   */
  public NodeTypeBindings assertNodeTypeOps(String type) {
    var ops = getNodeTypeOps(type);
    if (ops == null) {
      throw new LookupError("Unknown node type: " + type);
    }
    return ops;
  }

  /**
   * Get the node type operations for a type.
   *
   * <p>This method is a type-safe version of {@link #getNodeTypeOps(String)}; the clazz parameter
   * is used to cast the result.
   *
   * @param type the type.
   * @param clazz the type of the node type operations.
   * @return the node type operations.
   * @param <T> the type of the node type operations.
   * @throws LookupError if the environment does not have node type operations.
   */
  public <T extends NodeTypeBindings> T assertNodeTypeOps(String type, Class<T> clazz) {
    return clazz.cast(assertNodeTypeOps(type));
  }

  /**
   * Create a new graph.
   *
   * @return the graph.
   */
  public LoomGraph createGraph() {
    return new LoomGraph(new LoomDoc(), this);
  }

  /**
   * Wrap a document in a graph.
   *
   * <p>The graph will be linked to this environment.
   *
   * @param doc the document.
   * @return the graph.
   */
  public LoomGraph wrap(LoomDoc doc) {
    // TODO: validate?
    return new LoomGraph(doc, this);
  }

  /**
   * Parse a JSON document into a graph.
   *
   * @param json the JSON document.
   * @return the graph.
   */
  public LoomGraph parse(String json) {
    return wrap(JsonUtil.fromJson(json, LoomDoc.class));
  }

  /**
   * Parse a JSON document into a graph.
   *
   * @param json the JSON document.
   * @return the graph.
   */
  public LoomGraph parse(JsonNode json) {
    return wrap(JsonUtil.convertValue(json, LoomDoc.class));
  }

  /**
   * Validate a graph against an environment.
   *
   * @param graph the graph.
   * @throws LoomValidationError if the graph is invalid.
   */
  public void validateGraph(LoomGraph graph) {
    var issues = new ValidationIssueCollector();

    validateGraph(graph, issues);

    issues.check();
  }

  /**
   * Validate a graph against an environment.
   *
   * <p>Collects issues in the given issue collector.
   *
   * @param graph the graph.
   * @param issueCollector the issue collector.
   */
  void validateGraph(LoomGraph graph, @Nonnull ValidationIssueCollector issueCollector) {
    Objects.requireNonNull(issueCollector);

    for (var node : graph.nodes()) {
      var nodeType = node.getType();
      var nodeOps = typeOpsMap.get(nodeType);

      if (nodeOps == null) {
        issueCollector.add(
            ValidationIssue.builder()
                .type(UNKNOWN_NODE_TYPE)
                .summary("Unknown node type: " + nodeType));
      } else {
        nodeOps.checkNodeSchema(this, node, issueCollector);
      }
    }

    if (issueCollector.isEmpty()) {
      for (var node : graph.nodes()) {
        var nodeType = node.getType();
        var nodeOps = typeOpsMap.get(nodeType);

        nodeOps.checkNodeSemantics(this, node, issueCollector);
      }
    }

    if (issueCollector.isEmpty()) {
      for (var constraint : constraints) {
        issueCollector.collect(() -> constraint.validate(this, graph, issueCollector));
      }
    }
  }
}
