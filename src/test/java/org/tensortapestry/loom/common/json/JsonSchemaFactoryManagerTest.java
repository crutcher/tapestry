package org.tensortapestry.loom.common.json;

import static org.tensortapestry.loom.common.json.JsonSchemaFactoryManager.JSD_ERROR;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.ValidationMessage;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.junit.Test;
import org.tensortapestry.loom.testing.BaseTestClass;
import org.tensortapestry.loom.validation.ListValidationIssueCollector;
import org.tensortapestry.loom.validation.ValidationIssue;

public class JsonSchemaFactoryManagerTest extends BaseTestClass {

  @Nonnull
  private final JsonSchemaFactoryManager manager = new JsonSchemaFactoryManager();

  @Value
  @Jacksonized
  @Builder
  public static class Example {

    @Nonnull
    UUID id;

    @Nonnull
    String name;
  }

  @Test
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public void test_unmapped_uri() {
    assertThatExceptionOfType(RuntimeException.class)
      .isThrownBy(() -> manager.loadSchema(URI.create("http://loom.example/data")));
  }

  @Test
  public void test_if() {
    manager.addSchema(
      "http://loom.example/data",
      """
           {
                "$schema": "https://json-schema.org/draft/2020-12/schema#",
                "allOf": [
                   {
                       "if": {
                           "properties": {
                               "type": {
                                   "enum": [ "A" ],
                                   "description": "A"
                               }
                           },
                           "required": ["type"]
                       },
                       "then": {
                           "properties": {
                               "type": {
                                   "const": "A"
                               },
                               "a": {
                                   "type": "string"
                               }
                           },
                           "required": ["type", "a"],
                           "additionalProperties": false
                       }
                   },
                   {
                       "if": {
                           "properties": {
                               "type": {
                                   "enum": [ "B" ]
                               }
                           },
                           "required": ["type"]
                       },
                       "then": {
                           "properties": {
                               "type": {
                                   "const": "B"
                               },
                               "x": {
                                   "type": "integer"
                               }
                           },
                           "required": ["type", "x"],
                           "additionalProperties": false
                       }
                   }
                ],
                "definitions": {
                    "ZType": {
                        "$anchor": "zz",
                        "type": "string",
                        "enum": ["A", "B", "C"]
                    }
                }
           }
           """
    );

    URI schemaUri = URI.create("http://loom.example/data");
    var schema = manager.loadSchema(schemaUri);
    schema.initializeValidators();

    assertThat(schema.validate(JsonUtil.valueToJsonNodeTree(Map.of("type", "A", "a", "foo"))))
      .isEmpty();
    assertThat(schema.validate(JsonUtil.valueToJsonNodeTree(Map.of("type", "B", "x", 12))))
      .isEmpty();

    assertThat(schema.validate(JsonUtil.valueToJsonNodeTree(Map.of("type", "A", "a", 12))))
      .hasSize(1);
  }

  @Test
  public void test() {
    manager.addSchema(
      "http://loom.example/data",
      """
           {
                "$schema": "https://json-schema.org/draft/2020-12/schema#",
                "definitions": {
                    "ZType": {
                        "$anchor": "zz",
                        "type": "string",
                        "enum": ["A", "B", "C"]
                    }
                }
           }
           """
    );
    manager.addSchema(
      "http://loom.example/example",
      """
                {
                    "$id": "http://loom.example/example",
                    "$schema": "https://json-schema.org/draft/2020-12/schema#",
                    "title": "Example Schema",
                    "type": "object",
                    "properties": {
                        "id": {
                            "$ref": "#foo"
                        },
                        "name": {
                            "$ref": "http://loom.example/data#zz"
                        }
                    },
                    "required": [ "id", "name" ],
                    "additionalProperties": false,
                    "definitions": {
                        "id": {
                            "$anchor": "foo",
                            "type": "string",
                            "format": "uuid"
                        }
                    }
                }
                """
    );

    URI schemaUri = URI.create("http://loom.example/example");
    var schema = manager.loadSchema(schemaUri);
    schema.initializeValidators();

    var example = Example.builder().id(UUID.randomUUID()).name("N").build();
    JsonNode exampleTree = JsonUtil.valueToJsonNodeTree(example);

    Set<ValidationMessage> errors = schema.validate(exampleTree);
    assertThat(errors).hasSize(1);

    var msg = errors.stream().findAny().orElseThrow();
    assertThat(
      JsonUtil.jsonPathOnValue(exampleTree, msg.getInstanceLocation().toString(), String.class)
    )
      .isEqualTo("N");
  }

  @Test
  public void test_lookup_by_pointer() {
    manager.addSchema(
      "http://loom.example/data",
      """
          {
            "$schema": "http://json-schema.org/draft/2020-12/schema#",
            "type": "object",
            "$defs": {
              "nodes": {
                "$defs": {
                  "ztype": {
                      "type": "string",
                      "enum": ["A", "B", "C"]
                  }
                }
              }
            }
          }
          """
    );
    var schema = manager.loadSchema(
      URI.create("http://loom.example/data#/$defs/nodes/$defs/ztype")
    );

    assertThat(schema.validate(JsonUtil.valueToJsonNodeTree("A"))).isEmpty();
    assertThat(schema.validate(JsonUtil.valueToJsonNodeTree("X"))).hasSize(1);
  }

  @Test
  public void test_nested_schemas() {
    manager.addSchema(
      "http://loom.example/schema1",
      """
                {
                  "innerA": {
                    "$schema": "http://json-schema.org/draft/2020-12/schema#",
                    "type": "object",
                    "$defs": {
                      "ztype": {
                          "type": "string",
                          "enum": ["A", "B", "C"]
                      }
                    }
                  },
                  "innerB": {
                    "$schema": "http://json-schema.org/draft/2020-12/schema#",
                    "type": "array",
                    "items": {
                      "$ref": "#/innerA/$defs/ztype"
                    }
                  }
                }
                """
    );
    manager.addSchema(
      "http://loom.example/schema2",
      """
                {
                  "innerC": {
                    "$schema": "http://json-schema.org/draft/2020-12/schema#",
                    "$ref": "http://loom.example/schema1#/innerB"
                  }
                }
                """
    );

    {
      var schema = manager.loadSchema(
        URI.create("http://loom.example/schema1#/innerA/$defs/ztype")
      );
      assertThat(schema.validate(JsonUtil.valueToJsonNodeTree("A"))).isEmpty();
      assertThat(schema.validate(JsonUtil.valueToJsonNodeTree("X"))).hasSize(1);
    }

    {
      var schema = manager.loadSchema(URI.create("http://loom.example/schema1#/innerB"));
      assertThat(schema.validate(JsonUtil.valueToJsonNodeTree(List.of("A", "B")))).isEmpty();
      assertThat(schema.validate(JsonUtil.valueToJsonNodeTree(List.of("X")))).hasSize(1);
    }

    {
      var schema = manager.loadSchema(URI.create("http://loom.example/schema2#/innerC"));
      assertThat(schema.validate(JsonUtil.valueToJsonNodeTree(List.of("A", "B")))).isEmpty();
      assertThat(schema.validate(JsonUtil.valueToJsonNodeTree(List.of("X")))).hasSize(1);
    }
  }

  @Data
  @Jacksonized
  @Builder
  public static class ExampleClass {

    private UUID id;
    private List<Integer> shape;
  }

  @Test
  public void test_adapt_scan() {
    manager.addSchema(
      """
    {
        "$id": "http://loom.example/data",
        "$schema": "https://json-schema.org/draft/2020-12/schema",
        "definitions": {
            "Shape": {
                "type": "array",
                "items": {
                    "type": "integer",
                    "minimum": 1
                },
                "minItems": 1
            }
        }
    }
    """
    );

    manager.addSchema(
      """
    {
        "$id": "http://loom.example/example",
        "$schema": "https://json-schema.org/draft/2020-12/schema",
        "type": "object",
        "properties": {
            "id": {
                "type": "string",
                "format": "uuid"
            },
            "shape": {
                "$ref": "http://loom.example/data#/definitions/Shape"
            }
        },
        "required": ["id", "shape"]
    }
    """
    );

    final var path = "$.nodes[@.id = 'foo']";
    var example = ExampleClass.builder().id(UUID.randomUUID()).shape(List.of(1, 2, 3, -1)).build();

    var uri = URI.create("http://loom.example/example");
    var schema = manager.loadSchema(uri);
    schema.initializeValidators();

    var data = JsonUtil.valueToJsonNodeTree(example);

    Supplier<List<ValidationIssue.Context>> contexts = () ->
      List.of(ValidationIssue.Context.builder().name("Node").jsonpath(path).data(data).build());

    ListValidationIssueCollector collector = new ListValidationIssueCollector();
    manager
      .issueScan()
      .issueCollector(collector)
      .type(JSD_ERROR)
      .summaryPrefix("Body ")
      .jsonPathPrefix(path)
      .schema(schema)
      .data(data)
      .contexts(contexts)
      .build()
      .scan();

    assertValidationIssues(
      collector,
      ValidationIssue
        .builder()
        .type(JSD_ERROR)
        .param("keyword", "minimum")
        .param("keywordArgs", List.of(1))
        .param("path", "$.nodes[@.id = 'foo'].shape[3]")
        .param("schemaPath", "http://loom.example/data#/definitions/Shape/items/minimum")
        .summary("Body [minimum] :: $.shape[3]: -1")
        .message("$.shape[3]: must have a minimum value of 1")
        .context(
          ValidationIssue.Context
            .builder()
            .name("Data")
            .jsonpath("$.nodes[@.id = 'foo'].shape[3]")
            .data(-1)
            .build()
        )
        .withContexts(contexts)
        .build()
    );
  }
}
