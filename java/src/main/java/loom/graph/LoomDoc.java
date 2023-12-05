package loom.graph;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.extern.jackson.Jacksonized;
import loom.common.HasToJsonString;
import loom.common.LookupError;
import loom.common.serialization.JsonUtil;
import loom.common.serialization.MapValueListUtil;
import org.jetbrains.annotations.NotNull;

/**
 * A Loom Graph document.
 *
 * <p>This class bears a 1:1 relationship with the JSON representation of a graph.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoomDoc implements HasToJsonString {
  /**
   * A node in the graph.
   *
   * <p>This class bears a 1:1 relationship with the JSON representation of a graph.
   */
  @Data
  @Jacksonized
  @Builder(toBuilder = true)
  public static class NodeDoc implements HasToJsonString {
    /** Extensions to the NodeDoc builder. */
    public static class NodeDocBuilder {
      /**
       * Add a field to the node.
       *
       * <p>The field will be converted to a JsonNode via Jackson defaults.
       *
       * @param key the field name.
       * @param value the field value as a Java object.
       * @return the builder.
       */
      public NodeDocBuilder fieldFromObject(String key, Object value) {
        return this.field(key, JsonUtil.toTree(value));
      }

      /**
       * Add a field to the node.
       *
       * <p>The field will be converted to a JsonNode via parsing the string.
       *
       * @param key the field name.
       * @param json the field value as a JSON string.
       * @return the builder.
       */
      public NodeDocBuilder fieldFromString(String key, String json) {
        return this.field(key, JsonUtil.readTree(json));
      }

      /**
       * Interpret the given object as JSON and add the fields to the node.
       *
       * @param fields the fields to add.
       * @return the builder.
       */
      public NodeDocBuilder asFields(Object fields) {
        JsonUtil.toTree(fields)
            .fields()
            .forEachRemaining(entry -> this.field(entry.getKey(), entry.getValue()));
        return this;
      }
    }

    @NotNull private final UUID id;
    @NotNull private final String type;

    @Nullable private String label;

    @Singular private final Map<String, JsonNode> fields;

    public NodeDoc deepCopy() {
      var builder = builder();
      builder.id(id);
      builder.type(type);
      builder.label(label);

      fields.forEach((key, value) -> builder.field(key, value.deepCopy()));

      return builder.build();
    }

    /**
     * Does this node have a field with the given name?
     *
     * @param key the field name.
     * @return true if the node has a field with the given name.
     */
    public boolean hasField(String key) {
      return fields.containsKey(key);
    }

    /**
     * Get a field from the node; read as a JsonNode.
     *
     * @param key the field name.
     * @return the field value.
     */
    public JsonNode getFieldAsJsonNode(String key) {
      var node = fields.get(key);
      if (node == null) {
        throw new LookupError("Field not found: " + key);
      }
      return node;
    }

    /**
     * Get a field from the node; read as the target parse type.
     *
     * @param key the field name.
     * @param type the target type.
     * @return the field value.
     * @param <T> the target type.
     */
    public <T> T getFieldAsType(String key, Class<T> type) {
      return JsonUtil.convertValue(getFieldAsJsonNode(key), type);
    }

    /**
     * Get a field from the node; read as an Object.
     *
     * @param key the field name.
     * @return the field value.
     */
    public Object getFieldAsObject(String key) {
      return getFieldAsType(key, Object.class);
    }
  }

  @Nullable private UUID id;

  @JsonSerialize(using = MapValueListUtil.MapSerializer.class)
  @JsonDeserialize(using = JacksonSupport.NodeListToMapDeserializer.class)
  private final Map<UUID, NodeDoc> nodes = new HashMap<>();

  /** Make a full deep copy of the XGraphDoc. */
  @CheckReturnValue
  public LoomDoc deepCopy() {
    var dup = new LoomDoc();
    dup.id = id;
    nodes.values().forEach(n -> dup.addNode(n.deepCopy()));
    return dup;
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
  public NodeDoc assertNode(UUID id) {
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
  public NodeDoc assertNode(String id) {
    return assertNode(UUID.fromString(id));
  }

  /**
   * Add a node to the graph.
   *
   * @param node the node to add.
   * @return the ID of the node.
   */
  public UUID addNode(NodeDoc node) {
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
  public UUID addNode(NodeDoc.NodeDocBuilder builder) {
    if (builder.id == null) {
      builder.id = newUnusedNodeId();
    }

    var node = builder.build();
    return addNode(node);
  }

  /** Support classes for Jackson serialization. */
  public static class JacksonSupport {
    private JacksonSupport() {}

    /** Jackson deserializer for {@link LoomDoc#nodes}. */
    public static class NodeListToMapDeserializer
        extends MapValueListUtil.MapDeserializer<UUID, NodeDoc> {
      public NodeListToMapDeserializer() {
        super(NodeDoc.class, NodeDoc::getId, HashMap.class);
      }
    }
  }
}
