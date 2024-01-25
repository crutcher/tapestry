package org.tensortapestry.loom.graph;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.tensortapestry.loom.common.lazy.LazyString;
import org.tensortapestry.loom.common.lazy.Thunk;
import org.tensortapestry.loom.validation.ValidationIssue;
import org.tensortapestry.loom.validation.ValidationIssueCollector;

public class ValidationUtils {

  public static <T> T validateNodeReference(
    LoomGraph graph,
    UUID nodeId,
    String nodeType,
    Class<T> nodeClass,
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
    if (!nodeClass.isInstance(node)) {
      issueCollector.addIssue(
        ValidationIssue
          .builder()
          .type(LoomConstants.Errors.NODE_REFERENCE_ERROR)
          .param("expectedClass", nodeClass.getSimpleName())
          .param("actualClass", node.getClass().getSimpleName())
          .summary("Referenced node has the wrong class")
          .context(context)
          .withContexts(contextsSupplier)
          .build()
      );
      return null;
    }
    return nodeClass.cast(node);
  }
}
