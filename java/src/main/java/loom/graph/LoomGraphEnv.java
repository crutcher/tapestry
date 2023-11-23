package loom.graph;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.Data;
import lombok.Getter;
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
    @Getter private final String type;
    @Getter private final String fieldSchema;

    /**
     * Validate a node against the type.
     *
     * <p>Runs standard validation, and then calls {@link #checkNode(LoomGraphEnv,
     * LoomGraph.NodeDom)}.
     *
     * @param env the environment.
     * @param node the node to validate.
     * @throws loom.validation.LoomValidationError if the node is invalid.
     */
    public final void validateTypedNode(LoomGraphEnv env, LoomGraph.NodeDom node) {
      var issues = new ValidationIssueCollector();

      issues.collect(
          () ->
              env.applySchemaJson(
                  URI.create("urn:loom:node:" + type),
                  JsonUtil.parseToMap(fieldSchema),
                  null,
                  JsonUtil.toJson(node.getFields())));

      issues.collect(() -> checkNode(env, node));

      issues.check();
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
    public void checkNode(LoomGraphEnv env, LoomGraph.NodeDom node) {}
  }

  public static LoomGraphEnv createDefault() {
    var env = new LoomGraphEnv();

    var tensorOps = env.registerNodeTypeOps(new TensorNodeTypeOps());
    tensorOps.addDatatype("int32");

    env.registerNodeTypeOps(new OperationNodeTypeOps());

    return env;
  }

  private final SchemaStore schemaStore;
  private final Map<String, LoomNodeTypeOps> typeOpsMap = new HashMap<>();

  public LoomGraphEnv() {
    this(null);
  }

  public LoomGraphEnv(@Nullable SchemaStore schemaStore) {
    this.schemaStore = schemaStore != null ? schemaStore : new SchemaStore(true);
  }

  public <T extends LoomNodeTypeOps> T registerNodeTypeOps(T ops) {
    typeOpsMap.put(ops.getType(), ops);
    return ops;
  }

  public LoomGraph wrap(LoomDoc doc) {
    return new LoomGraph(doc, this);
  }

  public LoomGraph parse(String json) {
    return wrap(JsonUtil.fromJson(json, LoomDoc.class));
  }

  public LoomGraph parse(JsonNode json) {
    return wrap(JsonUtil.fromJson(json, LoomDoc.class));
  }

  public void validateDom(LoomGraph dom) {
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
        issues.collect(() -> nodeOps.validateTypedNode(this, node));
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
   * @throws loom.validation.LoomValidationError if the JSON document does not match the schema.
   */
  public void applySchemaJson(
      URI schemaUri, Object schemaDoc, @Nullable String jpathPrefix, String json) {
    var issues = new ValidationIssueCollector();

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

    try {
      validator.validateJson(schema, json);
    } catch (ValidationException e) {
      if (!(e instanceof ListValidationException)) {
        throw new RuntimeException(e);
      }

      var errors = ((ListValidationException) e).getErrors();

      String msgContext =
          """
                  - Context: %s
                  - Schema: %s
                  """
              .formatted(JsonUtil.reformatToPrettyJson(json), JsonUtil.toPrettyJson(schemaDoc));

      for (var error : errors) {
        String summary =
            error.getSchema().getUri().getFragment()
                + ": ("
                + error.getObject()
                + ") "
                + error.getMessage();

        issues.add(
            ValidationIssue.builder()
                .type(Constants.JSD_ERROR)
                .param("error", error.getClass().getSimpleName())
                .summary(summary)
                .message(msgContext)
                .context(
                    ValidationIssue.Context.builder().name("Schema").dataToJson(schemaDoc).build())
                .context(
                    ValidationIssue.Context.builder()
                        .name("Instance")
                        .jsonpath(
                            JsonPathUtils.concatJsonPath(
                                jpathPrefix,
                                JsonPathUtils.jsonPointerToJsonPath(
                                    error.getUri().toString().substring(1))))
                        .dataToJson(error.getObject())
                        .build())
                .build());
      }

      issues.check();
    }
  }
}
