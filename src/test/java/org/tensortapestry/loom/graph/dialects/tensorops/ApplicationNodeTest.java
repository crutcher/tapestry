package org.tensortapestry.loom.graph.dialects.tensorops;

import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.tensortapestry.loom.graph.CommonEnvironments;
import org.tensortapestry.loom.testing.BaseTestClass;
import org.tensortapestry.loom.zspace.ZRange;

public class ApplicationNodeTest extends BaseTestClass {

  @Test
  public void test_body() {
    UUID operationId = UUID.randomUUID();
    UUID tensorIdA = UUID.randomUUID();
    UUID tensorIdB = UUID.randomUUID();
    ApplicationBody body = ApplicationBody
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

    var inputTensor = TensorOpNodes
      .tensorBuilder(graph, b -> b.shape(2, 3).dtype("float32"))
      .build();
    var outputTensor = TensorOpNodes.tensorBuilder(graph, b -> b.shape(10).dtype("int32")).build();

    var operation = TensorOpNodes
      .operationBuilder(
        graph,
        b ->
          b
            .kernel("increment")
            .input(
              "x",
              List.of(
                new TensorSelection(
                  inputTensor.getId(),
                  inputTensor.viewBodyAs(TensorBody.class).getRange()
                )
              )
            )
            .output(
              "y",
              List.of(
                new TensorSelection(
                  outputTensor.getId(),
                  outputTensor.viewBodyAs(TensorBody.class).getRange()
                )
              )
            )
      )
      .build();
    OperationBody opBody = operation.viewBodyAs(OperationBody.class);

    var application = TensorOpNodes
      .applicationBuilder(
        graph,
        b ->
          b
            .operationId(UUID.randomUUID())
            .operationId(operation.getId())
            .inputs(opBody.getInputs())
            .outputs(opBody.getOutputs())
      )
      .build();

    assertThat(ApplicationBody.getOperation(application)).isSameAs(operation);
    assertThat(OperationBody.getShards(operation)).containsOnly(application);

    graph.validate();
  }
}
