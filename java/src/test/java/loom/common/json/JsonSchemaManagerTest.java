package loom.common.json;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import loom.testing.BaseTestClass;
import loom.validation.ListValidationIssueCollector;
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

    var schema = manager.loadSchema(EXAMPLE_SCHEMA);
    assertThat(schema).isInstanceOf(JsonSchema.class);
    assertThat(manager.loadSchema(EXAMPLE_SCHEMA)).isSameAs(schema);
  }

  @Test
  public void test() {
    var manager = new JsonSchemaManager();

    var instance = new ExampleClass();
    instance.id = UUID.randomUUID();
    instance.shape = Arrays.asList(1, 2, 3, -1);

    final String instanceJson = JsonUtil.toJson(instance);

    var schema = manager.loadSchema(EXAMPLE_SCHEMA);

    var problems = manager.validationProblems(schema, instanceJson);

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

    var schema = manager.loadSchema(EXAMPLE_SCHEMA);

    var issueCollector = new ListValidationIssueCollector();
    manager
      .issueScan()
      .issueCollector(issueCollector)
      .param("foo", "bar")
      .schema(schema)
      .summaryPrefix("[qqq] ")
      .json(instanceJson)
      .jsonPathPrefix("foo.bar[2]")
      .context(
        ValidationIssue.Context
          .builder()
          .name("foo")
          .jsonpath("$.foo")
          .dataFromJson("{\"foo\": \"bar\"}")
          .build()
      )
      .build()
      .scan();

    // System.out.println(collector.toDisplayString());

    assertThat(issueCollector.getIssues())
      .hasSize(1)
      .extracting(ValidationIssue::getSummary)
      .contains(
        "[qqq] /shape/3 [minimum] :: The numeric value must be greater than or equal to 1."
      );
  }

  @Test
  public void test_formatParseError() {
    var manager = new JsonSchemaManager();

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() ->
        manager.loadSchema("""
        {
          "type": "object",
        }
        """)
      )
      .withMessageContaining("Error parsing JSON schema")
      .withMessageContaining(
        """
                       {
                         "type": "object",
                   >>> }
                       """
      );
  }
}
