package loom.common.serialization;

import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.*;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Map;
import loom.testing.CommonAssertions;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.ValidationError;
import net.jimblackler.jsonschemafriend.Validator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class JsonSchemaTest implements CommonAssertions {
  @XmlRootElement
  public static class Example {
    private String a;
    private int b;

    public Example() {}

    public Example(String a, int b) {
      this.a = a;
      this.b = b;
    }

    @XmlElement(name = "a")
    public String getA() {
      return a;
    }

    public void setA(String a) {
      this.a = a;
    }

    @XmlElement(name = "b")
    public int getB() {
      return b;
    }

    public void setB(int b) {
      this.b = b;
    }
  }

  @Rule public TemporaryFolder temp = new TemporaryFolder();

  private File writeTemp(String localName, String content) {
    try {
      File file = temp.newFile(localName);
      // Write `schemaJson' to `schemaFile'
      try (var writer = new java.io.FileWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
        writer.write(content);
      }
      return file;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testExampleSchema() throws Exception {
    JsonUtilTest.Example example = new JsonUtilTest.Example("hello", 2);

    var schemaStore = new SchemaStore();

    String schemaJson =
        """
                      {
                          "$schema": "http://json-schema.org/draft-07/schema#",
                          "type": "object",
                          "definitions": {
                            "example": {
                              "type": "object",
                              "properties": {
                                  "a": {
                                    "type": "string",
                                    "example": "hello"
                                  },
                                  "b": {
                                    "type": "integer",
                                    "example": 2
                                  }
                              },
                              "required": ["a", "b"],
                              "example": {
                                  "a": "hello",
                                  "b": 2
                                }
                            }
                          }
                      }

                            """;

    var schemaFile = writeTemp("schema.json", schemaJson);

    // var schema =
    //  schemaStore.loadSchemaJson(
    //          schemaJson);

    URI schemaUri = schemaFile.toURI();
    var schema = schemaStore.loadSchema(new URI(schemaUri + "#/definitions/example"));

    System.out.printf(
        """
              resource uri: %s
              """,
        ((URI) schema.getResourceUri()).getRawFragment());

    var validator = new Validator();
    validator.validate(schema, JsonUtil.treeToSimpleJson(JsonUtil.toTree(example)));

    var errors = new ArrayList<ValidationError>();
    schema.validateExamples(validator, errors::add);
    assertThat(errors).isEmpty();
  }

  @Test
  public void testVertx() throws Exception {
    var repo =
        SchemaRepository.create(
            new JsonSchemaOptions()
                .setOutputFormat(OutputFormat.Basic)
                .setBaseUri("file:///")
                .setDraft(Draft.DRAFT202012));

    String schemaJson =
        """
                      {
                          "type": "object",
                          "definitions": {
                            "example": {
                              "type": "object",
                              "properties": {
                                  "a": {
                                    "type": "string",
                                    "example": "hello"
                                  },
                                  "b": {
                                    "type": "integer",
                                    "example": 2,
                                    "x-foo": "abc"
                                  }
                              },
                              "required": ["a", "b"],
                              "example": {
                                  "a": "hello",
                                  "b": 2
                                }
                            }
                          }
                      }

                            """;

    String schemaUri = "file:///foo.json";

    JsonSchema baseSchema = JsonSchema.of(schemaUri, new JsonObject(schemaJson));
    repo.dereference(baseSchema);

    var exampleUri = schemaUri + "#/definitions/example";

    var val = repo.validator(exampleUri);

    JsonUtilTest.Example example = new JsonUtilTest.Example("hello", 2);

    var out = val.validate(new JsonObject(JsonUtil.toJson(example)));
    out.checkValidity();

    assertThatExceptionOfType(JsonSchemaValidationException.class)
        .isThrownBy(
            () -> val.validate(new JsonObject(JsonUtil.toJson(Map.of("a", 12)))).checkValidity())
        .withMessageContaining("type number is invalid. Expected string");
  }
}
