package org.tensortapestry.loom.graph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.*;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.UtilityClass;
import org.tensortapestry.loom.common.json.*;
import org.tensortapestry.loom.validation.ValidationIssueCollector;

/** A Loom Graph document. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Getter
@Setter
public final class LoomGraph implements HasToJsonString, IterableStreamable<LoomNode> {

  @Nullable @JsonIgnore
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

  public void validate() {
    assertEnv().validateGraph(this);
  }

  public void validate(ValidationIssueCollector issueCollector) {
    assertEnv().validateGraph(this, issueCollector);
  }

  @Override
  @NotNull @Nonnull
  public Iterator<LoomNode> iterator() {
    return nodes.values().iterator();
  }

  @Override
  @Nonnull
  public Stream<LoomNode> stream() {
    return nodes.values().stream();
  }

  @Nonnull
  public IterableStreamable<LoomNode> byType(String type) {
    return () -> nodes.values().stream().filter(node -> node.getType().equals(type)).iterator();
  }

  public LoomEnvironment assertEnv() {
    if (env == null) {
      throw new IllegalStateException("Environment not set");
    }
    return env;
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
  @Nullable public LoomNode getNode(UUID id) {
    return nodes.get(id);
  }

  /**
   * Get the node with the given ID.
   *
   * @param id the ID of the node to get.
   * @return the node, or null if not found.
   */
  @Nullable public LoomNode getNode(String id) {
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
   * @return the builder.
   */
  @Nonnull
  public LoomNode.LoomNodeBuilder nodeBuilder() {
    return LoomNode.builder().graph(this);
  }

  /**
   * Create a new node builder, bound to this graph.
   * @param type the node type.
   * @return the builder.
   */
  @Nonnull
  public LoomNode.LoomNodeBuilder nodeBuilder(String type) {
    return nodeBuilder().type(type);
  }

  /**
   * Add a node to the graph.
   *
   * @param wrapper the node to add.
   * @return the added Node.
   */
  @Nonnull
  public LoomNode addNode(@Nonnull NodeWrapper wrapper) {
    var node = wrapper.unwrap();

    {
      var nodeGraph = node.getGraph();
      if (nodeGraph != null && nodeGraph != this) {
        throw new IllegalArgumentException("Node already belongs to a graph: " + nodeGraph.getId());
      }
    }
    {
      var existingNode = getNode(node.getId());
      if (existingNode != null && existingNode != node) {
        throw new IllegalArgumentException("Graph already has node with id: " + node.getId());
      }
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
  public LoomNode addNode(@Nonnull LoomNode.LoomNodeBuilder builder) {
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
  @Nullable @CanIgnoreReturnValue
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
  public LoomNode removeNode(@Nonnull NodeWrapper node) {
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
