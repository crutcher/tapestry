package loom.demo;

import loom.graph.CommonEnvironments;
import loom.graph.LoomGraph;
import loom.graph.nodes.TensorNode;
import loom.testing.BaseTestClass;
import loom.zspace.ZPoint;
import org.junit.Test;

public class DemoTest extends BaseTestClass {
  public static TensorNode newTensor(LoomGraph graph, String dtype, ZPoint shape) {
    return graph.addNode(
        TensorNode.builder()
            .type(TensorNode.Meta.TYPE)
            .body(TensorNode.Body.builder().dtype(dtype).shape(shape).build()));
  }

  @Test
  public void testDemo() {
    var env = CommonEnvironments.simpleTensorEnvironment("int32");
    var graph = env.createGraph();

    var tensorA = newTensor(graph, "int32", new ZPoint(2, 3));

    assertThat(tensorA).isInstanceOf(TensorNode.class).hasFieldOrPropertyWithValue("graph", graph);
  }
}
