package loom.graph;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.*;
import loom.common.collections.IteratorUtils;
import loom.common.json.HasToJsonString;
import loom.common.json.JsonUtil;
import loom.common.json.MapValueListUtil;
import loom.validation.ListValidationIssueCollector;
import loom.validation.ValidationIssueCollector;

/** A Loom Graph document. */
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class LoomGraph implements Iterable<LoomNode<?, ?>>, HasToJsonString {

  @JsonIgnore @Nonnull private final LoomEnvironment env;

  @ToString.Include @Nullable private UUID id;

  @ToString.Include
  @JsonProperty(value = "nodes")
  @JsonSerialize(using = MapValueListUtil.MapSerializer.class)
  @JsonDeserialize(using = Serialization.NodeListToMapDeserializer.class)
  private final Map<UUID, LoomNode<?, ?>> nodeMap = new HashMap<>();

  /**
   * Validate the graph.
   *
   * <p>Validates the graph against the environment.
   *
   * @throws loom.validation.LoomValidationError if the graph is invalid.
   */
  public void validate() {
    var collector = new ListValidationIssueCollector();
    validate(collector);
    collector.check();
  }

  /**
   * Validate the graph.
   *
   * <p>Validates the graph against the environment.
   *
   * @param issueCollector collector to collect validation errors into.
   */
  public void validate(ValidationIssueCollector issueCollector) {
    env.validateGraph(this, issueCollector);
  }

  /**
   * Does this graph contain a node with the given ID?
   *
   * @param id the ID to check.
   * @return true if the graph contains a node with the given ID.
   */
  public boolean hasNode(UUID id) {
    return nodeMap.containsKey(id);
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
   * @return the node, or null if not found.
   */
  @Nullable public LoomNode<?, ?> getNode(UUID id) {
    return nodeMap.get(id);
  }

  /**
   * Get the node with the given ID.
   *
   * @param id the ID of the node to get.
   * @return the node, or null if not found.
   */
  @Nullable public LoomNode<?, ?> getNode(String id) {
    return getNode(UUID.fromString(id));
  }

  /**
   * Get the node with the given ID.
   *
   * @param id the ID of the node to get.
   * @return the node.
   * @throws IllegalStateException if the node does not exist.
   */
  @Nonnull
  public LoomNode<?, ?> assertNode(String id) {
    return assertNode(UUID.fromString(id));
  }

  /**
   * Get the node with the given ID.
   *
   * @param id the ID of the node to get.
   * @return the node.
   * @throws IllegalStateException if the node does not exist.
   */
  @Nonnull
  public LoomNode<?, ?> assertNode(UUID id) {
    var node = nodeMap.get(id);
    if (node == null) {
      throw new IllegalStateException("Node not found: " + id);
    }
    return node;
  }

  /**
   * Get the node with the given ID.
   *
   * @param id the ID of the node to get.
   * @param type the type of the node to get; null to skip type check.
   * @param nodeClass the class of the node to get.
   * @return the cast node.
   * @param <T> the type of the node to get.
   * @throws IllegalStateException if the node does not exist, or is not of the given type.
   */
  @Nonnull
  public <T extends LoomNode<?, ?>> T assertNode(String id, String type, Class<T> nodeClass) {
    return assertNode(UUID.fromString(id), type, nodeClass);
  }

  /**
   * Get the node with the given ID.
   *
   * @param id the ID of the node to get.
   * @param type the type of the node to get; null to skip type check.
   * @param nodeClass the class of the node to get.
   * @return the cast node.
   * @param <T> the type of the node to get.
   * @throws IllegalStateException if the node does not exist, or is not of the given type.
   */
  @Nonnull
  public <T extends LoomNode<?, ?>> T assertNode(
      UUID id, @Nullable String type, Class<T> nodeClass) {
    var node = assertNode(id);
    if (type != null && !node.getType().equals(type)) {
      throw new IllegalStateException("Node is not of type " + type + ": " + id);
    }
    if (!nodeClass.isInstance(node)) {
      throw new IllegalStateException(
          "Node is not of type " + nodeClass.getSimpleName() + ": " + id);
    }
    return nodeClass.cast(node);
  }

  @Override
  @Nonnull
  public Iterator<LoomNode<?, ?>> iterator() {
    return iterableNodes().iterator();
  }

  /**
   * Get an iterable of all nodes in the graph.
   *
   * @return the iterable.
   */
  @Nonnull
  public Iterable<LoomNode<?, ?>> iterableNodes() {
    return nodeMap.values();
  }

  /**
   * Get an iterable of all nodes of class {@code T} the graph, optionally filtered by type.
   *
   * @param type the type to filter by; null to skip type filter.
   * @param nodeClass the class of the nodes to get.
   * @return the iterable.
   * @param <NodeType> the type of the nodes to get.
   */
  @Nonnull
  public <NodeType extends LoomNode<?, ?>> Iterable<NodeType> iterableNodes(
      @Nullable String type, Class<NodeType> nodeClass) {
    return IteratorUtils.supplierToIterable(() -> stream(type, nodeClass).iterator());
  }

  /**
   * Get a stream of all nodes of class {@code T} the graph, optionally filtered by type.
   *
   * @param type the type to filter by; null to skip type filter.
   * @param nodeClass the class of the nodes to get.
   * @return the stream.
   * @param <NodeType> the type of the nodes to get.
   */
  @CheckReturnValue
  @Nonnull
  public <NodeType extends LoomNode<?, ?>> Stream<NodeType> stream(
      @Nullable String type, Class<NodeType> nodeClass) {
    var s = stream();
    if (type != null) {
      s = s.filter(node -> node.getType().equals(type));
    }

    return s.filter(nodeClass::isInstance).map(nodeClass::cast);
  }

  /**
   * Get a stream of all nodes in the graph.
   *
   * @return the stream.
   */
  @CheckReturnValue
  @Nonnull
  public Stream<? extends LoomNode<?, ?>> stream() {
    return nodeMap.values().stream();
  }

  /**
   * Create a new, unused node ID.
   *
   * <p>Generates a new UUID, and checks that it is not already in use in the graph.
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
   * Add a node to the graph.
   *
   * <p>If the node builder does not have an ID, a new ID will be generated.
   *
   * @param builder a builder for the node to add.
   * @return the ID of the node.
   */
  @Nonnull
  public <N extends LoomNode<N, B>, B> N addNode(LoomNode.LoomNodeBuilder<N, B, ?, ?> builder) {
    if (!builder.hasId()) {
      builder.id(newUnusedNodeId());
    }

    @SuppressWarnings("unchecked")
    var node = (N) builder.build();

    return addNode(node);
  }

  /**
   * Add a node to the graph.
   *
   * @param node the node to add.
   * @return the added Node.
   */
  @Nonnull
  @SuppressWarnings("unchecked")
  public <T extends LoomNode<?, ?>> T addNode(T node) {
    if (hasNode(node.getId())) {
      throw new IllegalArgumentException("Graph already has node with id: " + node.getId());
    }

    if (node.getGraph() != null) {
      throw new IllegalArgumentException("Node already belongs to a graph: " + node.getId());
    }
    node.setGraph(this);

    env.assertClassForType(node.getType(), (Class<T>) node.getClass());

    nodeMap.put(node.getId(), node);

    return node;
  }

  /**
   * Add a node to the graph.
   *
   * <p>If the node does not have an ID, a new ID will be added to the tree.
   *
   * @param jsonNode the json node tree to build from.
   * @return the added Node.
   */
  @Nonnull
  public LoomNode<?, ?> addNode(@Nonnull JsonNode jsonNode) {
    ObjectNode obj = (ObjectNode) jsonNode;
    String type = obj.get("type").asText();
    var nodeClass = env.assertClassForType(type);
    if (obj.get("id") == null) {
      obj.put("id", newUnusedNodeId().toString());
    }
    var node = JsonUtil.convertValue(obj, nodeClass);
    return addNode(node);
  }

  /** Create a new NodeBuilder. */
  public NodeBuilder nodeBuilder() {
    return new NodeBuilder();
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public final class NodeBuilder {
    private String type;
    private String label;
    private JsonNode body;

    /**
     * Set the type of the node.
     *
     * @param type the type.
     * @return the builder.
     */
    public NodeBuilder type(String type) {
      this.type = type;
      return this;
    }

    /**
     * Set the label of the node.
     *
     * @param label the label.
     * @return the builder.
     */
    public NodeBuilder label(@Nullable String label) {
      this.label = label;
      return this;
    }

    /**
     * Set the body of the node.
     *
     * <p>If the {@code body} is a string, it will be parsed as JSON to a JsonNode, and then
     * converted. If the {@code body} is a JsonNode, or other data structure, it will be converted
     * to the appropriate node class.
     *
     * @param body the body.
     * @return the builder.
     */
    public NodeBuilder body(Object body) {
      if (body instanceof String str) {
        body = JsonUtil.parseToJsonNodeTree(str);
      }
      if (!(body instanceof JsonNode)) {
        body = JsonUtil.valueToJsonNodeTree(body);
      }
      this.body = ((JsonNode) body).deepCopy();
      return this;
    }

    /**
     * Build the node on the graph.
     *
     * @return the new node.
     */
    public LoomNode<?, ?> build() {
      var nodeTree = JsonNodeFactory.instance.objectNode();
      nodeTree.put("type", type);
      if (label != null) {
        nodeTree.put("label", label);
      }
      nodeTree.set("body", body);
      return addNode(nodeTree);
    }
  }

  /** Support classes for Jackson serialization. */
  @NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
  public static final class Serialization {
    /** Jackson deserializer for {@link LoomGraph#nodeMap}. */
    public static final class NodeListToMapDeserializer
        extends MapValueListUtil.MapDeserializer<UUID, LoomNode<?, ?>> {
      @SuppressWarnings("unchecked")
      public NodeListToMapDeserializer() {
        super((Class<LoomNode<?, ?>>) (Class<?>) LoomNode.class, LoomNode::getId, HashMap::new);
      }
    }
  }
}
