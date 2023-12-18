package loom.demo;

import java.util.List;
import java.util.Map;
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

    TensorNode tensorF =
        TensorNode.withBody(
                TensorNode.Body.builder().dtype("int32").shape(new ZPoint(2, 3)).build())
            .label("F")
            .buildOn(graph);

    TensorNode tensorG =
        TensorNode.withBody(TensorNode.Body.builder().dtype("int32").shape(new ZPoint(2, 3)))
            .label("G")
            .buildOn(graph);

    TensorNode tensorI =
        TensorNode.withBody(
                b -> {
                  b.dtype("int32");
                  b.shape(new ZPoint(2, 3));
                })
            .label("I")
            .buildOn(graph);

    var tensors = List.of(tensorA, tensorB, tensorC, tensorF, tensorG, tensorI);
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
    assertThat(op1.getOutputNodes()).containsEntry("pin", tensors);

    assertThat(OperationNode.GraphOps.getSourceNode(tensorA)).isSameAs(op1);
    assertThat(OperationNode.GraphOps.getSourceId(tensorA)).isSameAs(op1.getId());
  }
}
