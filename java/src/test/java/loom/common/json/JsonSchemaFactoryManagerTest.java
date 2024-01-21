package loom.common.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.ValidationMessage;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import loom.testing.BaseTestClass;
import org.junit.Test;

public class JsonSchemaFactoryManagerTest extends BaseTestClass {

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
    var manager = new JsonSchemaFactoryManager();
    assertThatExceptionOfType(RuntimeException.class)
      .isThrownBy(() -> manager.getSchema(URI.create("http://loom.example/data")));
  }

  @Test
  public void test_bad_schema() {}

  @Test
  public void test_if() {
    var manager = new JsonSchemaFactoryManager();
    manager.addSchema(
      "http://loom.example/data",
      """
           {
                "$schema": "https://json-schema.org/draft/2020-12/schema",
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
    var schema = manager.getSchema(schemaUri);
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
    var manager = new JsonSchemaFactoryManager();
    manager.addSchema(
      "http://loom.example/data",
      """
           {
                "$schema": "https://json-schema.org/draft/2020-12/schema",
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
                    "$schema": "https://json-schema.org/draft/2020-12/schema",
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
    var schema = manager.getSchema(schemaUri);
    schema.initializeValidators();

    var example = Example.builder().id(UUID.randomUUID()).name("N").build();
    JsonNode exampleTree = JsonUtil.valueToJsonNodeTree(example);

    Set<ValidationMessage> errors = schema.validate(exampleTree);
    assertThat(errors).hasSize(1);

    var msg = errors.stream().findAny().orElseThrow();
    assertThat(JsonUtil.jsonPathOnValue(exampleTree, msg.getPath(), String.class)).isEqualTo("N");
  }
}
