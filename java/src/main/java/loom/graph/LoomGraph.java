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
import lombok.Data;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
public class LoomGraph {
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
     * @param id   The ID of the node to get.
     * @param type The type to cast the node to.
     * @param <T>  The type to cast the node to.
     * @return The node with the given ID.
     * @throws ClassCastException If the node is not of the given type.
     */
    public <T extends LoomNode> T lookupNode(UUID id, Class<T> type) {
        return type.cast(lookupNode(id));
    }

    /**
     * Add a node to the graph.
     *
     * @param node The node to add.
     * @return The node that was added.
     * @throws IllegalStateException If the node is already part of a graph.
     */
    public LoomNode addNode(LoomNode node) {
        if (node.graph != null) {
            throw new IllegalStateException("Node is already part of a graph");
        }
        nodeMap.put(node.id, node);
        node.graph = this;
        return node;
    }

    static final class JsonSupport {
        /**
         * Private constructor to prevent instantiation.
         */
        private JsonSupport() {
        }

        static final class NodesSerializer extends JsonSerializer<Map<UUID, LoomNode>> {
            @Override
            public void serialize(
                    Map<UUID, LoomNode> value, JsonGenerator gen, SerializerProvider serializers)
                    throws IOException {
                gen.writeStartArray();
                for (LoomNode node : value.values()) {
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
