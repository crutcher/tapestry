package loom.demo;

import java.util.List;
import java.util.Map;
import loom.graph.CommonEnvironments;
import loom.graph.LoomEnvironment;
import loom.graph.LoomGraph;
import loom.graph.nodes.GraphUtils;
import loom.graph.nodes.OperationNode;
import loom.graph.nodes.TensorNode;
import loom.testing.BaseTestClass;
import loom.zspace.ZPoint;
import org.junit.Test;

public class DemoTest extends BaseTestClass {

  public static LoomEnvironment demoEnvironment() {
    return CommonEnvironments.expressionEnvironment();
  }

  @Test
  public void testDemo() {
    var env = demoEnvironment();
    var graph = env.createGraph();

    TensorNode tensorA =
        graph.addNode(
            TensorNode.builder()
                .label("A")
                .body(TensorNode.Body.builder().dtype("int32").shape(new ZPoint(2, 3)).build()));

    TensorNode tensorB =
        (TensorNode)
            graph.buildNode(TensorNode.TYPE, "B", Map.of("dtype", "int32", "shape", List.of(2, 3)));

    TensorNode tensorC =
        (TensorNode) graph.buildNode(TensorNode.TYPE, "{\"dtype\": \"int32\", \"shape\": [2, 3]}");

    TensorNode tensorI =
        TensorNode.withBody(
                b -> {
                  b.dtype("int32");
                  b.shape(new ZPoint(2, 3));
                })
            .label("I")
            .buildOn(graph);

    var tensors = List.of(tensorA, tensorB, tensorC, tensorI);
    var tensorIds = tensors.stream().map(LoomGraph.Node::getId).toList();

    var op1 =
        graph.addNode(
            OperationNode.builder()
                .label("op1")
                .body(
                    OperationNode.Body.builder()
                        .opName("source")
                        .outputs(Map.of("pin", tensorIds))
                        .build()));

    {
      assertThat(tensorA.getDtype()).isEqualTo("int32");
      assertThat(tensorA.getShape()).isEqualTo(new ZPoint(2, 3));
    }

    // System.out.println(graph.toPrettyJsonString());

    graph.validate();

    assertThat(tensorA).isInstanceOf(TensorNode.class).hasFieldOrPropertyWithValue("graph", graph);

    assertThat(op1.getOutputs()).containsEntry("pin", tensorIds);
    assertThat(op1.getOutputNodeListMap()).containsEntry("pin", tensors);

    assertThat(GraphUtils.getSourceNode(tensorA)).isSameAs(op1);
    assertThat(GraphUtils.getSourceId(tensorA)).isSameAs(op1.getId());
  }
}
