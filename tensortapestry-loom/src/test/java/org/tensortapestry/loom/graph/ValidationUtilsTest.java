package org.tensortapestry.loom.graph;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.tensortapestry.common.lazy.LazyString;
import org.tensortapestry.common.testing.CommonAssertions;
import org.tensortapestry.common.validation.ListValidationIssueCollector;
import org.tensortapestry.common.validation.ValidationIssue;
import org.tensortapestry.loom.graph.dialects.common.NoteNode;
import org.tensortapestry.loom.graph.dialects.tensorops.TensorNode;

public class ValidationUtilsTest implements CommonAssertions {

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
      .nodeBuilder(NoteNode.TYPE)
      .body(NoteNode.Body.builder().message("Hello").build())
      .build();

    var collector = new ListValidationIssueCollector();
    LoomNode result = ValidationUtils.validateNodeReference(
      graph,
      node.getId(),
      NoteNode.TYPE,
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
      NoteNode.TYPE,
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

    var node = NoteNode.builder(graph).body(b -> b.message("Hello")).build();

    var collector = new ListValidationIssueCollector();
    LoomNode result = ValidationUtils.validateNodeReference(
      graph,
      node.getId(),
      TensorNode.TYPE,
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
}
