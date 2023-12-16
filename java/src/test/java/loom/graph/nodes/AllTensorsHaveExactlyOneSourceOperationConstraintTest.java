package loom.graph.nodes;

import java.util.List;
import java.util.Map;
import loom.demo.DemoTest;
import loom.graph.CommonEnvironments;
import loom.graph.LoomConstants;
import loom.graph.NodeApi;
import loom.testing.BaseTestClass;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;
import loom.zspace.ZPoint;
import org.junit.Test;

public class AllTensorsHaveExactlyOneSourceOperationConstraintTest extends BaseTestClass {
  @Test
  public void testSingleSourceConstraint_NoSource() {
    var env = CommonEnvironments.simpleTensorEnvironment("int32");
    env.getConstraints().add(new AllTensorsHaveExactlyOneSourceOperationConstraint());
    env.getConstraints().add(DemoTest::CycleCheckConstraint);
    var graph = env.createGraph();

    var tensorA = NodeApi.newTensor(graph, "int32", new ZPoint(2, 3));
    tensorA.setLabel("TooManySources");

    ValidationIssueCollector issueCollector = new ValidationIssueCollector();
    graph.validate(issueCollector);

    assertThat(issueCollector.getIssues())
        .contains(
            ValidationIssue.builder()
                .type(LoomConstants.NODE_VALIDATION_ERROR)
                .param("nodeType", TensorNode.Prototype.TYPE)
                .context(
                    ValidationIssue.Context.builder()
                        .name("Tensor")
                        .jsonpath(tensorA.getJsonPath())
                        .jsonData(tensorA.toJsonString()))
                .summary("Tensor (TooManySources) has no Operation source")
                .build());
  }

  @Test
  public void testSingleSourceConstraint_TooManySources() {
    var env = CommonEnvironments.simpleTensorEnvironment("int32");
    env.getConstraints().add(new AllTensorsHaveExactlyOneSourceOperationConstraint());
    env.getConstraints().add(DemoTest::CycleCheckConstraint);
    var graph = env.createGraph();

    var tensorA = NodeApi.newTensor(graph, "int32", new ZPoint(2, 3));
    tensorA.setLabel("TooManySources");

    var op1 =
        NodeApi.newOperation(graph, "source", Map.of(), Map.of("pin", List.of(tensorA)), null);
    op1.setLabel("op1");
    var op2 =
        NodeApi.newOperation(graph, "source", Map.of(), Map.of("pin", List.of(tensorA)), null);
    op2.setLabel("op2");

    ValidationIssueCollector issueCollector = new ValidationIssueCollector();
    graph.validate(issueCollector);

    // System.out.println(issueCollector.toDisplayString());

    assertThat(issueCollector.getIssues())
        .contains(
            ValidationIssue.builder()
                .type(LoomConstants.NODE_VALIDATION_ERROR)
                .param("nodeType", TensorNode.Prototype.TYPE)
                .context(
                    ValidationIssue.Context.builder()
                        .name("Tensor")
                        .jsonpath(tensorA.getJsonPath())
                        .jsonData(tensorA.toJsonString()))
                .summary("Tensor (TooManySources) has too many Operation sources: 2")
                .message("Tensor id: " + tensorA.getId())
                .context(
                    ValidationIssue.Context.builder()
                        .name("Source Operation #0 (op1)")
                        .jsonpath(op1.getJsonPath())
                        .jsonData(op1.toJsonString()))
                .context(
                    ValidationIssue.Context.builder()
                        .name("Source Operation #1 (op2)")
                        .jsonpath(op2.getJsonPath())
                        .jsonData(op2.toJsonString()))
                .build());
  }
}
