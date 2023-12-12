package loom.demo;

import java.util.List;
import java.util.Map;
import loom.graph.CommonEnvironments;
import loom.graph.LoomEnvironment;
import loom.graph.LoomGraph;
import loom.graph.NodeApi;
import loom.graph.nodes.AllTensorsHaveExactlyOneSourceOperationConstraint;
import loom.graph.nodes.TensorNode;
import loom.testing.BaseTestClass;
import loom.validation.ValidationIssueCollector;
import loom.zspace.ZPoint;
import org.junit.Test;

public class DemoTest extends BaseTestClass {

  public static void CycleCheckConstraint(
      LoomEnvironment env, LoomGraph graph, ValidationIssueCollector issueCollector) {
    // Assuming TensorNode::AllTensorsHaveExactlyOneSourceOperation has already been run;
    // verify that there are no cycles in the graph.
  }

  @Test
  public void testDemo() {
    var env = CommonEnvironments.simpleTensorEnvironment("int32");
    env.getConstraints().add(new AllTensorsHaveExactlyOneSourceOperationConstraint());
    env.getConstraints().add(DemoTest::CycleCheckConstraint);
    var graph = env.createGraph();

    var tensorA = NodeApi.newTensor(graph, "int32", new ZPoint(2, 3));
    tensorA.setLabel("A");

    var op1 = NodeApi.newOperation(graph, "source", Map.of(), Map.of("pin", List.of(tensorA)));
    op1.setLabel("op1");

    graph.validate();

    assertThat(tensorA).isInstanceOf(TensorNode.class).hasFieldOrPropertyWithValue("graph", graph);

    assertThat(op1.getOutputNodes()).containsEntry("pin", List.of(tensorA));
    assertThat(tensorA.getSourceOperationNode()).isSameAs(op1);
    assertThat(tensorA.getSourceOperationId()).isSameAs(op1.getId());
  }
}
