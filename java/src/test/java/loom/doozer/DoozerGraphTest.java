package loom.doozer;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.experimental.Delegate;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.common.HasToJsonString;
import loom.common.LookupError;
import loom.common.serialization.JsonUtil;
import loom.common.serialization.MapValueListUtil;
import loom.testing.BaseTestClass;
import loom.zspace.ZPoint;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.Validator;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("SameNameButDifferent")
public class DoozerGraphTest extends BaseTestClass {


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
  //    *. A validator which checks that the data field is valid against the JSD schema
  //       and the type specific semantic rules.
  //    *. A validator which checks that the node is valid in context of the full graph.
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
  @Builder
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static final class DoozerGraph {
    @Data
    @Builder
    public static final class Environment {
      private final Node.NodeMetaFactory nodeMetaFactory;

      public DoozerGraph graphFromJson(String json) {
        var tree = JsonUtil.readTree(json);

        var graph = DoozerGraph.builder().env(this).build();

        for (var entry : tree.properties()) {
          var key = entry.getKey();
          if (key.equals("id")) {
            graph.setId(UUID.fromString(entry.getValue().asText()));

          } else if (key.equals("nodes")) {
            for (var nodeTree : entry.getValue()) {
              var node = getNodeMetaFactory().nodeFromTree(nodeTree);
              graph.addNode(node);
            }
          } else {
            throw new IllegalArgumentException("Unknown property: " + key);
          }
        }

        return graph;
      }
    }


    @JsonIgnore
    @Builder.Default
    private final Environment env = new Environment(new GenericNodeMetaFactory());

    @Nullable private UUID id;

    @JsonSerialize(using = MapValueListUtil.MapSerializer.class)
    @JsonDeserialize(using = JacksonSupport.NodeListToMapDeserializer.class)
    private final Map<UUID, Node> nodes = new HashMap<>();

    /**
     * Create a new, unused node ID.
     *
     * @return the new ID.
     */
    public UUID newUnusedNodeId() {
      UUID id;
      do {
        id = UUID.randomUUID();
      } while (hasNode(id));
      return id;
    }

    /**
     * Does this graph contain a node with the given ID?
     *
     * @param id the ID to check.
     * @return true if the graph contains a node with the given ID.
     */
    public boolean hasNode(UUID id) {
      return nodes.containsKey(id);
    }

    /**
     * Does this graph contain a node with the given ID?
     *
     * @param id the ID to check.
     * @return true if the graph contains a node with the given ID.
     */
    public boolean hasNode(String id) {
      return hasNode(UUID.fromString(id));
    }

    /**
     * Get the node with the given ID.
     *
     * @param id the ID of the node to get.
     * @return the node.
     * @throws LookupError if the node does not exist.
     */
    public Node assertNode(UUID id) {
      var node = nodes.get(id);
      if (node == null) {
        throw new LookupError("Node not found: " + id);
      }
      return node;
    }

    /**
     * Get the node with the given ID.
     *
     * @param id the ID of the node to get.
     * @return the node.
     * @throws LookupError if the node does not exist.
     */
    public Node assertNode(String id) {
      return assertNode(UUID.fromString(id)); }

    /**
     * Add a node to the graph.
     *
     * @param node the node to add.
     * @return the ID of the node.
     */
    public UUID addNode(Node node) {
      if (hasNode(node.getId())) {
        throw new IllegalArgumentException("Node already exists: " + node.getId());
      }
      nodes.put(node.getId(), node);
      return node.getId();
    }

    /**
     * Add a node to the graph.
     *
     * <p>If the node builder does not have an ID, a new ID will be generated.
     *
     * @param builder a builder for the node to add.
     * @return the ID of the node.
     */
    public UUID addNode(Node.NodeBuilder builder) {
      if (builder.id == null) {
        builder.id = newUnusedNodeId();
      }

      var node = builder.build();
      return addNode(node);
    }

    /** Support classes for Jackson serialization. */
    public static class JacksonSupport {
      private JacksonSupport() {}

      /** Jackson deserializer for {@link DoozerGraph#nodes}. */
      public static class NodeListToMapDeserializer
              extends MapValueListUtil.MapDeserializer<UUID, Node> {
        public NodeListToMapDeserializer() {
          super(Node.class, Node::getId, HashMap.class);
        }
      }
    }
  }


  public static final class GenericNodeMetaFactory extends Node.NodeMetaFactory {
    @Override
    public Node.NodeMeta<?, ?> getMeta(String type) {
      return new GenericNode.Meta();
    }
  }

  @Data
  @Builder
  public static final class TypeMapNodeMetaFactory extends Node.NodeMetaFactory {
    @Singular
    private final Map<String, Node.NodeMeta<?, ?>> typeMappings;

    @Override
    public Node.NodeMeta<?, ?> getMeta(String type) {
      return typeMappings.get(type);
    }
  }

  @Data
  @SuperBuilder
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonSerialize(using = Node.NodeSerializer.class)
  public abstract static class Node<NodeType extends Node<NodeType, BodyType>, BodyType> implements HasToJsonString {
    /**
     * A Jackson serializer for Node.
     * We use a custom serializer because {@code @Delegate} applied to a method in subclasses
     * to delegate the type methods of {@code body} does not honor [@code @JsonIgnore}, and
     * we otherwise generate data fields for every getter in the body.
     *
     * @param <B> the type of the node body.
     */
    public static final class NodeSerializer<N extends Node<N, B>, B> extends JsonSerializer<Node<N, B>> {
      @Override
      public void serialize(Node<N, B> value, JsonGenerator gen, SerializerProvider serializers)
              throws IOException {
        gen.writeStartObject();
        gen.writeStringField("id", value.getId().toString());
        gen.writeStringField("type", value.getType());

        var label = value.getLabel();
        if (label != null) {
          gen.writeStringField("label", label);
        }

        gen.writeObjectField("body", value.getBody());
        gen.writeEndObject();
      }
    }

    @Data
    public static abstract class NodeMeta<NodeType extends Node<NodeType, BodyType>, BodyType> {
      private final Class<NodeType> nodeTypeClass;
      private final Class<BodyType> bodyTypeClass;
      private final String bodySchema;

      public final void validate(Node<NodeType, BodyType> node) {
        validateBody(node.getBody());
        validateBodySchema(node.bodyAsJson());
      }

      public void validateBody(BodyType body) {}

      public final void validateBodySchema(String json) {
        SchemaStore schemaStore = new SchemaStore();
        try {
          var schema = schemaStore.loadSchemaJson(getBodySchema());
          var validator = new Validator();
          validator.validateJson(schema, json);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }

      public final NodeType nodeFromJson(String json) {
        var node = JsonUtil.fromJson(json, getNodeTypeClass());
        node.setMeta(this);
        node.validate();
        return node;
      }

      public final NodeType nodeFromTree(Object tree) {
        var node = JsonUtil.convertValue(tree, getNodeTypeClass());
        node.setMeta(this);
        node.validate();
        return node;
      }
    }

    public static abstract class NodeMetaFactory {
      public abstract Node.NodeMeta<?, ?> getMeta(String type);

      public final Node<?, ?> nodeFromJson(String json) {
        return nodeFromTree(JsonUtil.readTree(json));
      }

      public final Node<?, ?> nodeFromTree(JsonNode tree) {
        var type = tree.get("type").asText();
        var meta = getMeta(type);
        return meta.nodeFromTree(tree);
      }
    }

    @JsonIgnore
    @Nullable
    private NodeMeta<NodeType, BodyType> meta;

    @Nonnull
    private final UUID id;
    @Nonnull
    private final String type;
    @Nullable
    private String label;

    @Nonnull
    private BodyType body;

    // TODO: collect body class, schema, and validation into a validator class.
    // This is to support evolving the schema and validation.

    @JsonIgnore
    @SuppressWarnings("unchecked")
    public Class<BodyType> getBodyClass() {
        return (Class<BodyType>) body.getClass();
    }

    public final String bodyAsJson() {
      return JsonUtil.toPrettyJson(getBody());
    }

    public final Map<String, Object> bodyAsMap() {
      return JsonUtil.toMap(getBody());
    }

    public final void setBodyFromJson(String json) {
      setBody(JsonUtil.fromJson(json, getBodyClass()));
    }

    public final void validate() {
      getMeta().validate(this);
    }
  }

  @Jacksonized
  @SuperBuilder
  public static class GenericNode extends Node<GenericNode, GenericNode.Body> {
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

    public static final class Meta extends NodeMeta<GenericNode, Body> {
      public static final String BODY_SCHEMA = """
              {
                "type": "object",
                "patternProperties": {
                  "^[a-zA-Z_][a-zA-Z0-9_]*$": {}
                }
              }
              """;

      public Meta() {
        super(GenericNode.class, Body.class, BODY_SCHEMA);
      }
    };

    /**
     * Exists to support {@code @Delegate} for {@code getBody()}.
     */
    @Delegate
    private Body delegateProvider() {
      return getBody();
    }
  }

  @Jacksonized
  @SuperBuilder
  public static final class TNode extends Node<TNode, TNode.Body> {
    @Data
    @Jacksonized
    @Builder
    public static class Body {
      @Nonnull
      private String dtype;

      @Nonnull
      private ZPoint shape;
    }

    public static class Meta extends NodeMeta<TNode, TNode.Body> {
      public static final String TYPE = "TreeNode";

      public static final String BODY_SCHEMA = """
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

      public Meta() {
        super(TNode.class, Body.class, BODY_SCHEMA);
      }

      @Override
      public void validateBody(TNode.Body body) {
        if (!body.getShape().coords.isStrictlyPositive()) {
          throw new IllegalArgumentException("shape must be positive and non-empty: " + body.getShape());
        }
      }
    }

    /**
     * Exists to support {@code @Delegate} for {@code getBody()}.
     */
    @Delegate
    private Body delegateProvider() {
      return getBody();
    }
  }


  @Test
  public void testGraph() {
    var source =
        """
        {
          "id": "00000000-0000-4000-8000-00000000000A",
          "nodes": [
            {
              "id": "00000000-0000-0000-0000-000000000000",
              "type": "TreeNode",
              "label": "foo",
              "body": {
                "dtype": "int32",
                "shape": [2, 3]
              }
            }
          ]
         }
        """;

    var env = DoozerGraph.Environment.builder()
            .nodeMetaFactory(TypeMapNodeMetaFactory.builder()
                    .typeMapping(TNode.Meta.TYPE, new TNode.Meta())
                    .build())
            .build();

    var graph = env.graphFromJson(source);

    var node = (TNode) graph.assertNode("00000000-0000-0000-0000-000000000000");
    assertThat(node.getDtype()).isEqualTo("int32");
  }

  @Test
  public void testNothing() {
    var source =
        """
          {
            "id": "00000000-0000-0000-0000-000000000000",
            "type": "TreeNode",
            "label": "foo",
            "body": {
              "dtype": "int32",
              "shape": [2, 3]
            }
          }
          """;
    {
      var factory = TypeMapNodeMetaFactory.builder()
              .typeMapping(TNode.Meta.TYPE, new TNode.Meta())
              .build();

      var node = (TNode) factory.nodeFromJson(source);

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
      var factory = new GenericNodeMetaFactory();

      var node = (GenericNode) factory.nodeFromJson(source);

      assertThat(node.getId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000000"));
      assertThat(node.getLabel()).isEqualTo("foo");

      assertThat(node.getFields())
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
