package org.tensortapestry.loom.graph;

import java.util.UUID;
import org.junit.Test;
import org.tensortapestry.loom.common.lazy.LazyString;
import org.tensortapestry.loom.graph.dialects.common.NoteNode;
import org.tensortapestry.loom.graph.dialects.tensorops.TensorNode;
import org.tensortapestry.loom.graph.dialects.tensorops.TensorOpNodes;
import org.tensortapestry.loom.testing.BaseTestClass;
import org.tensortapestry.loom.validation.ListValidationIssueCollector;
import org.tensortapestry.loom.validation.ValidationIssue;

public class ValidationUtilsTest extends BaseTestClass {

  public LoomEnvironment createEnv() {
    return LoomEnvironment
      .builder()
      .build()
      .addNodeTypeClass(NoteNode.NOTE_NODE_TYPE, NoteNode.class)
      .addNodeTypeClass(TensorOpNodes.TENSOR_NODE_TYPE, TensorNode.class);
  }

  public LoomGraph createGraph() {
    return createEnv().newGraph();
  }

  @Test
  public void test_validateNodeReference_valid() {
    var graph = createGraph();

    var node = NoteNode.withBody(b -> b.message("Hello")).addTo(graph);

    var collector = new ListValidationIssueCollector();
    NoteNode result = ValidationUtils.validateNodeReference(
      graph,
      node.getId(),
      NoteNode.NOTE_NODE_TYPE,
      NoteNode.class,
      LazyString.fixed("message"),
      collector,
      () -> null
    );

    assertThat(result).isInstanceOf(NoteNode.class).isSameAs(node);

    collector.check();
  }

  @Test
  public void test_validateNodeReference_missing() {
    var graph = createGraph();

    var id = UUID.randomUUID();

    var collector = new ListValidationIssueCollector();
    NoteNode result = ValidationUtils.validateNodeReference(
      graph,
      id,
      NoteNode.NOTE_NODE_TYPE,
      NoteNode.class,
      LazyString.fixed("message"),
      collector,
      () -> null
    );

    assertThat(result).isNull();
    assertThat(collector.getIssues())
      .containsExactly(
        ValidationIssue
          .builder()
          .type(LoomConstants.Errors.NODE_REFERENCE_ERROR)
          .param("nodeId", id)
          .param("nodeType", NoteNode.NOTE_NODE_TYPE)
          .summary("Referenced node does not exist")
          .context(
            ValidationIssue.Context
              .builder()
              .name("Reference")
              .jsonpath(LazyString.fixed("message"))
              .data(id)
              .build()
          )
          .build()
      );
  }

  @Test
  public void test_validateNodeReference_wrongType() {
    var graph = createGraph();

    var node = NoteNode.withBody(b -> b.message("Hello")).addTo(graph);

    var collector = new ListValidationIssueCollector();
    NoteNode result = ValidationUtils.validateNodeReference(
      graph,
      node.getId(),
      TensorOpNodes.TENSOR_NODE_TYPE,
      NoteNode.class,
      LazyString.fixed("message"),
      collector,
      () -> null
    );

    assertThat(result).isNull();
    assertThat(collector.getIssues())
      .containsExactly(
        ValidationIssue
          .builder()
          .type(LoomConstants.Errors.NODE_REFERENCE_ERROR)
          .param("nodeId", node.getId())
          .param("expectedType", TensorOpNodes.TENSOR_NODE_TYPE)
          .param("actualType", NoteNode.NOTE_NODE_TYPE)
          .summary("Referenced node has the wrong type")
          .context(
            ValidationIssue.Context
              .builder()
              .name("Reference")
              .jsonpath(LazyString.fixed("message"))
              .data(node.getId())
              .build()
          )
          .build()
      );
  }

  @Test
  public void test_validateNodeReference_wrongClass() {
    var graph = createGraph();

    var node = NoteNode.withBody(b -> b.message("Hello")).addTo(graph);

    var collector = new ListValidationIssueCollector();
    TensorNode result = ValidationUtils.validateNodeReference(
      graph,
      node.getId(),
      NoteNode.NOTE_NODE_TYPE,
      TensorNode.class,
      LazyString.fixed("message"),
      collector,
      () -> null
    );

    assertThat(result).isNull();
    assertThat(collector.getIssues())
      .containsExactly(
        ValidationIssue
          .builder()
          .type(LoomConstants.Errors.NODE_REFERENCE_ERROR)
          .param("expectedClass", TensorNode.class.getSimpleName())
          .param("actualClass", NoteNode.class.getSimpleName())
          .summary("Referenced node has the wrong class")
          .context(
            ValidationIssue.Context
              .builder()
              .name("Reference")
              .jsonpath(LazyString.fixed("message"))
              .data(node.getId())
              .build()
          )
          .build()
      );
  }
}
