package loom.alt.densegraph;

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
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Data;

@Data
public class ExprGraph implements Iterable<EGNodeBase> {
  @JsonProperty(required = true)
  public UUID id = UUID.randomUUID();

  @JsonProperty(value = "nodes", required = true)
  @JsonSerialize(using = ExprGraph.JsonSupport.NodesSerializer.class)
  @JsonDeserialize(using = ExprGraph.JsonSupport.NodesDeserializer.class)
  private final Map<UUID, EGNodeBase> nodeMap = new HashMap<>();

  public ExprGraph() {}

  public UUID newId() {
    var id = UUID.randomUUID();
    while (nodeMap.containsKey(id)) {
      id = UUID.randomUUID();
    }
    return id;
  }

  @Nonnull
  @Override
  public Iterator<EGNodeBase> iterator() {
    return nodeMap.values().iterator();
  }

  public void validate() {
    List<ValidationError> errors = new ArrayList<>();
    for (EGNodeBase node : nodeMap.values()) {
      node.validationErrors(this, errors::add);
    }
    if (!errors.isEmpty()) {
      throw new ValidationError(
          "Validation errors:\n"
              + errors.stream().map(ValidationError::toString).collect(Collectors.joining("\n")));
    }
  }

  public void validateContains(UUID id) {
    if (!contains(id)) {
      throw new ValidationError(id, "Node not found in graph");
    }
  }

  public void validateLink(UUID from, String name, UUID to) {
    if (!contains(to)) {
      throw new ValidationError(
          from, String.format("%s -(%s)-> %s to node not found in graph", from, name, to));
    }
  }

  /**
   * Check if the graph contains a node with the given id.
   *
   * @param id The id to check.
   * @return True if the graph contains a node with the given id.
   */
  public boolean contains(UUID id) {
    return nodeMap.containsKey(id);
  }

  /**
   * Add a node to the graph.
   *
   * @param node The node to add.
   * @return The node that was added.
   * @throws IllegalStateException If the node is already part of a graph.
   */
  public <T extends EGNodeBase> T addNode(T node) {
    nodeMap.put(node.id, node);
    return node;
  }

  /**
   * Add a node to the graph.
   *
   * <p>The builder will have an id assigned, and then the build will be completed.
   *
   * @param builder The builder to use.
   * @return The node that was added.
   * @param <T> the type of node.
   * @param <B> the type of builder.
   */
  public <T extends EGNodeBase, B extends EGNodeBase.EGNodeBaseBuilder<T, B>> T addNode(
      EGNodeBase.EGNodeBaseBuilder<T, B> builder) {
    var node = builder.id(newId()).build();
    return addNode(node);
  }

  public EGNodeBase getNode(UUID id) {
    var node = nodeMap.get(id);
    if (node == null) {
      throw new IllegalArgumentException("Node not found: " + id);
    }
    return node;
  }

  public <T extends EGNodeBase> T getNode(UUID id, Class<T> type) {
    return type.cast(getNode(id));
  }

  static final class JsonSupport {

    /** Private constructor to prevent instantiation. */
    private JsonSupport() {}

    static final class NodesSerializer extends JsonSerializer<Map<UUID, EGNodeBase>> {
      @Override
      public void serialize(
          Map<UUID, EGNodeBase> value, JsonGenerator gen, SerializerProvider serializers)
          throws IOException {
        gen.writeStartArray();

        // Stable output ordering.
        var nodes = new ArrayList<>(value.values());
        nodes.sort(Comparator.comparing(n -> n.id));

        for (EGNodeBase node : nodes) {
          gen.writeObject(node);
        }

        gen.writeEndArray();
      }
    }

    static final class NodesDeserializer extends StdDeserializer<Map<UUID, EGNodeBase>> {
      public NodesDeserializer() {
        super(Map.class);
      }

      @Override
      public Map<UUID, EGNodeBase> deserialize(JsonParser p, DeserializationContext ctxt)
          throws java.io.IOException {
        var nodes = p.readValueAs(EGNodeBase[].class);
        Map<UUID, EGNodeBase> nodeMap = new HashMap<>();
        for (EGNodeBase node : nodes) {
          nodeMap.put(node.id, node);
        }
        return nodeMap;
      }
    }
  }
}
