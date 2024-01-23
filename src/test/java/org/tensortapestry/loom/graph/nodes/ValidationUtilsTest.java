package org.tensortapestry.loom.graph.nodes;

import java.util.UUID;
import org.junit.Test;
import org.tensortapestry.loom.common.lazy.LazyString;
import org.tensortapestry.loom.graph.LoomConstants;
import org.tensortapestry.loom.graph.LoomEnvironment;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.testing.BaseTestClass;
import org.tensortapestry.loom.validation.ListValidationIssueCollector;
import org.tensortapestry.loom.validation.ValidationIssue;

public class ValidationUtilsTest extends BaseTestClass {

  public LoomEnvironment createEnv() {
    return LoomEnvironment
      .builder()
      .build()
      .addNodeTypeClass(NoteNode.TYPE, NoteNode.class)
      .addNodeTypeClass(TensorNode.TYPE, TensorNode.class);
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
      NoteNode.TYPE,
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
      NoteNode.TYPE,
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
          .type(LoomConstants.NODE_REFERENCE_ERROR)
          .param("nodeId", id)
          .param("nodeType", NoteNode.TYPE)
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
      TensorNode.TYPE,
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
          .type(LoomConstants.NODE_REFERENCE_ERROR)
          .param("nodeId", node.getId())
          .param("expectedType", TensorNode.TYPE)
          .param("actualType", NoteNode.TYPE)
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
      NoteNode.TYPE,
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
          .type(LoomConstants.NODE_REFERENCE_ERROR)
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
