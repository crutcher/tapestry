package loom.doozer;

import com.fasterxml.jackson.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.common.HasToJsonString;
import loom.common.serialization.JsonUtil;
import loom.testing.BaseTestClass;
import loom.zspace.ZPoint;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.Validator;
import org.junit.Test;

@SuppressWarnings("SameNameButDifferent")
public class DGraphTest extends BaseTestClass {

  // Goals:
  // Given a json fragment:
  //    > {
  //    >   "id": <UUID>,
  //    >   "type": <type>,
  //    >   "data": {
  //    >     <type specific fields>
  //    >   }
  //    > }
  //
  // 1. Match, based upon type, to a Node subclass.
  // 2. Validate the structure of the data field against a type-specific JSD schema.
  // 3. Parse the fragment into a type-specific Node subclass instance.
  // 4. Provide a way to serialize the Node subclass instance back into a json fragment.
  // 5. Provide 2 type-specific semantic validators:
  //    a. A validator that checks that the data field is valid against the JSD schema
  //       and the type specific semantic rules.
  //    b. A validator that checks that the node is valid in context of the full graph.
  // 6. Provide a way to read the data field as a generic JSON or Object tree.
  // 7. Provide a way to read the data field as a generic JSON or Object tree.
  // 8. Provide a way to read the data field as type-specific data.
  // 9. Provide a way to write the data field as type-specific data.
  //
  // In a validated node containing node references, it should be possible to
  // read and manipulate the references, and it should also be possible to
  // transparently traverse the references to read and manipulate the referenced
  // nodes.

  @Data
  @SuperBuilder
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public abstract static class Node<T extends Node.BodyCommon> implements HasToJsonString {
    public interface BodyCommon extends HasToJsonString {}

    private final UUID id;
    private String label;
    private final String type;

    protected T body;

    // TODO: collect body class, schema, and validation into a validator class.
    // This is to support evolving the schema and validation.

    @JsonIgnore
    public abstract Class<? extends T> getBodyClass();

    @JsonIgnore
    public abstract String getBodyJsonSchema();

    public final String bodyAsJson() {
      return getBody().toJsonString();
    }

    public final Map<String, Object> bodyAsMap() {
      return JsonUtil.toMap(getBody());
    }

    public final void setBodyFromJson(String json) {
      validateBodySchema(json);
      setBody(JsonUtil.fromJson(json, getBodyClass()));
      validateBody();
    }

    public final void validate() {
      validateBody();
      validateBodySchema(bodyAsJson());
    }

    public final void validateBodySchema(String json) {
      SchemaStore schemaStore = new SchemaStore();
      try {
        var schema = schemaStore.loadSchemaJson(getBodyJsonSchema());
        var validator = new Validator();
        validator.validateJson(schema, json);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    public abstract void validateBody();
  }

  @Jacksonized
  @SuperBuilder
  public static class GenericNode extends Node<GenericNode.Body> {
    @Data
    public static class Body implements BodyCommon {
      private Map<String, Object> fields;

      @JsonCreator
      public Body(Map<String, Object> fields) {
        this.fields = fields;
      }

      @JsonAnySetter
      public void setField(String name, Object value) {
        fields.put(name, value);
      }

      @JsonValue
      @JsonAnyGetter
      public Map<String, Object> getFields() {
        return fields;
      }
    }

    @Override
    public Class<Body> getBodyClass() {
      return Body.class;
    }

    @Override
    public String getBodyJsonSchema() {
      return """
              {
                "type": "object",
                "additionalProperties": true
              }
              """;
    }

    @Override
    public void validateBody() {}
  }

  @Jacksonized
  @SuperBuilder
  public static final class TNode extends Node<TNode.Body> {
    @Data
    @Jacksonized
    @Builder
    public static class Body implements BodyCommon {
      private String dtype;
      private ZPoint shape;
    }

    @Override
    public Class<Body> getBodyClass() {
      return Body.class;
    }

    @Override
    public String getBodyJsonSchema() {
      return """
              {
                "type": "object",
                "properties": {
                  "dtype": {
                    "type": "string"
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
                "required": ["dtype", "shape"]
              }
              """;
    }

    @Override
    public void validateBody() {
      if (!getShape().coords.isStrictlyPositive()) {
        throw new IllegalArgumentException("shape must be positive and non-empty: " + getShape());
      }
    }

    @JsonIgnore
    public String getDtype() {
      return getBody().getDtype();
    }

    @JsonIgnore
    public void setDtype(String dtype) {
      getBody().setDtype(dtype);
    }

    @JsonIgnore
    public ZPoint getShape() {
      return getBody().getShape();
    }

    @JsonIgnore
    public void setShape(ZPoint shape) {
      getBody().setShape(shape);
    }
  }

  @Test
  public void testNothing() {
    var source =
        """
          {
            "id": "00000000-0000-0000-0000-000000000000",
            "label": "foo",
            "type": "TreeNode",
            "body": {
              "dtype": "int32",
              "shape": [2, 3]
            }
          }
          """;
    {
      var node = JsonUtil.fromJson(source, TNode.class);
      node.validate();

      assertThat(node.getId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000000"));
      assertThat(node.getLabel()).isEqualTo("foo");

      assertThat(node.getBody().getShape()).isEqualTo(ZPoint.of(2, 3));
      assertThat(node.getBody().getDtype()).isEqualTo("int32");

      assertThat(node.getShape()).isEqualTo(ZPoint.of(2, 3));
      assertThat(node.getDtype()).isEqualTo("int32");

      assertJsonEquals(node.getBody(), node.bodyAsJson());

      assertEquivalentJson(source, node.toJsonString());

      assertEquivalentJson(
          node.bodyAsJson(),
          """
              {
                "dtype": "int32",
                "shape": [2, 3]
              }
              """);

      assertThat(node.bodyAsMap()).isEqualTo(Map.of("dtype", "int32", "shape", List.of(2, 3)));

      node.setShape(ZPoint.of(3, 4));
      node.setDtype("float32");
      assertEquivalentJson(
          node.bodyAsJson(),
          """
              {
                "dtype": "float32",
                "shape": [3, 4]
              }
              """);

      node.setBodyFromJson(
          """
              {
                "dtype": "float32",
                "shape": [5, 6]
              }
              """);
      assertEquivalentJson(
          node.bodyAsJson(),
          """
              {
                "dtype": "float32",
                "shape": [5, 6]
              }
              """);
    }

    {
      var node = JsonUtil.fromJson(source, GenericNode.class);
      node.validate();

      assertThat(node.getId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000000"));
      assertThat(node.getLabel()).isEqualTo("foo");

      assertThat(node.getBody().getFields())
          .containsExactly(entry("dtype", "int32"), entry("shape", List.of(2, 3)));

      assertJsonEquals(node.getBody(), node.bodyAsJson());

      assertEquivalentJson(source, node.toJsonString());

      assertEquivalentJson(
          node.bodyAsJson(),
          """
              {
                "dtype": "int32",
                "shape": [2, 3]
              }
              """);
    }
  }
}
