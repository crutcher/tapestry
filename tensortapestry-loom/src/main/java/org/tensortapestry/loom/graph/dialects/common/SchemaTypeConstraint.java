package org.tensortapestry.loom.graph.dialects.common;

import java.net.URI;
import java.util.List;
import org.tensortapestry.common.json.JsonPathUtils;
import org.tensortapestry.common.json.JsonUtil;
import org.tensortapestry.common.validation.ValidationIssue;
import org.tensortapestry.common.validation.ValidationIssueCollector;
import org.tensortapestry.loom.graph.LoomEnvironment;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.LoomNode;
import org.tensortapestry.loom.json.JsonSchemaFactoryManager;

public class SchemaTypeConstraint implements LoomEnvironment.Constraint {

  @Override
  public void validateConstraint(
    LoomEnvironment env,
    LoomGraph graph,
    ValidationIssueCollector issueCollector
  ) {
    var manager = env.getJsonSchemaFactoryManager();
    graph.getNodes().values().forEach(node -> checkNode(manager, node, issueCollector));
  }

  private void checkNode(
    JsonSchemaFactoryManager manager,
    LoomNode node,
    ValidationIssueCollector issueCollector
  ) {
    var nodeType = node.getType();
    var nodeSchema = manager.loadSchema(URI.create(nodeType));
    manager
      .issueScan()
      .schema(nodeSchema)
      .issueCollector(issueCollector)
      .param("nodeType", nodeType)
      .summaryPrefix("Node Body ")
      .jsonPathPrefix(JsonPathUtils.concatJsonPath(node.getJsonPath() + ".body"))
      .data(node.viewBodyAsJsonNode())
      .contexts(() ->
        List.of(
          node.asValidationContext("Node"),
          ValidationIssue.Context
            .builder()
            .name("Node Body Schema")
            .data(nodeSchema.getSchemaNode())
            .build()
        )
      )
      .build()
      .scan();

    for (var entry : node.getTags().entrySet()) {
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
