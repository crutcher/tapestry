package loom.graph;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.*;
import loom.common.json.HasToJsonString;
import loom.common.json.JsonUtil;
import loom.common.json.MapValueListUtil;
import loom.validation.ListValidationIssueCollector;
import loom.validation.ValidationIssueCollector;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Stream;

/** A Loom Graph document. */
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class LoomGraph implements HasToJsonString {

  @JsonIgnore @Nonnull private final LoomEnvironment env;

  @ToString.Include @Nullable private UUID id;

  @ToString.Include
  @JsonProperty(value = "nodes")
  @JsonSerialize(using = MapValueListUtil.MapSerializer.class)
  @JsonDeserialize(using = Serialization.NodeListToMapDeserializer.class)
  private final Map<UUID, LoomNode<?, ?>> nodeMap = new HashMap<>();

  @JsonIgnore private final Map<String, List<LoomNode<?, ?>>> nodeTypeMap = new HashMap<>();
  @JsonIgnore private final Map<Class<?>, List<LoomNode<?, ?>>> nodeClassMap = new HashMap<>();

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

  /**
   * Builder for a LoomNode scan.
   *
   * @param <T> the type of the node to scan for.
   */
  public static final class NodeScanBuilder<T extends LoomNode<?, ?>> {
    @Nonnull private final LoomGraph graph;
    @Nullable private String type;
    @Nullable private Class<T> nodeClass;

    private NodeScanBuilder(@Nonnull LoomGraph graph) {
      this.graph = graph;
    }

    /**
     * Set the type of the node to scan for.
     *
     * @param type the type.
     * @return the builder.
     */
    public NodeScanBuilder<T> type(@Nullable String type) {
      this.type = type;
      return this;
    }

    /**
     * Set the class of the node to scan for.
     *
     * @param nodeClass the class.
     * @return the builder, cast to the correct type.
     * @param <X> the type of the node to scan for.
     */
    public <X extends LoomNode<?, ?>> NodeScanBuilder<X> nodeClass(@Nullable Class<X> nodeClass) {
      @SuppressWarnings("unchecked")
      var thisAs = (NodeScanBuilder<X>) this;
      thisAs.nodeClass = nodeClass;
      return thisAs;
    }

    /**
     * Get the nodes matching the scan as a stream.
     *
     * @return the stream.
     */
    public Stream<T> asStream() {
      enum NodeSource {
        ALL,
        TYPE_FILTERED,
        CLASS_FILTERED
      }

      NodeSource nodeSource = NodeSource.ALL;
      Collection<LoomNode<?, ?>> nodes = graph.getNodeMap().values();

      if (type != null) {
        var source = graph.nodeTypeMap.get(type);
        if (source != null && source.size() < nodes.size()) {
          nodeSource = NodeSource.TYPE_FILTERED;
          nodes = source;
        }
      }
      if (nodeClass != null) {
        var source = graph.nodeClassMap.get(nodeClass);
        if (source != null && source.size() < nodes.size()) {
          nodeSource = NodeSource.CLASS_FILTERED;
          nodes = source;
        }
      }

      Stream<LoomNode<?, ?>> baseStream = nodes.stream();

      if (type != null && nodeSource != NodeSource.TYPE_FILTERED) {
        baseStream = baseStream.filter(node -> node.getType().equals(type));
      }
      @SuppressWarnings("unchecked")
      var typedStream =
          (nodeClass == null || nodeSource == NodeSource.CLASS_FILTERED)
              ? (Stream<T>) baseStream
              : baseStream.filter(nodeClass::isInstance).map(nodeClass::cast);

      return typedStream;
    }

    /**
     * Get the nodes matching the scan as a list.
     *
     * @return the list.
     */
    public List<T> asList() {
      return asStream().toList();
    }
  }

  /**
   * Create a new NodeScanBuilder.
   *
   * @return the builder.
   */
  public NodeScanBuilder<LoomNode<?, ?>> nodeScan() {
    return new NodeScanBuilder<>(this);
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
   * Remove a node from the graph. Silently does nothing if the node does not exist.
   *
   * @param id the ID of the node to remove.
   */
  public void removeNode(UUID id) {
    var node = nodeMap.get(id);
    if (node == null) {
      return;
    }
    nodeMap.remove(id);
    nodeTypeMap.get(node.getType()).remove(node);
    nodeClassMap.get(node.getClass()).remove(node);
  }

  /**
   * Remove a node from the graph. Silently does nothing if the node does not exist.
   *
   * @param id the ID of the node to remove.
   */
  public void removeNode(String id) {
    removeNode(UUID.fromString(id));
  }

  /**
   * Remove a node from the graph. Silently does nothing if the node does not exist.
   *
   * @param node the node to remove.
   */
  public void removeNode(LoomNode<?, ?> node) {
    removeNode(node.getId());
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
    nodeTypeMap.computeIfAbsent(node.getType(), k -> new ArrayList<>()).add(node);
    nodeClassMap.computeIfAbsent(node.getClass(), k -> new ArrayList<>()).add(node);

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
