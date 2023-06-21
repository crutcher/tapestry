package loom.graph;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import lombok.Data;
import loom.common.IteratorUtils;

@Data
public final class LoomGraph {
  @JsonProperty(value = "nodes", required = true)
  @JsonSerialize(using = LoomGraph.JsonSupport.NodesSerializer.class)
  @JsonDeserialize(using = LoomGraph.JsonSupport.NodesDeserializer.class)
  private final Map<UUID, LoomNode> nodeMap = new HashMap<>();

  /**
   * Get a node by its ID.
   *
   * @param id The ID of the node to get.
   * @return The node with the given ID.
   */
  public LoomNode lookupNode(UUID id) {
    return nodeMap.get(id);
  }

  /**
   * Get a node by its ID and check a cast it to the given type.
   *
   * @param id The ID of the node to get.
   * @param type The type to cast the node to.
   * @param <T> The type to cast the node to.
   * @return The node with the given ID.
   * @throws ClassCastException If the node is not of the given type.
   */
  public <T extends LoomNode> T lookupNode(UUID id, Class<T> type) {
    return type.cast(lookupNode(id));
  }

  public <T extends LoomNode> List<T> listNodes(Class<T> nodeType) {
    return listNodes(nodeType, null, null);
  }

  public <T extends LoomNode> List<T> listNodes(
      Class<T> nodeType,
      @Nullable Iterable<Class<? extends T>> restrictToTypes,
      @Nullable Iterable<Class<? extends T>> excludeTypes) {
    return listNodes(nodeType, restrictToTypes, excludeTypes, null);
  }

  public <T extends LoomNode> Stream<T> _listNodes(
      Class<T> nodeType,
      @Nullable Iterable<Class<? extends T>> restrictToTypes,
      @Nullable Iterable<Class<? extends T>> excludeTypes,
      @Nullable Predicate<T> filter) {
    @SuppressWarnings("unchecked")
    Stream<T> stream = (Stream<T>) nodeMap.values().stream().filter(nodeType::isInstance);

    if (restrictToTypes != null && IteratorUtils.iterableIsNotEmpty(restrictToTypes)) {
      stream =
          stream.filter(
              n -> {
                for (var type : restrictToTypes) {
                  if (type.isInstance(n)) {
                    return true;
                  }
                }
                return false;
              });
    }

    if (excludeTypes == null) {
      stream = stream.filter(n -> !(n instanceof LoomTag));
    } else if (IteratorUtils.iterableIsNotEmpty(excludeTypes)) {
      stream =
          stream.filter(
              n -> {
                for (var type : excludeTypes) {
                  if (type.isInstance(n)) {
                    return false;
                  }
                }
                return true;
              });
    }

    if (filter != null) {
      stream = stream.filter(filter);
    }

    return stream;
  }

  public <T extends LoomNode> List<T> listNodes(
      Class<T> nodeType,
      @Nullable Iterable<Class<? extends T>> restrictToTypes,
      @Nullable Iterable<Class<? extends T>> excludeTypes,
      @Nullable Predicate<T> filter) {
    return _listNodes(nodeType, restrictToTypes, excludeTypes, filter).collect(Collectors.toList());
  }

  public <T extends LoomTag> List<T> listTags(
      Class<T> tagType,
      @Nullable UUID sourceId,
      @Nullable Iterable<Class<? extends T>> restrictToTypes,
      @Nullable Iterable<Class<? extends T>> excludeTypes,
      @Nullable Predicate<T> filter) {
    return _listTags(tagType, sourceId, restrictToTypes, excludeTypes, filter)
        .collect(Collectors.toList());
  }

  public <T extends LoomTag> Stream<T> _listTags(
      Class<T> tagType,
      @Nullable UUID sourceId,
      @Nullable Iterable<Class<? extends T>> restrictToTypes,
      @Nullable Iterable<Class<? extends T>> excludeTypes,
      @Nullable Predicate<T> filter) {

    var stream = _listNodes(tagType, restrictToTypes, excludeTypes, filter);
    if (sourceId != null) {
      stream = stream.filter(tag -> tag.sourceId.equals(sourceId));
    }
    if (excludeTypes == null) {
      stream = stream.filter(tag -> !(tag instanceof LoomEdge));
    }

    return stream;
  }

  public <T extends LoomEdge> List<T> listEdges(
      Class<T> edgeType,
      @Nullable UUID sourceId,
      @Nullable UUID targetId,
      @Nullable Iterable<Class<? extends T>> restrictToTypes,
      @Nullable Iterable<Class<? extends T>> excludeTypes,
      @Nullable Predicate<T> filter) {
    return _listEdges(edgeType, sourceId, targetId, restrictToTypes, excludeTypes, filter)
        .collect(Collectors.toList());
  }

  public <T extends LoomEdge> Stream<T> _listEdges(
      Class<T> edgeType,
      @Nullable UUID sourceId,
      @Nullable UUID targetId,
      @Nullable Iterable<Class<? extends T>> restrictToTypes,
      @Nullable Iterable<Class<? extends T>> excludeTypes,
      @Nullable Predicate<T> filter) {

    var stream = _listTags(edgeType, sourceId, restrictToTypes, excludeTypes, filter);
    if (targetId != null) {
      stream = stream.filter(edge -> edge.targetId.equals(targetId));
    }

    return stream;
  }

  /**
   * Add a node to the graph.
   *
   * @param node The node to add.
   * @return The node that was added.
   * @throws IllegalStateException If the node is already part of a graph.
   */
  public <T extends LoomNode> T addNode(T node) {
    if (node.graph != null) {
      throw new IllegalStateException("Node is already part of a graph");
    }
    nodeMap.put(node.id, node);
    node.graph = this;
    return node;
  }

  static final class JsonSupport {

    /** Private constructor to prevent instantiation. */
    private JsonSupport() {}

    static final class NodesSerializer extends JsonSerializer<Map<UUID, LoomNode>> {
      @Override
      public void serialize(
          Map<UUID, LoomNode> value, JsonGenerator gen, SerializerProvider serializers)
          throws IOException {
        gen.writeStartArray();

        // Stable output ordering.
        var nodes = new ArrayList<>(value.values());
        nodes.sort(Comparator.comparing(n -> n.id));

        for (LoomNode node : nodes) {
          gen.writeObject(node);
        }

        gen.writeEndArray();
      }
    }

    static final class NodesDeserializer extends StdDeserializer<Map<UUID, LoomNode>> {
      public NodesDeserializer() {
        super(Map.class);
      }

      @Override
      public Map<UUID, LoomNode> deserialize(JsonParser p, DeserializationContext ctxt)
          throws java.io.IOException {
        var nodes = p.readValueAs(LoomNode[].class);
        Map<UUID, LoomNode> nodeMap = new HashMap<>();
        for (LoomNode node : nodes) {
          nodeMap.put(node.id, node);
        }
        return nodeMap;
      }
    }
  }
}
