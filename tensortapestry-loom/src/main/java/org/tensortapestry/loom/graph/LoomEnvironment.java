package org.tensortapestry.loom.graph;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.*;
import javax.annotation.Nonnull;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import org.tensortapestry.common.json.JsonUtil;
import org.tensortapestry.common.runtime.ExcludeFromJacocoGeneratedReport;
import org.tensortapestry.common.validation.ListValidationIssueCollector;
import org.tensortapestry.common.validation.LoomValidationError;
import org.tensortapestry.common.validation.ValidationIssueCollector;
import org.tensortapestry.loom.json.JsonSchemaFactoryManager;

/**
 * Loom Graph Environment.
 *
 * <p>Describes the parsing and validation environment for a graph.
 */
@Data
@Builder(toBuilder = true)
public final class LoomEnvironment {

  /**
   * Constraint interface for graph validating plugins.
   */
  @FunctionalInterface
  public interface Constraint {
    /**
     * Check that the environment supports the requirements of this constraint.
     *
     * @param env the LoomEnvironment.
     * @throws IllegalStateException if the environment does not support the requirements of this
     */
    @ExcludeFromJacocoGeneratedReport
    default void checkRequirements(LoomEnvironment env) {
    }

    void validateConstraint(
      LoomEnvironment env,
      LoomGraph graph,
      ValidationIssueCollector issueCollector
    );
  }

  public interface TypeSupportProvider extends Constraint {
    boolean supportsNodeType(String type);

    boolean supportsTagType(String type);
  }


  @Nonnull
  private final TypeSupportProvider typeSupportProvider;

  @Singular
  private final List<Constraint> constraints;

  @Builder.Default
  private final JsonSchemaFactoryManager jsonSchemaFactoryManager = new JsonSchemaFactoryManager();

  @Nonnull
  @Singular
  private final Map<String, String> urlAliases;

  public String urlAlias(String type) {
    if (type.contains("#")) {
      var parts = type.split("#", 2);
      var url = parts[0];
      var path = parts[1];

      var alias = urlAliases.get(url);
      if (alias != null) {
        return alias + ":" + path.substring(path.lastIndexOf('/') + 1);
      }
    }
    return type;
  }

  /**
   * Get the type alias for a given node type.
   *
   * @param type the node type.
   * @return the type alias.
   */
  public String getTypeAlias(String type) {
    // TODO: something real.
    assertSupportsNodeType(type);
    return urlAlias(type);
  }

  /**
   * Does this environment support the given node type?
   *
   * @param type the node type.
   * @return true if the node type is supported.
   */
  @SuppressWarnings({"CheckReturnValue", "ResultOfMethodCallIgnored"})
  public boolean supportsNodeType(String type) {
    return typeSupportProvider.supportsNodeType(type);
  }

  /**
   * Does this environment support the given tag type?
   *
   * @param type the tag type.
   * @return true if the tag type is supported.
   */
  @SuppressWarnings({"CheckReturnValue", "ResultOfMethodCallIgnored"})
  public boolean supportsTagType(String type) {
    return typeSupportProvider.supportsTagType(type);
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

  /**
   * Lookup a constraint in this environment by class.
   *
   * @param constraintClass the constraint class.
   * @return the constraint, or null if not found.
   */
  public Constraint lookupConstraint(Class<? extends Constraint> constraintClass) {
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
  public Constraint assertConstraint(@Nonnull Class<? extends Constraint> constraintClass) {
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
    var graph = JsonUtil.fromJson(json, LoomGraph.class);
    graph.setEnv(this);
    return graph;
  }

  /**
   * Validate a graph in this environment.
   *
   * @param graph the graph to validate.
   * @throws LoomValidationError if the graph is invalid.
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
    typeSupportProvider.validateConstraint(this, graph, issueCollector);
    for (var constraint : constraints) {
      constraint.validateConstraint(this, graph, issueCollector);
    }
  }

  /**
   * Create a new graph with this environment.
   *
   * @return the graph.
   */
  public LoomGraph newGraph() {
    return new LoomGraph(this);
  }
}
