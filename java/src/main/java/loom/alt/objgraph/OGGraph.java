package loom.alt.objgraph;

import com.fasterxml.jackson.annotation.JsonCreator;
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
import javax.annotation.Nonnull;
import lombok.Data;
import loom.common.HasToJsonString;

@Data
public final class OGGraph implements HasToJsonString, Iterable<OGNode> {
  @JsonProperty(required = true)
  public UUID id = UUID.randomUUID();

  @JsonProperty(value = "nodes", required = true)
  @JsonSerialize(using = OGGraph.JsonSupport.NodesSerializer.class)
  @JsonDeserialize(using = OGGraph.JsonSupport.NodesDeserializer.class)
  private final Map<UUID, OGNode> nodeMap = new HashMap<>();

  public OGGraph() {}

  @JsonCreator
  public OGGraph(@JsonProperty("id") UUID id, @JsonProperty("nodes") Map<UUID, OGNode> nodeMap) {
    this.id = id;
    this.nodeMap.putAll(nodeMap);
  }

  @Nonnull
  @Override
  public Iterator<OGNode> iterator() {
    return nodeMap.values().iterator();
  }

  public UUID newId() {
    var id = UUID.randomUUID();
    while (nodeMap.containsKey(id)) {
      id = UUID.randomUUID();
    }
    return id;
  }

  /**
   * Add a node to the graph.
   *
   * @param node The node to add.
   * @return The node that was added.
   * @throws IllegalStateException If the node is already part of a graph.
   */
  public OGNode addNode(OGNode node) {
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
   */
  public OGNode addNode(OGNode.OGNodeBuilder builder) {
    var node = builder.id(newId()).build();
    return addNode(node);
  }

  static final class JsonSupport {

    /** Private constructor to prevent instantiation. */
    private JsonSupport() {}

    static final class NodesSerializer extends JsonSerializer<Map<UUID, OGNode>> {
      @Override
      public void serialize(
          Map<UUID, OGNode> value, JsonGenerator gen, SerializerProvider serializers)
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

    static final class NodesDeserializer extends StdDeserializer<Map<UUID, OGNode>> {
      public NodesDeserializer() {
        super(Map.class);
      }

      @Override
      public Map<UUID, OGNode> deserialize(JsonParser p, DeserializationContext ctxt)
          throws java.io.IOException {
        var nodes = p.readValueAs(OGNode[].class);
        Map<UUID, OGNode> nodeMap = new HashMap<>();
        for (var node : nodes) {
          nodeMap.put(node.id, node);
        }
        return nodeMap;
      }
    }
  }
}
