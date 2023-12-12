package loom.graph.nodes;

import java.util.List;
import java.util.Map;
import loom.demo.DemoTest;
import loom.graph.CommonEnvironments;
import loom.graph.NodeApi;
import loom.testing.BaseTestClass;
import loom.zspace.ZPoint;
import org.junit.Test;

public class OperationNodeTest extends BaseTestClass {
  @Test
  public void testBasic() {
    var env = CommonEnvironments.simpleTensorEnvironment("int32");
    env.getConstraints().add(new AllTensorsHaveExactlyOneSourceOperationConstraint());
    env.getConstraints().add(DemoTest::CycleCheckConstraint);
    var graph = env.createGraph();

    var tensorA = NodeApi.newTensor(graph, "int32", new ZPoint(2, 3));
    tensorA.setLabel("A");

    var op1 =
        NodeApi.newOperation(graph, "source", Map.of(), Map.of("pin", List.of(tensorA)), null);
    op1.setLabel("op1");

    var op2 =
        NodeApi.newOperation(
            graph, "sink", Map.of("inputs", List.of(tensorA)), Map.of(), Map.of("xyz", 123));
    op2.setLabel("op2");

    assertThat(op2.getParams()).containsEntry("xyz", 123);

    graph.validate();

    assertThat(op1.getOutputNodes()).containsEntry("pin", List.of(tensorA));
    assertThat(op2.getInputNodes()).containsEntry("inputs", List.of(tensorA));
  }
}