package loom.graph.nodes;

import loom.demo.DemoTest;
import loom.graph.CommonEnvironments;
import loom.graph.LoomConstants;
import loom.graph.LoomEnvironment;
import loom.testing.BaseTestClass;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;
import loom.zspace.ZPoint;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class AllTensorsHaveExactlyOneSourceOperationConstraintTest extends BaseTestClass {
  public LoomEnvironment createEnvironment() {
    var env = CommonEnvironments.simpleTensorEnvironment("int32");
    env.getConstraints().add(new AllTensorsHaveExactlyOneSourceOperationConstraint());
    env.getConstraints().add(DemoTest::CycleCheckConstraint);
    return env;
  }

  @Test
  public void test_NoSource() {
    var env = createEnvironment();
    var graph = env.createGraph();

    var tensorA =
        TensorNode.withBody(
                b -> {
                  b.dtype("int32");
                  b.shape(new ZPoint(2, 3));
                })
            .label("TooManySources")
            .buildOn(graph);

    ValidationIssueCollector issueCollector = new ValidationIssueCollector();
    graph.validate(issueCollector);

    assertThat(issueCollector.getIssues())
        .contains(
            ValidationIssue.builder()
                .type(LoomConstants.NODE_VALIDATION_ERROR)
                .param("nodeType", TensorNode.TYPE)
                .context(
                    ValidationIssue.Context.builder()
                        .name("Tensor")
                        .jsonpath(tensorA.getJsonPath())
                        .dataFromJson(tensorA.toJsonString()))
                .summary("Tensor (TooManySources) has no Operation source")
                .build());
  }

  @Test
  public void test_TooManySources() {
    var env = createEnvironment();
    var graph = env.createGraph();

    var tensorNode =
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
                  b1.outputs(Map.of("pin", List.of(tensorNode.getId())));
                })
            .label("op1")
            .buildOn(graph);

    var opNode2 =
        OperationNode.withBody(
                b -> {
                  b.opName("source");
                  b.outputs(Map.of("pin", List.of(tensorNode.getId())));
                })
            .label("op2")
            .buildOn(graph);

    var issueCollector = new ValidationIssueCollector();
    graph.validate(issueCollector);
    assertThat(issueCollector.getIssues())
        .contains(
            ValidationIssue.builder()
                .type(LoomConstants.NODE_VALIDATION_ERROR)
                .param("nodeType", TensorNode.TYPE)
                .summary("Tensor (TooManySources) has too many Operation sources: 2")
                .message("Tensor id: " + tensorNode.getId())
                .context(tensorNode.asContext("Tensor"))
                .context(opNode1.asContext("Source Operation #0 (op1)"))
                .context(opNode2.asContext("Source Operation #1 (op2)"))
                .build());
  }
}
