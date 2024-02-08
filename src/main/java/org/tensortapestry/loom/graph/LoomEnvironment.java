package org.tensortapestry.loom.graph;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.util.*;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Data;
import org.tensortapestry.common.json.JsonSchemaFactoryManager;
import org.tensortapestry.common.json.JsonUtil;
import org.tensortapestry.common.runtime.ExcludeFromJacocoGeneratedReport;
import org.tensortapestry.common.validation.ListValidationIssueCollector;
import org.tensortapestry.common.validation.LoomValidationError;
import org.tensortapestry.common.validation.ValidationIssueCollector;

/**
 * Loom Graph Environment.
 *
 * <p>Describes the parsing and validation environment for a graph.
 */
@Data
@Builder
public final class LoomEnvironment {

  /** Constraint interface for graph validating plugins. */
  @FunctionalInterface
  public interface Constraint {
    /**
     * Check that the environment supports the requirements of this constraint.
     *
     * @param env the LoomEnvironment.
     * @throws IllegalStateException if the environment does not support the requirements of this
     */
    @ExcludeFromJacocoGeneratedReport
    default void checkRequirements(LoomEnvironment env) {}

    void validateConstraint(
      LoomEnvironment env,
      LoomGraph graph,
      ValidationIssueCollector issueCollector
    );
  }

  @Target({ ElementType.TYPE })
  @Retention(RetentionPolicy.RUNTIME)
  public @interface WithConstraints {
    Class<? extends Constraint>[] value();
  }

  private final List<Constraint> constraints = new ArrayList<>();

  @Builder.Default
  private final JsonSchemaFactoryManager jsonSchemaFactoryManager = new JsonSchemaFactoryManager();

  @ExcludeFromJacocoGeneratedReport
  private static Constraint createConstraint(Class<? extends Constraint> constraintClass) {
    try {
      return constraintClass.getDeclaredConstructor().newInstance();
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  @Builder.Default
  private Map<String, String> urlAliasMap = new HashMap<>();

  @CanIgnoreReturnValue
  public LoomEnvironment addUrlAlias(String url, String alias) {
    urlAliasMap.put(url, alias);
    return this;
  }

  public String urlAlias(String type) {
    if (type.contains("#")) {
      var parts = type.split("#", 2);
      var url = parts[0];
      var path = parts[1];

      var alias = urlAliasMap.get(url);
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
  @SuppressWarnings({ "CheckReturnValue", "ResultOfMethodCallIgnored" })
  public boolean supportsNodeType(String type) {
    try {
      getJsonSchemaFactoryManager().loadSchema(URI.create(type));
      return true;
    } catch (LoomValidationError e) {
      return false;
    }
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
   * Add a constraint to the LoomEnvironment.
   *
   * @param constraint the constraint to add.
   * @return the modified LoomEnvironment with the added constraint.
   * @throws IllegalArgumentException if the constraint is not valid in this environment.
   */
  @CanIgnoreReturnValue
  public LoomEnvironment addConstraint(Constraint constraint) {
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
