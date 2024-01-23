package loom.graph.nodes;

import java.util.List;
import java.util.UUID;
import loom.graph.CommonEnvironments;
import loom.testing.BaseTestClass;
import loom.zspace.ZRange;
import org.junit.Test;

public class ApplicationNodeTest extends BaseTestClass {

  @Test
  public void test_body() {
    UUID operationId = UUID.randomUUID();
    UUID tensorIdA = UUID.randomUUID();
    UUID tensorIdB = UUID.randomUUID();
    ApplicationNode.Body body = ApplicationNode.Body
      .builder()
      .operationId(operationId)
      .input(
        "source",
        List.of(
          TensorSelection.builder().tensorId(tensorIdA).range(ZRange.newFromShape(2, 3)).build(),
          TensorSelection.builder().tensorId(tensorIdB).range(ZRange.newFromShape()).build()
        )
      )
      .build();

    assertJsonEquals(
      body,
      """
        {
          "operationId": "%s",
          "inputs": {
            "source": [
              {
                "tensorId": "%s",
                "range": {
                  "start": [0, 0],
                  "end": [2, 3]
                }
              },
              {
                "tensorId": "%s",
                "range": {
                  "start": [],
                  "end": []
                }
              }
            ]
          },
          "outputs": {}
        }
        """.formatted(
          operationId,
          tensorIdA,
          tensorIdB
        )
    );
  }

  @Test
  public void test_valid() {
    var env = CommonEnvironments.expressionEnvironment();
    var graph = env.newGraph();

    var inputTensor = TensorNode.withBody(b -> b.shape(2, 3).dtype("float32")).addTo(graph);
    var outputTensor = TensorNode.withBody(b -> b.shape(10).dtype("int32")).addTo(graph);

    var opSig = OperationSignatureNode
      .withBody(b ->
        b
          .kernel("increment")
          .input("x", List.of(new TensorSelection(inputTensor.getId(), inputTensor.getRange())))
          .output("y", List.of(new TensorSelection(outputTensor.getId(), outputTensor.getRange())))
      )
      .addTo(graph);

    var app = ApplicationNode
      .withBody(b ->
        b
          .operationId(UUID.randomUUID())
          .operationId(opSig.getId())
          .inputs(opSig.getInputs())
          .outputs(opSig.getOutputs())
      )
      .addTo(graph);

    assertThat(app.getOperationSignatureNode()).isSameAs(opSig);
    assertThat(opSig.getApplicationNodes()).containsOnly(app);

    graph.validate();
  }
}
