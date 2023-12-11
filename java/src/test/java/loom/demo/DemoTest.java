package loom.demo;

import loom.graph.CommonEnvironments;
import loom.graph.LoomGraph;
import loom.graph.nodes.OperationNode;
import loom.graph.nodes.TensorNode;
import loom.testing.BaseTestClass;
import loom.zspace.ZPoint;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class DemoTest extends BaseTestClass {
  public static TensorNode newTensor(LoomGraph graph, String dtype, ZPoint shape) {
    return graph.addNode(
        TensorNode.builder()
            .type(TensorNode.Meta.TYPE)
            .body(TensorNode.Body.builder().dtype(dtype).shape(shape).build()));
  }

  public static OperationNode newOperation(
      LoomGraph graph,
      String opName,
      Map<String, List<TensorNode>> inputs,
      Map<String, List<TensorNode>> outputs) {
    return graph.addNode(
        OperationNode.builder()
            .type(OperationNode.Meta.TYPE)
            .body(
                OperationNode.Body.builder()
                    .opName(opName)
                    .inputs(OperationNode.nodeMapToIdMap(inputs))
                    .outputs(OperationNode.nodeMapToIdMap(outputs))
                    .build()));
  }

  @Test
  public void testDemo() {
    var env = CommonEnvironments.simpleTensorEnvironment("int32");
    var graph = env.createGraph();

    var tensorA = newTensor(graph, "int32", new ZPoint(2, 3));

    var op = newOperation(graph, "source", Map.of(), Map.of("pin", List.of(tensorA)));

    assertThat(tensorA).isInstanceOf(TensorNode.class).hasFieldOrPropertyWithValue("graph", graph);

    assertThat(op.getOutputNodes()).containsEntry("pin", List.of(tensorA));
  }
}
