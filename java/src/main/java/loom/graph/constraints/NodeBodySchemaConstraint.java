package loom.graph.constraints;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import loom.common.json.JsonPathUtils;
import loom.common.json.WithSchema;
import loom.graph.LoomConstants;
import loom.graph.LoomEnvironment;
import loom.graph.LoomGraph;
import loom.graph.LoomNode;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;
import org.leadpony.justify.api.JsonSchema;

@Builder
@Getter
public final class NodeBodySchemaConstraint implements LoomEnvironment.Constraint {

  @SuppressWarnings("unused")
  public static class NodeBodySchemaConstraintBuilder {

    /**
     * Set the body schema from a class annotated with {@link WithSchema}.
     *
     * @param clazz the class.
     * @return this builder.
     */
    public NodeBodySchemaConstraintBuilder withSchemaFromBodyClass(Class<?> clazz) {
      var ws = clazz.getAnnotation(WithSchema.class);
      if (ws == null) {
        throw new IllegalArgumentException(
          "Class %s does not have a @WithSchema annotation".formatted(clazz)
        );
      }
      return bodySchema(ws.value());
    }

    /**
     * Set the body schema from a node class with a {@link WithSchema} body class.
     *
     * @param clazz the class.
     * @return this builder.
     */
    public NodeBodySchemaConstraintBuilder withSchemaFromNodeClass(
      Class<? extends LoomNode<?, ?>> clazz
    ) {
      return withSchemaFromBodyClass(LoomNode.getBodyClass(clazz));
    }
  }

  @Singular
  private final List<String> nodeTypes;

  @Singular
  private final List<Pattern> nodeTypePatterns;

  @Nonnull
  private final String bodySchema;

  public NodeBodySchemaConstraint(
    @Nonnull List<String> nodeTypes,
    @Nonnull List<Pattern> nodeTypePatterns,
    @Nonnull String bodySchema
  ) {
    this.nodeTypes = nodeTypes;
    this.nodeTypePatterns = nodeTypePatterns;
    this.bodySchema = bodySchema;
  }

  @Override
  public void checkRequirements(LoomEnvironment env) {
    for (String nodeType : nodeTypes) {
      env.assertSupportsNodeType(nodeType);
    }
    env.getJsonSchemaManager().loadSchema(bodySchema);
  }

  @Override
  public void validateConstraint(
    @SuppressWarnings("unused") LoomEnvironment env,
    LoomGraph graph,
    ValidationIssueCollector issueCollector
  ) {
    var schema = env.getJsonSchemaManager().loadSchema(bodySchema);

    ((Stream<? extends LoomNode<?, ?>>) graph.nodeScan().asStream()).filter(node -> {
        String type = node.getType();
        for (String nodeType : nodeTypes) {
          if (nodeType.equals(type)) {
            return true;
          }
        }
        for (Pattern p : nodeTypePatterns) {
          if (p.matcher(type).matches()) {
            return true;
          }
        }
        return false;
      })
      .forEach(node -> checkNode(env, node, schema, issueCollector));
  }

  private void checkNode(
    LoomEnvironment env,
    LoomNode<?, ?> node,
    JsonSchema schema,
    ValidationIssueCollector issueCollector
  ) {
    env
      .getJsonSchemaManager()
      .issueScan()
      .issueCollector(issueCollector)
      .type(LoomConstants.Errors.NODE_SCHEMA_ERROR)
      .param("nodeType", node.getType())
      .summaryPrefix("Body ")
      .jsonPathPrefix(JsonPathUtils.concatJsonPath(node.getJsonPath() + ".body"))
      .schema(schema)
      .json(node.getBodyAsJson())
      .context(node.asValidationContext("Node"))
      .context(
        ValidationIssue.Context.builder().name("Body Schema").dataFromJson(bodySchema).build()
      )
      .build()
      .scan();
  }
}
