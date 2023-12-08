package loom.common.json;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import loom.common.serialization.JsonUtil;
import loom.testing.BaseTestClass;
import loom.validation.ValidationIssue;
import org.junit.Test;
import org.leadpony.justify.api.JsonSchema;
import org.leadpony.justify.api.Problem;

public class JsonSchemaManagerTest extends BaseTestClass {

  @Data
  public static class ExampleClass {
    private UUID id;
    private List<Integer> shape;
  }

  public static final String EXAMPLE_SCHEMA =
      """
              {
                  "$schema": "http://json-schema.org/draft-07/schema#",
                  "type": "object",
                  "properties": {
                      "id": {
                          "type": "string",
                          "format": "uuid"
                      },
                      "shape": {
                          "type": "array",
                          "items": {
                              "type": "integer",
                              "minimum": 1
                          },
                          "minItems": 1
                      }
                  },
                  "required": ["id", "shape"]
              }
              """;

  @Test
  public void testCache() {
    var manager = new JsonSchemaManager();

    var schema = manager.getSchema(EXAMPLE_SCHEMA);
    assertThat(schema).isInstanceOf(JsonSchema.class);
    assertThat(manager.getSchema(EXAMPLE_SCHEMA)).isSameAs(schema);
  }

  @Test
  public void test() {
    var manager = new JsonSchemaManager();

    var instance = new ExampleClass();
    instance.id = UUID.randomUUID();
    instance.shape = Arrays.asList(1, 2, 3, -1);

    final String instanceJson = JsonUtil.toJson(instance);

    var problems = manager.validationProblems(EXAMPLE_SCHEMA, instanceJson);

    assertThat(problems)
        .hasSize(1)
        .extracting(Problem::getMessage)
        .contains("The numeric value must be greater than or equal to 1.");
  }

  @Test
  public void testIssueScan() {
    var manager = new JsonSchemaManager();

    var instance = new ExampleClass();
    instance.id = UUID.randomUUID();
    instance.shape = Arrays.asList(1, 2, 3, -1);

    final String instanceJson = JsonUtil.toJson(instance);

    var collector =
        manager
            .issueScan()
            .param("foo", "bar")
            .schemaSource(EXAMPLE_SCHEMA)
            .json(instanceJson)
            .jpathPrefix("foo.bar[2]")
            .context(
                ValidationIssue.Context.builder()
                    .name("foo")
                    .jsonpath("$.foo")
                    .jsonData("{\"foo\": \"bar\"}")
                    .build())
            .build()
            .scan();

    // System.out.println(collector.toDisplayString());

    assertThat(collector.getIssues())
        .hasSize(1)
        .extracting(ValidationIssue::getSummary)
        .contains("/shape/3 [minimum] :: The numeric value must be greater than or equal to 1.");
  }
}
