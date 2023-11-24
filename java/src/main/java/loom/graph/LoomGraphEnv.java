package loom.graph;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.*;
import javax.annotation.Nullable;
import lombok.Data;
import loom.common.json.JsonPathUtils;
import loom.common.serialization.JsonUtil;
import loom.graph.nodes.OperationNodeTypeOps;
import loom.graph.nodes.TensorNodeTypeOps;
import loom.validation.Constants;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;
import net.jimblackler.jsonschemafriend.*;

/**
 * LoomGraph environment.
 *
 * <p>Provides node type operations and schema validation.
 */
public class LoomGraphEnv {

  public static final String UNKNOWN_NODE_TYPE = "UnknownNodeType";

  /**
   * Node type operations.
   *
   * <p>Defines the type and field schema for a node type.
   */
  @Data
  public abstract static class LoomNodeTypeOps {
    private final String type;
    private final String fieldSchema;

    /**
     * Check a node against the JSD schema for the type.
     *
     * @param env the environment.
     * @param node the node to check.
     * @throws loom.validation.LoomValidationError if the node is invalid.
     */
    public final void checkNodeSchema(LoomGraphEnv env, LoomGraph.NodeDom node) {
      env.applySchemaJson(
          URI.create("urn:loom:node:" + type),
          JsonUtil.parseToMap(fieldSchema),
          node.jpath(),
          JsonUtil.toJson(node.getFields()),
          List.of(
              ValidationIssue.Context.builder()
                  .name("Node")
                  .jsonpath(node.jpath())
                  .dataFromTree(node.getDoc())
                  .build(),
              ValidationIssue.Context.builder()
                  .name("Field Schema")
                  .jsonData(fieldSchema)
                  .build()));
    }

    /**
     * Check a node.
     *
     * <p>Subclasses can override this method to perform additional checks.
     *
     * @param env the environment.
     * @param node the node to check.
     * @throws loom.validation.LoomValidationError if the node is invalid.
     */
    public void checkNodeSemantics(LoomGraphEnv env, LoomGraph.NodeDom node) {}
  }

  /**
   * Create a default environment.
   *
   * @return the environment.
   */
  public static LoomGraphEnv createDefault() {
    var env = new LoomGraphEnv();

    var tensorOps = env.registerNodeTypeOps(new TensorNodeTypeOps());
    tensorOps.addDatatype("int32");

    env.registerNodeTypeOps(new OperationNodeTypeOps());

    return env;
  }

  private final SchemaStore schemaStore;
  private final Map<String, LoomNodeTypeOps> typeOpsMap = new HashMap<>();

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
   * @param schemaStore the schema store; if null, an empty schema store will be created.
   */
  public LoomGraphEnv(@Nullable SchemaStore schemaStore) {
    this.schemaStore = schemaStore != null ? schemaStore : new SchemaStore(true);
  }

  /**
   * Register a node type operations.
   *
   * @param ops the node type operations.
   * @return the node type operations.
   * @param <T> the type of the node type operations.
   */
  public <T extends LoomNodeTypeOps> T registerNodeTypeOps(T ops) {
    typeOpsMap.put(ops.getType(), ops);
    return ops;
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
  public LoomNodeTypeOps getNodeTypeOps(String type) {
    return typeOpsMap.get(type);
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
   * @param dom the graph.
   * @throws loom.validation.LoomValidationError if the graph is invalid.
   */
  public void validateGraph(LoomGraph dom) {
    // A - Validate the whole graph against JSD.
    var env = dom.getEnv();

    var issues = new ValidationIssueCollector();

    for (var node : dom.nodes()) {
      var nodeType = node.getType();

      var nodeOps = env.typeOpsMap.get(nodeType);
      if (nodeOps == null) {
        issues.add(
            ValidationIssue.builder()
                .type(UNKNOWN_NODE_TYPE)
                .summary("Unknown node type: " + nodeType)
                .build());
      } else {
        issues.collect(() -> nodeOps.checkNodeSchema(this, node));
      }
    }

    if (issues.isEmpty()) {
      for (var node : dom.nodes()) {
        var nodeType = node.getType();
        var nodeOps = env.typeOpsMap.get(nodeType);
        issues.collect(() -> nodeOps.checkNodeSemantics(this, node));
      }
    }

    issues.check();
  }

  /**
   * Apply a JSON schema to a JSON document.
   *
   * @param schemaUri the schema URI.
   * @param schemaDoc the schema document.
   * @param jpathPrefix the JSON path prefix to apply to the context.
   * @param json the JSON document.
   * @param contexts additional Issue contexts.
   * @throws loom.validation.LoomValidationError if the JSON document does not match the schema.
   */
  public void applySchemaJson(
      URI schemaUri,
      Object schemaDoc,
      @Nullable String jpathPrefix,
      String json,
      @Nullable Collection<ValidationIssue.Context> contexts) {
    var validator = new Validator();

    try {
      schemaStore.store(schemaUri, schemaDoc);
    } catch (IllegalStateException e) {
      // Ignore; already loaded.
    }

    Schema schema;
    try {
      schema = schemaStore.loadSchema(schemaUri, validator);
    } catch (SchemaException e) {
      throw new RuntimeException(e);
    }

    Collection<ValidationError> errors;

    try {
      validator.validateJson(schema, json);
      return;
    } catch (ValidationException e) {
      if (!(e instanceof ListValidationException)) {
        throw new RuntimeException(e);
      }

      errors = ((ListValidationException) e).getErrors();
    }

    var issues = new ValidationIssueCollector();

    for (var error : errors) {
      String summary =
          error.getSchema().getUri().getFragment()
              + ": ("
              + error.getObject()
              + ") "
              + error.getMessage();

      var contextList = new ArrayList<ValidationIssue.Context>();
      contextList.add(
          ValidationIssue.Context.builder()
              .name("Instance")
              .jsonpath(
                  JsonPathUtils.concatJsonPath(
                      jpathPrefix,
                      JsonPathUtils.jsonPointerToJsonPath(error.getUri().toString().substring(1))))
              .dataFromTree(error.getObject())
              .build());

      if (contexts != null) {
        contextList.addAll(contexts);
      }

      issues.add(
          ValidationIssue.builder()
              .type(Constants.JSD_ERROR)
              .param("error", error.getClass().getSimpleName())
              .summary(summary)
              .contexts(contextList)
              .build());
    }

    issues.check();
  }
}
