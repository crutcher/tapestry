package org.tensortapestry.loom.graph;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.experimental.UtilityClass;
import org.tensortapestry.loom.common.lazy.LazyString;
import org.tensortapestry.loom.common.lazy.Thunk;
import org.tensortapestry.loom.validation.ValidationIssue;
import org.tensortapestry.loom.validation.ValidationIssueCollector;

@UtilityClass
public class ValidationUtils {

  public static LoomNode validateNodeReference(
    LoomGraph graph,
    UUID nodeId,
    String nodeType,
    LazyString relativeFieldPath,
    ValidationIssueCollector issueCollector,
    Supplier<List<ValidationIssue.Context>> contextsSupplier
  ) {
    var context = Thunk.of(() ->
      ValidationIssue.Context
        .builder()
        .name("Reference")
        .jsonpath(relativeFieldPath)
        .data(nodeId)
        .build()
    );

    var node = graph.getNode(nodeId);
    if (node == null) {
      issueCollector.addIssue(
        ValidationIssue
          .builder()
          .type(LoomConstants.Errors.NODE_REFERENCE_ERROR)
          .param("nodeId", nodeId)
          .param("nodeType", nodeType)
          .summary("Referenced node does not exist")
          .context(context)
          .withContexts(contextsSupplier)
          .build()
      );
      return null;
    }
    if (!node.getType().equals(nodeType)) {
      issueCollector.addIssue(
        ValidationIssue
          .builder()
          .type(LoomConstants.Errors.NODE_REFERENCE_ERROR)
          .param("nodeId", nodeId)
          .param("expectedType", nodeType)
          .param("actualType", node.getType())
          .summary("Referenced node has the wrong type")
          .context(context)
          .withContexts(contextsSupplier)
          .build()
      );
      return null;
    }
    return node;
  }
}
