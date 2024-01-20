package loom.experiment;

import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import loom.common.json.JsonSchemaManager;
import loom.common.json.JsonUtil;
import loom.graph.nodes.ApplicationNode;
import loom.graph.nodes.IPFSignature;
import loom.graph.nodes.TensorSelection;
import loom.polyhedral.IndexProjectionFunction;
import loom.testing.BaseTestClass;
import loom.zspace.ZRange;
import org.junit.Test;

public class JacksonSchemaTest extends BaseTestClass {
  @Value
  @Jacksonized
  @Builder
  public static class Example {
    UUID id;
  }

  private final JsonSchemaManager JSM = new JsonSchemaManager();

  private void trial(Object obj) {
    String schema = JsonUtil.jsonSchemaForClass(obj.getClass());
    // System.out.println(schema);
    assertThat(JSM.validationProblems(schema, JsonUtil.toJson(obj))).isEmpty();
  }

  @Test
  public void test_ipf() {
    trial(
        IPFSignature.builder()
            .input(
                "a",
                IndexProjectionFunction.builder().affineMap(new int[][] {{1, 0}, {0, 1}}).build())
            .build());
  }

  @Test
  public void test_app() {

    trial(
        ApplicationNode.Body.builder()
            .operationId(UUID.randomUUID())
            .input(
                "a",
                List.of(
                    TensorSelection.builder()
                        .tensorId(UUID.randomUUID())
                        .range(ZRange.newFromShape(10, 20))
                        .build()))
            .build());
  }
}
