package loom.graph.constraints;

import lombok.Builder;
import lombok.Getter;
import loom.common.json.JsonPathUtils;
import loom.common.json.WithSchema;
import loom.graph.LoomConstants;
import loom.graph.LoomConstraint;
import loom.graph.LoomEnvironment;
import loom.graph.LoomGraph;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;
import org.leadpony.justify.api.JsonSchema;

import javax.annotation.Nonnull;
import java.util.regex.Pattern;

@Builder
@Getter
public class NodeBodySchemaConstraint implements LoomConstraint {
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
            "Class %s does not have a @WithSchema annotation".formatted(clazz));
      }
      return bodySchema(ws.value());
    }
  }

  @Nonnull private final String nodeType;
  @Builder.Default private final boolean isRegex = false;
  @Nonnull private final String bodySchema;

  @Override
  public void checkRequirements(LoomEnvironment env) {
    env.assertSupportsNodeType(nodeType);
  }

  @Override
  public void validateConstraint(
      @SuppressWarnings("unused") LoomEnvironment env,
      LoomGraph graph,
      ValidationIssueCollector issueCollector) {
    var schema = env.getJsonSchemaManager().loadSchema(bodySchema);
    if (isRegex) {
      var typePattern = Pattern.compile(nodeType);
      for (var node : graph.iterableNodes()) {
        if (typePattern.matcher(node.getType()).matches()) {
          checkNode(env, node, schema, issueCollector);
        }
      }
    } else {
      for (var node : graph.iterableNodes(nodeType, LoomGraph.Node.class)) {
        checkNode(env, node, schema, issueCollector);
      }
    }
  }

  private void checkNode(
      LoomEnvironment env,
      LoomGraph.Node<?, ?> node,
      JsonSchema schema,
      ValidationIssueCollector issueCollector) {

    env.getJsonSchemaManager()
        .issueScan()
        .issueCollector(issueCollector)
        .type(LoomConstants.Errors.NODE_SCHEMA_ERROR)
        .param("nodeType", node.getType())
        .summaryPrefix("Body ")
        .jsonPathPrefix(JsonPathUtils.concatJsonPath(node.getJsonPath() + ".body"))
        .schema(schema)
        .json(node.getBodyAsJson())
        .context(node.asContext("Node"))
        .context(
            ValidationIssue.Context.builder().name("Body Schema").dataFromJson(bodySchema).build())
        .build()
        .scan();
  }
}
