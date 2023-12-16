package loom.demo;

import loom.graph.CommonEnvironments;
import loom.graph.LoomEnvironment;
import loom.graph.LoomGraph;
import loom.graph.nodes.AllTensorsHaveExactlyOneSourceOperationConstraint;
import loom.graph.nodes.OperationNode;
import loom.graph.nodes.TensorNode;
import loom.testing.BaseTestClass;
import loom.validation.ValidationIssueCollector;
import loom.zspace.ZPoint;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class DemoTest extends BaseTestClass {

  public static void CycleCheckConstraint(
      LoomEnvironment env, LoomGraph graph, ValidationIssueCollector issueCollector) {
    // Assuming TensorNode::AllTensorsHaveExactlyOneSourceOperation has already been run;
    // verify that there are no cycles in the graph.
  }

  public static LoomEnvironment demoEnvironment() {
    return CommonEnvironments.simpleTensorEnvironment("int32")
        .addConstraint(new AllTensorsHaveExactlyOneSourceOperationConstraint())
        .addConstraint(DemoTest::CycleCheckConstraint);
  }

  @Test
  public void testDemo() {
    var env = demoEnvironment();
    var graph = env.createGraph();

    var tensorA =
        graph.addNode(
            TensorNode.builder()
                .label("A")
                .body(TensorNode.Body.builder().dtype("int32").shape(new ZPoint(2, 3)).build()));

    var op1 =
        graph.addNode(
            OperationNode.builder()
                .label("op1")
                .body(
                    OperationNode.Body.builder()
                        .opName("source")
                        .outputs(Map.of("pin", List.of(tensorA.getId())))
                        .build()));

    graph.validate();

    assertThat(tensorA).isInstanceOf(TensorNode.class).hasFieldOrPropertyWithValue("graph", graph);

    assertThat(op1.getOutputNodes()).containsEntry("pin", List.of(tensorA));

    assertThat(OperationNode.GraphOps.getSourceNode(tensorA)).isSameAs(op1);
    assertThat(OperationNode.GraphOps.getSourceId(tensorA)).isSameAs(op1.getId());
  }
}
