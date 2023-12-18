package loom.graph.nodes;

import java.util.List;
import java.util.Map;
import loom.demo.DemoTest;
import loom.graph.CommonEnvironments;
import loom.graph.LoomEnvironment;
import loom.testing.BaseTestClass;
import loom.zspace.ZPoint;
import org.junit.Test;

public class OperationNodeTest extends BaseTestClass {
  public LoomEnvironment demoEnvironment() {
    return CommonEnvironments.simpleTensorEnvironment("int32")
        .addConstraint(new AllTensorsHaveExactlyOneSourceOperationConstraint())
        .addConstraint(DemoTest::CycleCheckConstraint);
  }

  @Test
  public void testBasic() {
    var env = demoEnvironment();
    var graph = env.createGraph();

    var tensorA =
        TensorNode.withBody(b -> b.dtype("int32").shape(new ZPoint(2, 3)))
            .label("A")
            .buildOn(graph);

    var op1 =
        OperationNode.withBody(
                b1 ->
                    b1.opName("source")
                        .inputs(OperationNode.nodeMapToIdMap(Map.of()))
                        .outputs(OperationNode.nodeMapToIdMap(Map.of("pin", List.of(tensorA)))))
            .buildOn(graph);
    op1.setLabel("op1");

    var op2 =
        OperationNode.withBody(
                b ->
                    b.opName("sink")
                        .inputs(OperationNode.nodeMapToIdMap(Map.of("inputs", List.of(tensorA))))
                        .outputs(OperationNode.nodeMapToIdMap(Map.of()))
                        .param("xyz", 123))
            .buildOn(graph);
    op2.setLabel("op2");

    assertThat(op2.getParams()).containsEntry("xyz", 123);

    graph.validate();

    assertThat(op1.getOutputNodes()).containsEntry("pin", List.of(tensorA));
    assertThat(op2.getInputNodes()).containsEntry("inputs", List.of(tensorA));
  }
}
