package loom.graph.constraints;

import loom.graph.CommonEnvironments;
import loom.graph.LoomConstants;
import loom.graph.LoomEnvironment;
import loom.graph.nodes.OperationNode;
import loom.graph.nodes.TensorNode;
import loom.testing.BaseTestClass;
import loom.validation.ListValidationIssueCollector;
import loom.validation.ValidationIssue;
import loom.zspace.ZPoint;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class AllTensorsHaveExactlyOneSourceOperationConstraintTest extends BaseTestClass {
  public LoomEnvironment createEnvironment() {
    return CommonEnvironments.expressionEnvironment();
  }

  @Test
  public void test() {
    var env = createEnvironment();
    var graph = env.createGraph();

    var noSources =
        TensorNode.withBody(
                b -> {
                  b.dtype("int32");
                  b.shape(new ZPoint(2, 3));
                })
            .label("NoSources")
            .buildOn(graph);

    var collector = new ListValidationIssueCollector();
    graph.validate(collector);

    assertThat(collector.getIssues())
        .contains(
            ValidationIssue.builder()
                .type(LoomConstants.NODE_VALIDATION_ERROR)
                .param("nodeType", TensorNode.TYPE)
                .context(
                    ValidationIssue.Context.builder()
                        .name("Tensor")
                        .jsonpath(noSources.getJsonPath())
                        .dataFromJson(noSources.toJsonString()))
                .summary("Tensor (NoSources) has no Operation source")
                .build());
  }

  @Test
  public void test_TooManySources() {
    var env = createEnvironment();
    var graph = env.createGraph();

    var tooManySources =
        TensorNode.withBody(
                b -> {
                  b.dtype("int32");
                  b.shape(new ZPoint(2, 3));
                })
            .label("TooManySources")
            .buildOn(graph);

    var opNode1 =
        OperationNode.withBody(
                b1 -> {
                  b1.opName("source");
                  b1.outputs(Map.of("pin", List.of(tooManySources.getId())));
                })
            .label("op1")
            .buildOn(graph);

    var opNode2 =
        OperationNode.withBody(
                b -> {
                  b.opName("source");
                  b.outputs(Map.of("pin", List.of(tooManySources.getId())));
                })
            .label("op2")
            .buildOn(graph);

    var collector = new ListValidationIssueCollector();
    graph.validate(collector);

    assertThat(collector.getIssues())
        .contains(
            ValidationIssue.builder()
                .type(LoomConstants.NODE_VALIDATION_ERROR)
                .param("nodeType", TensorNode.TYPE)
                .summary("Tensor (TooManySources) has too many Operation sources: 2")
                .message("Tensor id: " + tooManySources.getId())
                .context(tooManySources.asContext("Tensor"))
                .context(opNode1.asContext("Source Operation #0 (op1)"))
                .context(opNode2.asContext("Source Operation #1 (op2)"))
                .build());
  }
}
