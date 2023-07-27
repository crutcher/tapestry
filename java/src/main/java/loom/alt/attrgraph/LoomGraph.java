package loom.alt.attrgraph;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import loom.common.HasToJsonString;
import loom.common.JsonUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

/** The Loom expression graph. */
@Data
public final class LoomGraph implements HasToJsonString {
  /** Represents a node in the graph. */
  @Value
  @Jacksonized
  @Builder(toBuilder = true)
  public static class Node implements HasToJsonString {
    /** Extensions to the auto-generated Jackson {@link Builder} for {@link Node}. */
    public static class NodeBuilder {
      /**
       * Add an attribute to the node.
       *
       * <p>Values which are not {@link JsonNode} will be converted to JSON; values which are {@link
       * JsonNode} will be deep-copied.
       *
       * @param name The namespaced name.
       * @param value The value.
       * @return The builder.
       */
      public NodeBuilder attr(NSName name, Object value) {
        if (this.attrs == null) {
          this.attrs = new java.util.HashMap<>();
        }
        JsonNode jvalue;
        if (value instanceof JsonNode node) {
          jvalue = node.deepCopy();
        } else {
          jvalue = JsonUtil.toTree(value);
        }
        this.attrs.put(name, jvalue);
        return this;
      }

      /**
       * Remove an attribute from the node.
       *
       * @param name The namespaced name.
       * @return The builder.
       */
      public NodeBuilder removeAttr(NSName name) {
        if (this.attrs != null) {
          this.attrs.remove(name);
        }
        return this;
      }

      /**
       * Add a map of attributes to the node.
       *
       * <p>This method is used implicitly by the generated toBuilder method.
       *
       * @param entries the map of attributes.
       * @return The builder.
       */
      public NodeBuilder attrs(@Nonnull Map<NSName, ? extends Object> entries) {
        entries.forEach(this::attr);
        return this;
      }
    }

    @Nonnull
    @JsonProperty(required = true)
    UUID id;

    @Nonnull
    @JsonProperty(required = true)
    NSName type;

    @Nonnull Map<NSName, JsonNode> attrs;

    @Builder
    public Node(@Nullable UUID id, @Nonnull NSName type, @Nullable Map<NSName, JsonNode> attrs) {
      if (id == null) {
        id = UUID.randomUUID();
      }
      this.id = id;
      this.type = type;
      if (attrs == null) {
        attrs = Map.of();
      }
      this.attrs = Map.copyOf(attrs);
    }

    @Override
    public String toJsonString() {
      return toPrettyJsonString();
    }

   @Override
   public String toString() {
     return toJsonString();
   }

    /**
     * Iterable view of the node's attributes.
     *
     * @return The attributes.
     */
    public Iterable<Map.Entry<NSName, JsonNode>> attrs() {
      return attrs.entrySet();
    }

    /**
     * Stream view of the node's attributes.
     *
     * @return The attributes.
     */
    public Stream<Map.Entry<NSName, JsonNode>> attrStream() {
      return attrs.entrySet().stream();
    }

    /**
     * Return the set of namespaces used by the node's attributes.
     *
     * @return The namespaces.
     */
    public Set<String> namespaces() {
      Set<String> namespaces = new HashSet<>();
      for (Map.Entry<NSName, JsonNode> entry : attrs()) {
        namespaces.add(entry.getKey().urn());
      }
      return namespaces;
    }

    /**
     * Create a copy of the node.
     *
     * <p>Attribute values are considered immutable, so they are not deep-copied.
     *
     * @return The copy.
     */
    public Node copy() {
      return toBuilder().build();
    }

    /**
     * Does the node have an attribute with the given name?
     *
     * @param name The namespaced name.
     * @return True if the attribute exists.
     */
    public boolean hasAttr(NSName name) {
      return attrs.containsKey(name);
    }

    /**
     * Get the value of an attribute parsed as the given class.
     *
     * @param name The namespaced name.
     * @param clazz The class to parse the attribute as.
     * @param <T> The type of the attribute.
     * @return The attribute value.
     * @throws NoSuchElementException if the attribute does not exist.
     */
    public <T> T getAttr(NSName name, Class<T> clazz) {
      return JsonUtil.fromJson(getAttrTree(name), clazz);
    }

    /**
     * Get the value of an attribute as a {@link JsonNode}.
     *
     * @param name The namespaced name.
     * @return The attribute value.
     * @throws NoSuchElementException if the attribute does not exist.
     */
    public JsonNode getAttrTree(NSName name) {
      var attr = attrs.get(name);
      if (attr == null) {
        throw new NoSuchElementException(name.toString());
      }
      return attr;
    }

    /**
     * Get the value of an attribute as a {@link String}.
     *
     * @param name The namespaced name.
     * @return The attribute value as JSON.
     * @throws NoSuchElementException if the attribute does not exist.
     */
    public String getAttrString(NSName name) {
      return getAttrTree(name).toString();
    }
  }

  /** Jackson JSON utilities. */
  public static class JsonSupport {
    private JsonSupport() {}

    /** Jackson module to support {@link LoomGraph}. */
    public static class LoomGraphModule extends SimpleModule {
      public LoomGraphModule() {
        super("LoomGraphModule");
        addKeyDeserializer(NSName.class, new NSName.JsonSupport.KeyDeserializer());
      }
    }

    /** Serializer to write a {@link Map<UUID, Node>} as an array of {@link Node}. */
    static final class NodesSerializer extends JsonSerializer<Map<UUID, Node>> {
      @Override
      public void serialize(
          Map<UUID, Node> value, JsonGenerator gen, SerializerProvider serializers)
          throws IOException {
        gen.writeStartArray();

        // Stable output ordering.
        var nodes = new ArrayList<>(value.values());
        nodes.sort(Comparator.comparing(n -> n.id));

        for (var node : nodes) {
          gen.writeObject(node);
        }

        gen.writeEndArray();
      }
    }

    /** Deserializer to read a {@link Map<UUID, Node>} from an array of {@link Node}. */
    static final class NodesDeserializer extends StdDeserializer<Map<UUID, Node>> {
      public NodesDeserializer() {
        super(Map.class);
      }

      @Override
      public Map<UUID, Node> deserialize(JsonParser p, DeserializationContext ctxt)
          throws java.io.IOException {
        var nodes = p.readValueAs(Node[].class);
        Map<UUID, Node> nodeMap = new HashMap<>();
        for (var node : nodes) {
          nodeMap.put(node.id, node);
        }
        return nodeMap;
      }
    }
  }

  @Nonnull
  @JsonProperty(required = true)
  private UUID id = UUID.randomUUID();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Nullable private UUID parentId;

  @JsonProperty(value = "nodes", required = true)
  @JsonSerialize(using = JsonSupport.NodesSerializer.class)
  @JsonDeserialize(using = JsonSupport.NodesDeserializer.class)
  private final Map<UUID, Node> nodeMap = new HashMap<>();

  /**
   * Create a copy with the same nodes and ID.
   *
   * <p>The node children are considered immutable, and are copied by reference.
   *
   * @return The copy.
   */
  public LoomGraph copy() {
    var copy = new LoomGraph();
    copy.id = id;
    copy.parentId = parentId;
    copy.nodeMap.putAll(nodeMap);
    return copy;
  }

  /**
   * Create a copy with the same nodes and a new ID.
   *
   * <p>The node children are considered immutable, and are copied by reference.
   *
   * @return The copy.
   */
  public LoomGraph newChild() {
    var copy = new LoomGraph();
    copy.parentId = id;
    copy.nodeMap.putAll(nodeMap);
    return copy;
  }

  @Override
  public String toJsonString() {
    return toPrettyJsonString();
  }

  @Override
  public String toString() {
    return toJsonString();
  }

  /**
   * Iterable view of the nodes in the graph.
   *
   * @return The nodes.
   */
  public Iterable<Node> nodes() {
    return nodeMap.values();
  }

  /**
   * Stream view of the nodes in the graph.
   *
   * @return The nodes.
   */
  public Stream<Node> nodeStream() {
    return nodeMap.values().stream();
  }

  /** Generate a new ID which is not already in use. */
  public UUID newId() {
    UUID id = UUID.randomUUID();
    while (nodeMap.containsKey(id)) {
      id = UUID.randomUUID();
    }
    return id;
  }

  /**
   * Add a node to the graph.
   *
   * @param node The node.
   * @return The node.
   */
  public Node addNode(Node node) {
    nodeMap.put(node.id, node);
    return node;
  }

  /**
   * Add a node to the graph.
   *
   * <p>If the node has no ID, a new ID will be generated using {@link #newId()}.
   *
   * @param builder a builder for the node.
   * @return The new node.
   */
  public Node addNode(Node.NodeBuilder builder) {
    if (builder.id == null) {
      builder = builder.id(newId());
    }
    return addNode(builder.build());
  }

  /**
   * Does the graph have a node with the given ID?
   *
   * @param id The node ID.
   * @return True if the node exists.
   */
  public boolean hasNode(UUID id) {
    return nodeMap.containsKey(id);
  }

  /**
   * Remove a node from the graph.
   *
   * @param id The node ID.
   */
  public void removeNode(UUID id) {
    nodeMap.remove(id);
  }

  /**
   * Lookup a node in the graph.
   *
   * @param id The node ID.
   * @return The node.
   * @throws NoSuchElementException if no node with the given ID exists.
   */
  public Node lookupNode(UUID id) {
    var node = nodeMap.get(id);
    if (node == null) {
      throw new NoSuchElementException("No node with ID " + id);
    }
    return node;
  }

  /**
   * Return the set of namespaces used by the graph.
   *
   * @return The namespaces.
   */
  public Set<String> namespaces() {
    var namespaces = new HashSet<String>();
    for (var node : nodeMap.values()) {
      namespaces.addAll(node.namespaces());
    }
    return namespaces;
  }
}
