package loom.doozer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import loom.common.HasToJsonString;
import loom.common.LookupError;
import loom.common.serialization.JsonUtil;
import loom.common.serialization.MapValueListUtil;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.Validator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** A Loom Graph document. */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class DoozerGraph implements HasToJsonString {

  /**
   * Base class for a node in the graph.
   *
   * @param <NodeType> the node subclass.
   * @param <BodyType> the node body class.
   */
  @Data
  @ToString(of = {"id", "type", "label", "body"})
  @SuperBuilder
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonSerialize(using = Node.JsonSupport.NodeSerializer.class)
  public abstract static class Node<NodeType extends Node<NodeType, BodyType>, BodyType>
      implements HasToJsonString {

    @JsonIgnore @Nullable private DoozerGraph graph;
    @JsonIgnore @Nullable private NodeMeta<NodeType, BodyType> meta;

    @Nonnull private final UUID id;
    @Nonnull private final String type;
    @Nullable private String label;
    @Nonnull private BodyType body;

    /** Get the class type of the node body. */
    @JsonIgnore
    @SuppressWarnings("unchecked")
    public Class<BodyType> getBodyClass() {
      return (Class<BodyType>) body.getClass();
    }

    /**
     * Get the node body as a JSON string.
     *
     * @return the JSON string.
     */
    public final String getBodyAsJson() {
      return JsonUtil.toPrettyJson(getBody());
    }

    /**
     * Get the node body as a JSON tree.
     *
     * @return the JSON tree.
     */
    public final JsonNode getBodyAsJsonNode() {
      return JsonUtil.toTree(getBody());
    }

    /** Get the node body as an Object map. */
    public final Map<String, Object> getBodyAsObjectMap() {
      return JsonUtil.toMap(getBody());
    }

    /**
     * Set the node body from a JSON string.
     *
     * @param json the JSON string.
     */
    public final void setBodyFromJson(String json) {
      setBody(JsonUtil.fromJson(json, getBodyClass()));
    }

    /**
     * Set the node body from a JSON tree.
     *
     * @param tree the JSON tree; either a {@code JsonNode} or a {@code Map<String, Object>}.
     */
    public final void setBodyFromTree(Object tree) {
      setBody(JsonUtil.convertValue(tree, getBodyClass()));
    }

    /**
     * Subclass type helper.
     *
     * @return this, cast to the subclass {@code NodeType} type.
     */
    @SuppressWarnings("unchecked")
    public final NodeType self() {
      return (NodeType) this;
    }

    /** Validate the node against the NodeMeta. */
    public final void validate() {
      getMeta().validate(self());
    }

    public static class JsonSupport {
      private JsonSupport() {}

      /**
       * A Jackson serializer for Node. We use a custom serializer because {@code @Delegate} applied
       * to a method in subclasses to delegate the type methods of {@code body} does not honor
       * [@code @JsonIgnore}, and we otherwise generate data fields for every getter in the body.
       *
       * @param <B> the type of the node body.
       */
      public static final class NodeSerializer<N extends Node<N, B>, B>
          extends JsonSerializer<Node<N, B>> {
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
    }
  }

  /**
   * Meta class to attach schema and validation to a node type.
   *
   * @param <NodeType> the node class.
   * @param <BodyType> the node body class.
   */
  @Data
  public abstract static class NodeMeta<NodeType extends Node<NodeType, BodyType>, BodyType> {
    private final Class<NodeType> nodeTypeClass;
    private final Class<BodyType> bodyTypeClass;
    private final String bodySchema;

    public final void validate(NodeType node) {
      validateNode(node);
      validateBodySchema(node.getBodyAsJson());
    }

    public void validateNode(NodeType node) {}

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

  /** Factory to lookup node meta by type. */
  public abstract static class NodeMetaFactory {
    public abstract NodeMeta<?, ?> getMeta(String type);

    public final Node<?, ?> nodeFromJson(String json) {
      return nodeFromTree(JsonUtil.readTree(json));
    }

    public final Node<?, ?> nodeFromTree(JsonNode tree) {
      var type = tree.get("type").asText();
      var meta = getMeta(type);
      return meta.nodeFromTree(tree);
    }
  }

  public static final DoozerEnvironment GENERIC_ENV =
      DoozerEnvironment.builder().nodeMetaFactory(new GenericNodeMetaFactory()).build();

  @JsonIgnore @Builder.Default private final DoozerEnvironment env = GENERIC_ENV;

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
    return assertNode(UUID.fromString(id));
  }

  /**
   * Add a node to the graph.
   *
   * @param node the node to add.
   * @return the added Node.
   */
  @SuppressWarnings("unchecked")
  public <N extends Node> N addNode(N node) {
    if (hasNode(node.getId())) {
      throw new IllegalArgumentException("Node already exists: " + node.getId());
    }

    if (node.getGraph() != null) {
      throw new IllegalArgumentException("Node already belongs to a graph: " + node.getId());
    }
    node.setGraph(this);

    if (node.getMeta() == null) {
      try {
        node.setMeta(env.getNodeMetaFactory().getMeta(node.getType()));
      } catch (Exception e) {
        throw new IllegalArgumentException("Unknown node type: " + node.getType());
      }
    }

    nodes.put(node.getId(), node);

    return node;
  }

  /**
   * Add a node to the graph.
   *
   * <p>If the node builder does not have an ID, a new ID will be generated.
   *
   * @param builder a builder for the node to add.
   * @return the ID of the node.
   */
  public <N extends Node<N, B>, B> N addNode(Node.NodeBuilder<N, B, ?, ?> builder) {
    if (builder.id == null) {
      builder.id = newUnusedNodeId();
    }

    @SuppressWarnings("unchecked")
    var node = (N) builder.build();

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
