package org.tensortapestry.loom.graph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import lombok.*;
import lombok.experimental.UtilityClass;
import org.tensortapestry.common.collections.StreamableIterable;
import org.tensortapestry.common.json.HasToJsonString;
import org.tensortapestry.common.json.MapValueListUtil;
import org.tensortapestry.common.runtime.ReflectionUtils;
import org.tensortapestry.common.validation.LoomValidationError;
import org.tensortapestry.common.validation.ValidationIssueCollector;
import org.tensortapestry.loom.graph.dialects.common.JsdType;

/**
 * A Loom Graph document.
 * <p>
 * A {@link LoomGraph} is a collection of {@link LoomNode}s, paired with a {@link LoomEnvironment}.
 * <p>
 * Relationships, edges, and other graph structures are not directly represented in the graph
 * document, but are instead represented by data in the nodes themselves.
 * <p>
 * Different {@link LoomEnvironment}s may permit different types of nodes, and impose different
 * constraints on the graph; and common collections of permitted types and constraints can be
 * composed together into <b>Loom Dialects</b>.
 *
 * <h2>Serialization</h2>
 * Base {@link LoomGraph} serialization is a JSON object with the following properties:
 * <pre>{@code
 * {
 *   "id": "<graph-uuid>",
 *   "nodes": [
 *     {
 *       "id": "<node-uuid>",
 *       "type": "<type-url>",
 *       "label": "<non-unique-label>",  // optional
 *       "body": <type-specific-JSON>,
 *       "tags": { // optional
 *         <tag-type>: <tag-type-specific-JSON>,
 *         ...
 *       }
 *     },
 *     ...
 *   ]
 * }
 * }</pre>
 *
 * <h3>msgpack</h3>
 * <p>
 * The chosen serialization format for LoomGraph is JSON, in a way which works equally well with
 * <b>msgpack</b>.
 *
 * <h2>Example</h2>
 * <p>
 * Assuming two very simple types, a tensor and a simple operation with no sharding information, we
 * could define types and graphs such that a desaturation operation is performed on a tensor of
 * images:
 *
 * <pre>{@code
 * {
 *   "id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
 *   "nodes": [
 *     {
 *       "id": "8e3f8f1e-6c54-4b01-90e6-0ae1a048f0851",
 *       "type": "https://<schema-url>#/types/Tensor",
 *       "label": "Images",
 *       "body": {
 *         "dtype": "uint8"
 *         "shape": [100, 256, 256, 3],
 *       }
 *     },
 *     {
 *       "id": "8e3f8f1e-6c54-4b01-90e6-0ae1a048f9000",
 *       "type": "https://<schema-url>#/types/Tensor",
 *       "label": "Monochrome",
 *       "body": {
 *         "dtype": "uint8"
 *         "shape": [100, 256, 256, 3],
 *       }
 *     },
 *     {
 *       "id": "8e3f8f1e-6c54-4b01-90e6-0ae1a048faaaa",
 *       "type": "https://<schema-url>#/types/Operation",
 *       "body": {
 *         "kernel": "desaturate",
 *         "inputs": ["8e3f8f1e-6c54-4b01-90e6-0ae1a048f0851"],
 *         "outputs": ["8e3f8f1e-6c54-4b01-90e6-0ae1a048f9000"],
 *       }
 *     }
 *   ]
 * }
 * }</pre>
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Getter
@Setter
public final class LoomGraph implements HasToJsonString, StreamableIterable<LoomNode> {

  @Nullable
  @JsonIgnore
  private LoomEnvironment env = null;

  @JsonInclude
  @Nonnull
  private UUID id = UUID.randomUUID();

  @JsonSerialize(using = MapValueListUtil.MapSerializer.class)
  @JsonDeserialize(using = Serialization.NodeListToMapDeserializer.class)
  private final Map<UUID, LoomNode> nodes = new HashMap<>();

  public LoomGraph(@Nonnull LoomEnvironment env) {
    this.env = env;
  }

  @JsonCreator
  public LoomGraph(
    @JsonProperty("id") @Nonnull UUID id,
    @JsonProperty("nodes") @Nullable Map<UUID, LoomNode> nodes
  ) {
    this.id = id;
    if (nodes != null) {
      this.nodes.putAll(nodes);
    }
  }

  @Nonnull
  public LoomGraph copy() {
    var g = new LoomGraph(env);
    g.id = id;
    nodes.values().stream().map(LoomNode::copy).forEach(g::addNode);
    return g;
  }

  /**
   * Get the environment this graph belongs to.
   *
   * @return the environment.
   * @throws IllegalStateException if the environment is not set.
   */
  @Nonnull
  public LoomEnvironment assertEnv() {
    if (env == null) {
      throw new IllegalStateException("Environment not set");
    }
    return env;
  }

  /**
   * Validate the graph.
   *
   * @throws LoomValidationError if the graph is invalid.
   */
  public void validate() {
    assertEnv().validateGraph(this);
  }

  /**
   * Validate the graph.
   *
   * @param issueCollector the issue collector to use.
   */
  public void validate(ValidationIssueCollector issueCollector) {
    assertEnv().validateGraph(this, issueCollector);
  }

  /**
   * Get an iterator over all nodes in the graph.
   *
   * @return the iterator.
   */
  @Override
  @NotNull
  @Nonnull
  public Iterator<LoomNode> iterator() {
    return nodes.values().iterator();
  }

  /**
   * Get a stream of all nodes in the graph.
   *
   * @return the stream.
   */
  @Override
  @Nonnull
  public Stream<LoomNode> stream() {
    return nodes.values().stream();
  }

  /**
   * Get all nodes of the given type.
   *
   * @param type the node type.
   * @return the nodes.
   */
  @Nonnull
  public StreamableIterable<LoomNode> byType(String type) {
    return () -> nodes.values().stream().filter(node -> node.getType().equals(type)).iterator();
  }

  /**
   * Get all nodes of the given type, wrapped in the given wrapper class.
   *
   * @param wrapperClass the wrapper class; must have a constructor taking a {@link LoomNode} and
   *   a {@code @JsdType} annotation.
   * @param <W> the wrapper type.
   * @return the nodes.
   */
  @Nonnull
  public <W extends LoomNodeWrapper<W>> StreamableIterable<W> byType(Class<W> wrapperClass) {
    return byType(
      JsdType.Util.assertType(wrapperClass),
      n -> ReflectionUtils.newInstance(wrapperClass, n)
    );
  }

  /**
   * Get all nodes of the given type, wrapped in the given wrapper class.
   *
   * @param type the node type.
   * @param wrap a function to wrap the nodes.
   * @param <W> the wrapper type.
   * @return the nodes.
   */
  @Nonnull
  public <W extends LoomNodeWrapper<W>> StreamableIterable<W> byType(
    String type,
    Function<LoomNode, W> wrap
  ) {
    return () ->
      nodes.values().stream().filter(node -> node.getType().equals(type)).map(wrap).iterator();
  }

  /**
   * Create a new, unused node ID.
   *
   * @return the new ID.
   */
  @Nonnull
  public UUID genNodeId() {
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
   * @return the node, or null if not found.
   */
  @Nullable
  public LoomNode getNode(UUID id) {
    return nodes.get(id);
  }

  /**
   * Get the node with the given ID.
   *
   * @param id the ID of the node to get.
   * @return the node, or null if not found.
   */
  @Nullable
  public LoomNode getNode(String id) {
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
  public LoomNode assertNode(UUID id) {
    var node = getNode(id);
    if (node == null) {
      throw new IllegalStateException("Node not found: " + id);
    }
    return node;
  }

  public <W extends LoomNodeWrapper<W>> W assertNode(UUID id, Function<LoomNode, W> wrap) {
    return wrap.apply(assertNode(id));
  }

  public <W extends LoomNodeWrapper<W>> W assertNode(UUID id, Class<W> wrapper) {
    return ReflectionUtils.newInstance(wrapper, assertNode(id));
  }

  /**
   * Get the node with the given ID.
   *
   * @param id the ID of the node to get.
   * @return the node.
   * @throws IllegalStateException if the node does not exist.
   */
  @Nonnull
  public LoomNode assertNode(String id) {
    return assertNode(UUID.fromString(id));
  }

  public LoomNode assertNode(UUID id, String type) {
    var node = assertNode(id);
    if (!node.getType().equals(type)) {
      throw new IllegalStateException("Node " + id + " is not of type " + type);
    }
    return node;
  }

  public LoomNode assertNode(String id, String type) {
    return assertNode(UUID.fromString(id), type);
  }

  /**
   * Create a new node builder, bound to this graph.
   *
   * @return the builder.
   */
  @Nonnull
  @CheckReturnValue
  public LoomNode.Builder nodeBuilder() {
    return LoomNode.builder().graph(this);
  }

  /**
   * Create a new node builder, bound to this graph.
   *
   * @param type the node type.
   * @return the builder.
   */
  @Nonnull
  @CheckReturnValue
  public LoomNode.Builder nodeBuilder(String type) {
    return nodeBuilder().type(type);
  }

  /**
   * Add a node to the graph.
   *
   * @param wrapper the node to add.
   * @return the added Node.
   */
  @Nonnull
  @CanIgnoreReturnValue
  @SuppressWarnings("ReferenceEquality")
  public LoomNode addNode(@Nonnull LoomNodeWrapper<?> wrapper) {
    var node = wrapper.unwrap();

    var nodeGraph = node.getGraph();
    if (nodeGraph != null && nodeGraph != this) {
      throw new IllegalArgumentException("Node already belongs to a graph: " + nodeGraph.getId());
    }

    var existingNode = getNode(node.getId());
    if (existingNode != null && existingNode != node) {
      throw new IllegalArgumentException("Graph already has node with id: " + node.getId());
    }

    node.setGraph(this);
    nodes.put(node.getId(), node);
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
  @CanIgnoreReturnValue
  public LoomNode addNode(@Nonnull LoomNode.Builder builder) {
    return addNode(builder.build());
  }

  /**
   * Remove a node from the graph.
   *
   * <p>Silently does nothing if the graph does not contain the node.</p>
   *
   * @param id the ID of the node to remove.
   * @return the removed node.
   */
  public LoomNode removeNode(@Nonnull UUID id) {
    var node = nodes.remove(id);
    if (node != null) {
      node.setGraph(null);
    }
    return node;
  }

  /**
   * Remove a node from the graph.
   *
   * <p>Silently does nothing if the graph does not contain the node.</p>
   *
   * @param id the ID of the node to remove.
   * @return the removed node.
   */
  @Nullable
  @CanIgnoreReturnValue
  public LoomNode removeNode(@Nonnull String id) {
    return removeNode(UUID.fromString(id));
  }

  /**
   * Remove a node from the graph which matches the id of the given node.
   *
   * <p>Silently does nothing if the graph does not contain the node.</p>
   *
   * @param node the node to remove.
   * @return the removed node.
   */
  @Nonnull
  @CanIgnoreReturnValue
  public LoomNode removeNode(@Nonnull LoomNodeWrapper<?> node) {
    return removeNode(node.unwrap().getId());
  }

  /**
   * Support namespace for serialization.
   */
  @UtilityClass
  public static class Serialization {

    /**
     * Jackson deserializer for {@link LoomNode} lists.
     */
    public final class NodeListToMapDeserializer
      extends MapValueListUtil.MapDeserializer<UUID, LoomNode> {

      public NodeListToMapDeserializer() {
        super(LoomNode.class, LoomNode::getId, HashMap::new);
      }
    }
  }
}
