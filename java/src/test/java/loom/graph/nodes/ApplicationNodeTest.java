package loom.graph.nodes;

import java.util.List;
import java.util.UUID;
import loom.testing.BaseTestClass;
import loom.zspace.ZRange;
import org.junit.Test;

public class ApplicationNodeTest extends BaseTestClass {
  @Test
  public void test_body() {
    UUID operationId = UUID.randomUUID();
    UUID tensorIdA = UUID.randomUUID();
    UUID tensorIdB = UUID.randomUUID();
    ApplicationNode.Body body =
        ApplicationNode.Body.builder()
            .operationId(operationId)
            .input(
                "source",
                List.of(
                    ApplicationNode.TensorSelection.builder()
                        .tensorId(tensorIdA)
                        .range(ZRange.fromShape(2, 3))
                        .build(),
                    ApplicationNode.TensorSelection.builder()
                        .tensorId(tensorIdB)
                        .range(ZRange.fromShape())
                        .build()))
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
        """
            .formatted(operationId, tensorIdA, tensorIdB));
  }
}
