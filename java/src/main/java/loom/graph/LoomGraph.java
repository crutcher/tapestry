package loom.graph;

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
import loom.common.json.JsonPathUtils;
import loom.common.serialization.JsonUtil;
import loom.common.serialization.MapValueListUtil;
import loom.graph.nodes.GenericNodeMetaFactory;
import loom.validation.ValidationIssue;

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
public final class LoomGraph implements HasToJsonString {

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

    @JsonIgnore private NodeMeta<NodeType, BodyType> meta;
    @JsonIgnore @Nullable private LoomGraph graph;

    @Nonnull private final UUID id;
    @Nonnull private final String type;
    @Nullable private String label;
    @Nonnull private BodyType body;

    @JsonIgnore
    public String getJsonPath() {
      return "$.nodes[@.id=='%s']".formatted(getId());
    }

    /**
     * Get the graph that this node belongs to.
     *
     * @return the graph.
     */
    public LoomGraph assertGraph() {
      if (graph == null) {
        throw new IllegalStateException("Node does not belong to a graph: " + id);
      }
      return graph;
    }

    /**
     * Create a deep copy of this node.
     *
     * @return the copy.
     */
    public final NodeType deepCopy() {
      return getMeta().nodeFromTree(this);
    }

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
      return JsonUtil.valueToJsonNodeTree(getBody());
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
    public final void setBodyFromValue(Object tree) {
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
       * {@code @JsonIgnore}, and we otherwise generate data fields for every getter in the body.
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
   * Metaclass to attach schema and validation to a node type.
   *
   * @param <NodeType> the node class.
   * @param <BodyType> the node body class.
   */
  @Data
  public abstract static class NodeMeta<NodeType extends Node<NodeType, BodyType>, BodyType> {
    @Nonnull private final Class<NodeType> nodeTypeClass;
    @Nonnull private final Class<BodyType> bodyTypeClass;
    @Nonnull private final String bodySchema;

    /**
     * Validates the provided node against the NodeMeta's schema. This method checks if the node's
     * body adheres to the schema defined in the NodeMeta. If the node's body does not adhere to the
     * schema, it throws a ValidationIssue.
     *
     * @param node The node to be validated.
     * @throws loom.validation.LoomValidationError If the node's body does not adhere to the schema.
     */
    public final void validate(NodeType node) {
      validateNode(node);
      var graph = node.assertGraph();
      var env = graph.getEnv();

      var bodySchema = getBodySchema();

      env.getJsonSchemaManager()
          .issueScan()
          .type(LoomConstants.Errors.NODE_SCHEMA_ERROR)
          .param("nodeType", node.getType())
          .summaryPrefix("Body ")
          .jsonPathPrefix(JsonPathUtils.concatJsonPath(node.getJsonPath() + ".body"))
          .schemaSource(bodySchema)
          .json(node.getBodyAsJson())
          .context(
              ValidationIssue.Context.builder()
                  .name("Node")
                  .jsonpath(node.getJsonPath())
                  .jsonData(node.toPrettyJsonString())
                  .build())
          .context(
              ValidationIssue.Context.builder().name("Body Schema").jsonData(bodySchema).build())
          .build()
          .scan()
          .check();
    }

    /**
     * Subclass-overridden type validator.
     *
     * @param node the node to validate.
     * @throws loom.validation.LoomValidationError if the node is invalid.
     */
    public void validateNode(NodeType node) {}

    /**
     * Adopt a node into this meta.
     *
     * @param node the node to adopt.
     * @return the node.
     */
    private NodeType adopt(NodeType node) {
      node.setMeta(this);
      return node;
    }

    /**
     * Create a new node with this meta.
     *
     * @param json the JSON string to parse.
     * @return the node.
     */
    public final NodeType nodeFromJson(String json) {
      return adopt(JsonUtil.fromJson(json, getNodeTypeClass()));
    }

    /**
     * Create a new node with this meta.
     *
     * @param tree the JSON tree to parse.
     * @return the node.
     */
    public final NodeType nodeFromTree(Object tree) {
      return adopt(JsonUtil.convertValue(tree, getNodeTypeClass()));
    }
  }

  /** Factory to lookup node meta by type. */
  public abstract static class NodeMetaFactory {
    /**
     * Get the node meta for the given type.
     *
     * @param type the node type.
     * @return the node meta, or null if not found.
     */
    public abstract NodeMeta<?, ?> getMetaForType(String type);

    /**
     * Parse a node from a JSON string, using the node type in the JSON.
     *
     * @param json the JSON string.
     * @return the node.
     */
    public final Node<?, ?> nodeFromJson(String json) {
      return nodeFromTree(JsonUtil.parseToJsonNodeTree(json));
    }

    /**
     * Parse a node from a JSON tree, using the node type in the JSON.
     *
     * @param tree the JSON tree.
     * @return the node.
     */
    public final Node<?, ?> nodeFromTree(JsonNode tree) {
      var type = tree.get("type").asText();
      var meta = getMetaForType(type);
      return meta.nodeFromTree(tree);
    }
  }

  public static final LoomEnvironment GENERIC_ENV =
      LoomEnvironment.builder().nodeMetaFactory(new GenericNodeMetaFactory()).build();

  @JsonIgnore @Builder.Default private final LoomEnvironment env = GENERIC_ENV;

  @Nullable private UUID id;

  @JsonSerialize(using = MapValueListUtil.MapSerializer.class)
  @JsonDeserialize(using = JacksonSupport.NodeListToMapDeserializer.class)
  private final Map<UUID, Node<?, ?>> nodes = new HashMap<>();

  /**
   * Validate the graph.
   *
   * <p>Validates the graph against the environment.
   *
   * @throws loom.validation.LoomValidationError if the graph is invalid.
   */
  public void validate() {
    env.validateGraph(this);
  }

  public LoomGraph deepCopy() {
    var graph = LoomGraph.builder().env(env).id(id).build();

    for (var node : nodes.values()) {
      graph.addNode(node.deepCopy());
    }

    return graph;
  }

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
  public Node<?, ?> assertNode(UUID id) {
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
  public Node<?, ?> assertNode(String id) {
    return assertNode(UUID.fromString(id));
  }

  /**
   * Get the node with the given ID.
   *
   * @param id the ID of the node to get.
   * @param type the type of the node to get; null to skip type check.
   * @param nodeClass the class of the node to get.
   * @return the cast node.
   * @param <T> the type of the node to get.
   * @throws LookupError if the node does not exist, or is not of the given type.
   */
  public <T extends Node<?, ?>> T assertNode(UUID id, @Nullable String type, Class<T> nodeClass) {
    var node = assertNode(id);
    if (type != null && !node.getType().equals(type)) {
      throw new LookupError("Node is not of type " + type + ": " + id);
    }
    if (!nodeClass.isInstance(node)) {
      throw new LookupError("Node is not of type " + nodeClass.getSimpleName() + ": " + id);
    }
    return nodeClass.cast(node);
  }

  /**
   * Get the node with the given ID.
   *
   * @param id the ID of the node to get.
   * @param type the type of the node to get; null to skip type check.
   * @param nodeClass the class of the node to get.
   * @return the cast node.
   * @param <T> the type of the node to get.
   * @throws LookupError if the node does not exist, or is not of the given type.
   */
  public <T extends Node<?, ?>> T assertNode(String id, String type, Class<T> nodeClass) {
    return assertNode(UUID.fromString(id), type, nodeClass);
  }

  /**
   * Add a node to the graph.
   *
   * @param node the node to add.
   * @return the added Node.
   */
  @SuppressWarnings("unchecked")
  public <NodeType extends Node<NodeType, BodyType>, BodyType> NodeType addNode(
      Node<NodeType, BodyType> node) {
    if (hasNode(node.getId())) {
      throw new IllegalArgumentException("Node already exists: " + node.getId());
    }

    if (node.getGraph() != null) {
      throw new IllegalArgumentException("Node already belongs to a graph: " + node.getId());
    }
    node.setGraph(this);

    if (node.getMeta() == null) {
      try {
        node.setMeta(
            (NodeMeta<NodeType, BodyType>) env.getNodeMetaFactory().getMetaForType(node.getType()));
      } catch (Exception e) {
        throw new IllegalArgumentException("Unknown node type: " + node.getType());
      }
    }

    nodes.put(node.getId(), node);

    return (NodeType) node;
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

    /** Jackson deserializer for {@link LoomGraph#nodes}. */
    public static class NodeListToMapDeserializer
        extends MapValueListUtil.MapDeserializer<UUID, Node<?, ?>> {
      @SuppressWarnings("unchecked")
      public NodeListToMapDeserializer() {
        super((Class<Node<?, ?>>) (Class<?>) Node.class, Node::getId, HashMap.class);
      }
    }
  }
}
