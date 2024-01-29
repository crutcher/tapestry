package org.tensortapestry.loom.graph;

import java.util.UUID;
import org.junit.Test;
import org.tensortapestry.loom.common.lazy.LazyString;
import org.tensortapestry.loom.graph.dialects.common.CommonNodes;
import org.tensortapestry.loom.graph.dialects.common.NoteBody;
import org.tensortapestry.loom.graph.dialects.tensorops.TensorOpNodes;
import org.tensortapestry.loom.testing.BaseTestClass;
import org.tensortapestry.loom.validation.ListValidationIssueCollector;
import org.tensortapestry.loom.validation.ValidationIssue;

public class ValidationUtilsTest extends BaseTestClass {

  public LoomEnvironment createEnv() {
    return CommonEnvironments.expressionEnvironment();
  }

  public LoomGraph createGraph() {
    return createEnv().newGraph();
  }

  @Test
  public void test_validateNodeReference_valid() {
    var graph = createGraph();

    var node = graph
      .nodeBuilder(CommonNodes.NOTE_NODE_TYPE)
      .body(NoteBody.builder().message("Hello").build())
      .build();

    var collector = new ListValidationIssueCollector();
    LoomNode result = ValidationUtils.validateNodeReference(
      graph,
      node.getId(),
      CommonNodes.NOTE_NODE_TYPE,
      LazyString.fixed("message"),
      collector,
      () -> null
    );

    assertThat(result).isSameAs(node);

    collector.check();
  }

  @Test
  public void test_validateNodeReference_missing() {
    var graph = createGraph();

    var id = UUID.randomUUID();

    var collector = new ListValidationIssueCollector();
    LoomNode result = ValidationUtils.validateNodeReference(
      graph,
      id,
      CommonNodes.NOTE_NODE_TYPE,
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
          .param("nodeType", CommonNodes.NOTE_NODE_TYPE)
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

    var node = CommonNodes.noteBuilder(graph, b -> b.message("Hello")).build();

    var collector = new ListValidationIssueCollector();
    LoomNode result = ValidationUtils.validateNodeReference(
      graph,
      node.getId(),
      TensorOpNodes.TENSOR_NODE_TYPE,
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
          .param("actualType", CommonNodes.NOTE_NODE_TYPE)
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
}
