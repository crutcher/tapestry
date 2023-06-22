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
import loom.common.HasToJsonString;
import loom.common.LookupError;
import loom.common.ReflectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
public final class TGraph implements Iterable<TNode>, HasToJsonString {
    @JsonProperty(value = "nodes", required = true)
    @JsonSerialize(using = TGraph.JsonSupport.NodesSerializer.class)
    @JsonDeserialize(using = TGraph.JsonSupport.NodesDeserializer.class)
    private final Map<UUID, TNode> nodeMap = new HashMap<>();

    @Override
    public Iterator<TNode> iterator() {
        return nodeMap.values().iterator();
    }

    public TGraph copy() {
        var copy = new TGraph();
        for (var node : this) {
            copy.addNode(node.copy());
        }
        return copy;
    }

    /**
     * Node stream manipulation wrapper.
     *
     * @param <T> The type of node to filter for.
     */
    public class NodeStream<T extends TNode> {
        private final Class<T> nodeType;
        @Nullable
        protected Stream<T> stream;

        /**
         * Create a new node stream.
         *
         * @param nodeType The type of node to filter for.
         */
        private NodeStream(Class<T> nodeType) {
            ReflectionUtils.checkIsSubclass(nodeType, TNode.class);
            this.nodeType = nodeType;
            stream = nodeMap.values().stream().filter(nodeType::isInstance).map(nodeType::cast);
        }

        protected Stream<T> assertStream() {
            if (stream == null) {
                throw new IllegalStateException("Stream has already been consumed");
            }
            return stream;
        }

        /**
         * Filter the stream to only include nodes that are subclasses of the given types.
         *
         * @param types The types to filter for.
         * @return this node stream, for chaining.
         */
        public NodeStream<T> restrictedTo(@Nonnull Class<?>... types) {
            for (var type : types) {
                ReflectionUtils.checkIsSubclass(type, nodeType);
            }
            stream =
                    assertStream()
                            .filter(
                                    n -> {
                                        for (var type : types) {
                                            if (type.isInstance(n)) {
                                                return true;
                                            }
                                        }
                                        return false;
                                    });
            return this;
        }

        /**
         * Filter the stream to exclude nodes that are subclasses of the given types.
         *
         * @param types The types to filter out.
         * @return this node stream, for chaining.
         */
        public NodeStream<T> excluding(@Nonnull Class<?>... types) {
            for (var type : types) {
                ReflectionUtils.checkIsSubclass(type, nodeType);
            }
            stream =
                    assertStream()
                            .filter(
                                    n -> {
                                        for (var type : types) {
                                            if (type.isInstance(n)) {
                                                return false;
                                            }
                                        }
                                        return true;
                                    });
            return this;
        }

        /**
         * Filter the stream to only include nodes that pass the given predicate.
         *
         * @return this node stream, for chaining.
         */
        public NodeStream<T> withFilter(@Nonnull Predicate<T> filter) {
            stream = assertStream().filter(filter);
            return this;
        }

        /**
         * Consume the stream and apply the given action to each node.
         *
         * @param action The action to apply.
         */
        public void forEach(Consumer<T> action) {
            toStream().forEach(action);
        }

        public Stream<T> toStream() {
            var stream = assertStream();
            this.stream = null;
            return stream;
        }

        /**
         * Consume the stream and return a list of the results.
         *
         * @return The list of nodes.
         */
        public List<T> toList() {
            return toStream().collect(Collectors.toList());
        }

        /**
         * Consume the stream and return the single item, or null if the stream is empty.
         *
         * @return The single item, or null if the stream is empty.
         * @throws IllegalStateException If the stream contains more than one item.
         */
        public T toOptionalSingleton() {
            var items = toList();
            if (items.size() > 1) {
                throw new IllegalStateException("Expected exactly one item, got " + items.size());
            }
            if (items.isEmpty()) {
                return null;
            }
            return items.get(0);
        }

        /**
         * Consume the stream and return the single item.
         *
         * @return The single item.
         * @throws IllegalStateException If the stream contains none or more than one item.
         */
        public T toSingleton() {
            var item = toOptionalSingleton();
            if (item == null) {
                throw new IllegalStateException("Expected exactly one item, got none");
            }
            return item;
        }
    }

    public <T extends TNode> NodeStream<T> queryNodes(Class<T> nodeType) {
        return new NodeStream<>(nodeType);
    }

    public class TagStream<T extends TTag> extends NodeStream<T> {
        private TagStream(Class<T> tagType) {
            super(tagType);
            ReflectionUtils.checkIsSubclass(tagType, TTag.class);
        }

        public TagStream<T> withSourceId(UUID sourceId) {
            this.stream = assertStream().filter(tag -> tag.sourceId.equals(sourceId));
            return this;
        }
    }

    public <T extends TTag> TagStream<T> queryTags(Class<T> tagType) {
        return new TagStream<>(tagType);
    }

    public class EdgeStream<T extends TEdge> extends TagStream<T> {
        private EdgeStream(Class<T> tagType) {
            super(tagType);
            ReflectionUtils.checkIsSubclass(tagType, TEdge.class);
        }

        public EdgeStream<T> withTargetId(UUID targetId) {
            this.stream = assertStream().filter(tag -> tag.targetId.equals(targetId));
            return this;
        }
    }

    public <T extends TEdge> EdgeStream<T> queryEdges(Class<T> edgeType) {
        return new EdgeStream<>(edgeType);
    }

    /**
     * Lookup a node by its ID.
     *
     * @param id The ID to lookup.
     * @return The node.
     * @throws LookupError If the node does not exist in the graph.
     */
    public TNode lookupNode(UUID id) {
        var node = nodeMap.get(id);
        if (node == null) {
            throw new LookupError(id + " not found in graph");
        }
        return node;
    }

    /**
     * Lookup a node by its ID, and cast it to the given type.
     *
     * @param id       The ID to lookup.
     * @param nodeType The type to cast to.
     * @param <T>      The type to cast to.
     * @return The node.
     * @throws LookupError        If the node does not exist in the graph.
     * @throws ClassCastException If the node is not of the given type.
     */
    public <T extends TNode> T lookupNode(UUID id, Class<T> nodeType) {
        return nodeType.cast(nodeMap.get(id));
    }

    /**
     * Add a node to the graph.
     *
     * @param node The node to add.
     * @return The node that was added.
     * @throws IllegalStateException If the node is already part of a graph.
     */
    public <T extends TNode> T addNode(T node) {
        if (node.graph != null) {
            throw new IllegalStateException("Node is already part of a graph");
        }
        nodeMap.put(node.id, node);
        node.graph = this;
        return node;
    }

    public void validate() {
        for (var node : nodeMap.values()) {
            node.validate();
        }
    }

    static final class JsonSupport {

        /**
         * Private constructor to prevent instantiation.
         */
        private JsonSupport() {
        }

        static final class NodesSerializer extends JsonSerializer<Map<UUID, TNode>> {
            @Override
            public void serialize(
                    Map<UUID, TNode> value, JsonGenerator gen, SerializerProvider serializers)
                    throws IOException {
                gen.writeStartArray();

                // Stable output ordering.
                var nodes = new ArrayList<>(value.values());
                nodes.sort(Comparator.comparing(n -> n.id));

                for (TNode node : nodes) {
                    gen.writeObject(node);
                }

                gen.writeEndArray();
            }
        }

        static final class NodesDeserializer extends StdDeserializer<Map<UUID, TNode>> {
            public NodesDeserializer() {
                super(Map.class);
            }

            @Override
            public Map<UUID, TNode> deserialize(JsonParser p, DeserializationContext ctxt)
                    throws java.io.IOException {
                var nodes = p.readValueAs(TNode[].class);
                Map<UUID, TNode> nodeMap = new HashMap<>();
                for (TNode node : nodes) {
                    nodeMap.put(node.id, node);
                }
                return nodeMap;
            }
        }
    }
}
