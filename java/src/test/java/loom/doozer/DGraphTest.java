package loom.doozer;

import com.fasterxml.jackson.annotation.*;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.common.HasToJsonString;
import loom.common.serialization.JsonUtil;
import loom.testing.BaseTestClass;
import loom.zspace.ZPoint;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    public abstract static class Node<T> implements HasToJsonString {

        private final UUID id;
        private final String type;

        protected T body;

        @JsonIgnore
        public abstract Class<? extends T> getBodyClass();

        public String bodyAsJson() {
            return JsonUtil.toJson(getBody());
        }

        public Map<String, Object> bodyAsMap() {
            return JsonUtil.toMap(getBody());
        }

        public void setBodyFromJson(String json) {
            setBody(JsonUtil.fromJson(json, getBodyClass()));
        }
    }

    @Jacksonized
    @SuperBuilder
    public static class JsonNode extends Node<JsonNode.Body> {
        @Data
        public static class Body {
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
    }

    @Jacksonized
    @SuperBuilder
    public static final class TNode extends Node<TNode.Body> {
        @Data
        @Jacksonized
        @Builder
        public static class Body {
            private String dtype;
            private ZPoint shape;
        }

        @Override
        public Class<Body> getBodyClass() {
            return Body.class;
        }
    }

    @Test
    public void testNothing() {
        var source =
          """
          {
            "id": "00000000-0000-0000-0000-000000000000",
            "type": "TreeNode",
            "body": {
              "dtype": "int32",
              "shape": [2, 3]
            }
          }
          """;
        {
            var tensorNode = JsonUtil.fromJson(source, TNode.class);
            assertThat(tensorNode.getBody().getShape()).isEqualTo(ZPoint.of(2, 3));
            assertThat(tensorNode.getBody().getDtype()).isEqualTo("int32");

            assertJsonEquals(tensorNode.getBody(), tensorNode.bodyAsJson());

            assertEquivalentJson(source, tensorNode.toJsonString());
        }

        {
            var treeNode = JsonUtil.fromJson(source, JsonNode.class);
            assertThat(treeNode.getBody().getFields()).containsExactly(
                    entry("dtype", "int32"),
                    entry("shape", List.of(2, 3))
            );

            assertJsonEquals(treeNode.getBody(), treeNode.bodyAsJson());

            assertEquivalentJson(source, treeNode.toJsonString());
        }

    }
}