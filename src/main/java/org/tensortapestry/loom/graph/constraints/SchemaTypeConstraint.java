package org.tensortapestry.loom.graph.constraints;

import java.net.URI;
import java.util.List;
import org.tensortapestry.loom.common.json.JsonPathUtils;
import org.tensortapestry.loom.common.json.JsonSchemaFactoryManager;
import org.tensortapestry.loom.common.json.JsonUtil;
import org.tensortapestry.loom.graph.LoomEnvironment;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.LoomNode;
import org.tensortapestry.loom.validation.ValidationIssue;
import org.tensortapestry.loom.validation.ValidationIssueCollector;

public class SchemaTypeConstraint implements LoomEnvironment.Constraint {

  @Override
  public void validateConstraint(
    LoomEnvironment env,
    LoomGraph graph,
    ValidationIssueCollector issueCollector
  ) {
    var manager = env.getJsonSchemaFactoryManager();
    graph.nodeScan().asStream().forEach(node -> checkNode(manager, node, issueCollector));
  }

  private void checkNode(
    JsonSchemaFactoryManager manager,
    LoomNode<?, ?> node,
    ValidationIssueCollector issueCollector
  ) {
    var nodeType = node.getType();
    var nodeSchema = manager.loadSchema(URI.create(nodeType));
    manager
      .issueScan()
      .schema(nodeSchema)
      .issueCollector(issueCollector)
      .param("nodeType", nodeType)
      .summaryPrefix("Body ")
      .jsonPathPrefix(JsonPathUtils.concatJsonPath(node.getJsonPath() + ".body"))
      .data(node.getBodyAsJsonNode())
      .contexts(() ->
        List.of(
          node.asValidationContext("Node"),
          ValidationIssue.Context
            .builder()
            .name("Body Schema")
            .data(nodeSchema.getSchemaNode())
            .build()
        )
      )
      .build()
      .scan();

    for (var entry : node.getAnnotations().entrySet()) {
      var annotationType = entry.getKey();
      var annotation = entry.getValue();

      var annSchema = manager.loadSchema(URI.create(annotationType));
      manager
        .issueScan()
        .schema(annSchema)
        .issueCollector(issueCollector)
        .param("nodeType", nodeType)
        .param("annotationType", annotationType)
        .summaryPrefix("Annotation ")
        .jsonPathPrefix(
          JsonPathUtils.concatJsonPath(
            node.getJsonPath() + ".annotations['" + annotationType + "']"
          )
        )
        .data(JsonUtil.valueToJsonNodeTree(annotation))
        .contexts(() ->
          List.of(
            node.asValidationContext("Node"),
            ValidationIssue.Context
              .builder()
              .name("Annotation Schema")
              .data(annSchema.getSchemaNode())
              .build()
          )
        )
        .build()
        .scan();
    }
  }
}
