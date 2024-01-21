package loom.graph;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Data;
import loom.common.json.JsonSchemaFactoryManager;
import loom.common.json.JsonUtil;
import loom.common.runtime.CheckThat;
import loom.common.runtime.ExcludeFromJacocoGeneratedReport;
import loom.graph.constraints.NodeBodySchemaConstraint;
import loom.validation.ListValidationIssueCollector;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;

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

  @Nullable
  private Class<? extends LoomNode<?, ?>> defaultNodeTypeClass;

  private final Map<String, Class<? extends LoomNode<?, ?>>> nodeTypeClasses = new HashMap<>();

  @Nullable
  private Class<?> defaultAnnotationTypeClass;

  private final Map<String, Class<?>> annotationTypeClasses = new HashMap<>();

  private final List<Constraint> constraints = new ArrayList<>();

  @Builder.Default
  private final JsonSchemaFactoryManager jsonSchemaFactoryManager = new JsonSchemaFactoryManager();

  /**
   * Add a node type class to the LoomEnvironment.
   *
   * @param type the type of the node.
   * @param nodeTypeClass the class representing the node type.
   * @return this LoomEnvironment.
   */
  public LoomEnvironment addNodeTypeClass(
    String type,
    Class<? extends LoomNode<?, ?>> nodeTypeClass
  ) {
    this.nodeTypeClasses.put(type, nodeTypeClass);
    return this;
  }

  /**
   * Register a Node type class, and common constraints.
   *
   * @param type the type.
   * @param nodeTypeClass the node type class.
   * @return this LoomEnvironment.
   */
  public LoomEnvironment autowireNodeTypeClass(
    String type,
    Class<? extends LoomNode<?, ?>> nodeTypeClass
  ) {
    addNodeTypeClass(type, nodeTypeClass);
    addConstraint(
      NodeBodySchemaConstraint
        .builder()
        .nodeType(type)
        .withSchemaFromNodeClass(nodeTypeClass)
        .build()
    );
    for (var withConstraints : nodeTypeClass.getAnnotationsByType(WithConstraints.class)) {
      for (var cls : withConstraints.value()) {
        addConstraint(createConstraint(cls));
      }
    }
    return this;
  }

  @ExcludeFromJacocoGeneratedReport
  private static Constraint createConstraint(Class<? extends Constraint> constraintClass) {
    try {
      return constraintClass.getDeclaredConstructor().newInstance();
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
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
    return "loom:" + type;
  }

  public String getAnnotationTypeAlias(String key) {
    // TODO: something real.
    return "loom:" + key;
  }

  /**
   * Does this environment support the given node type?
   *
   * @param type the node type.
   * @return true if the node type is supported.
   */
  public boolean supportsNodeType(String type) {
    return (defaultNodeTypeClass != null || nodeTypeClasses.containsKey(type));
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
   * Returns the class associated with the given type in this LoomEnvironment.
   *
   * @param type the type of the node.
   * @return the class representing the node type, or null if the type is not found.
   */
  @Nullable
  public Class<? extends LoomNode<?, ?>> classForType(String type) {
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
  public Class<? extends LoomNode<?, ?>> assertClassForType(String type) {
    var nodeTypeClass = classForType(type);
    if (nodeTypeClass == null) {
      throw new IllegalStateException("Unknown node type: " + type);
    }
    return nodeTypeClass;
  }

  /**
   * Add an annotation type class to the LoomEnvironment.
   *
   * @param key the annotation key.
   * @param annotationTypeClass the annotation type class.
   * @return this LoomEnvironment.
   */
  public LoomEnvironment addAnnotationTypeClass(String key, Class<?> annotationTypeClass) {
    this.annotationTypeClasses.put(key, annotationTypeClass);
    return this;
  }

  /**
   * Assert that a node type class is present in this environment.
   *
   * @param type the node type.
   * @param nodeTypeClass the node type class.
   * @throws IllegalStateException if the node type class is not present.
   */
  public void assertClassForType(String type, Class<? extends LoomNode<?, ?>> nodeTypeClass) {
    if (nodeTypeClass != assertClassForType(type)) {
      throw new IllegalStateException("Node type class mismatch: " + type + " is " + nodeTypeClass);
    }
  }

  /**
   * Get the annotation class for a given key.
   *
   * @param key the annotation key.
   * @return the annotation class, or null if not found.
   */
  public Class<?> getAnnotationClass(String key) {
    var cls = annotationTypeClasses.get(key);
    if (cls == null && defaultAnnotationTypeClass != null) {
      cls = defaultAnnotationTypeClass;
    }
    return cls;
  }

  /**
   * Get the annotation class for a given key.
   *
   * @param key the annotation key.
   * @return the annotation class.
   * @throws IllegalStateException if the annotation key is not present.
   */
  @Nonnull
  public Class<?> assertAnnotationClass(String key) {
    return CheckThat.valueIsNotNull(
      getAnnotationClass(key),
      IllegalStateException.class,
      "Unknown annotation key: %s",
      key
    );
  }

  /**
   * Add a constraint to the LoomEnvironment.
   *
   * @param constraint the constraint to add.
   * @return the modified LoomEnvironment with the added constraint.
   * @throws IllegalArgumentException if the constraint is not valid in this environment.
   */
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
  public Constraint assertConstraint(Class<? extends Constraint> constraintClass) {
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
    for (var nodeIt = graph.nodeScan().asStream().iterator(); nodeIt.hasNext();) {
      var node = nodeIt.next();
      var type = node.getType();
      var nodeClass = node.getClass();
      var expectedClass = classForType(type);

      if (expectedClass == null) {
        issueCollector.addIssue(
          ValidationIssue
            .builder()
            .type(LoomConstants.NODE_VALIDATION_ERROR)
            .summary("Unknown node type: " + type)
            .context(node.asValidationContext("Node"))
            .build()
        );
      } else if (!expectedClass.isAssignableFrom(nodeClass)) {
        issueCollector.addIssue(
          ValidationIssue
            .builder()
            .type(LoomConstants.NODE_VALIDATION_ERROR)
            .summary("Node type mismatch: " + type + " is " + nodeClass)
            .context(node.asValidationContext("Node"))
            .build()
        );
      }

      for (var entry : node.getAnnotations().entrySet()) {
        var key = entry.getKey();
        var annotation = entry.getValue();
        var annotationClass = annotation.getClass();

        var expectedAnnotationClass = getAnnotationClass(key);
        if (expectedAnnotationClass == null) {
          issueCollector.addIssue(
            ValidationIssue
              .builder()
              .type(LoomConstants.NODE_VALIDATION_ERROR)
              .summary("Unknown annotation key: " + key)
              .context(node.asValidationContext("Node"))
              .build()
          );
        } else if (!expectedAnnotationClass.isAssignableFrom(annotationClass)) {
          issueCollector.addIssue(
            ValidationIssue
              .builder()
              .type(LoomConstants.NODE_VALIDATION_ERROR)
              .summary("Annotation type mismatch: " + key + " is " + annotationClass)
              .context(node.asValidationContext("Node"))
              .build()
          );
        }
      }
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
  public LoomGraph newGraph() {
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
