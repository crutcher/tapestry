package loom.graph;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import loom.common.json.HasToJsonString;
import loom.common.json.JsonUtil;
import loom.common.runtime.ReflectionUtils;
import loom.validation.ValidationIssue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.UUID;

/**
 * Base class for a node in the graph.
 *
 * @param <NodeType> the node subclass.
 * @param <BodyType> the node body class.
 */
@Getter
@Setter
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonSerialize(using = LoomNode.Serialization.NodeSerializer.class)
public abstract class LoomNode<NodeType extends LoomNode<NodeType, BodyType>, BodyType>
    implements HasToJsonString {

  /**
   * Extensions to the auto-generated {@code @Builder} annotation for LoomNode.
   *
   * @param <NodeType> the node subclass.
   * @param <BodyType> the node body class.
   * @param <C> a generic extension of the node subclass.
   * @param <B> the builder subclass.
   */
  @SuppressWarnings("unused")
  public abstract static class LoomNodeBuilder<
      NodeType extends LoomNode<NodeType, BodyType>,
      BodyType,
      C extends LoomNode<NodeType, BodyType>,
      B extends LoomNodeBuilder<NodeType, BodyType, C, B>> {

    /**
     * Complete the build of the node on the graph.
     *
     * <p>Equivalent to {@code buildOn(graph)}.
     *
     * <p>Assigns a random UUID to the node if the node does not have an ID.
     *
     * @param graph the node type.
     * @return this builder.
     */
    public NodeType buildOn(LoomGraph graph) {
      return graph.addNode(this);
    }

    /**
     * Does the builder have an ID?
     *
     * @return true if the builder has an ID.
     */
    public final boolean hasId() {
      return id != null;
    }
  }

  /**
   * Build a {@link ValidationIssue.Context} for this node.
   *
   * @param name the name of the context.
   * @return the context.
   */
  public ValidationIssue.Context asValidationContext(String name) {
    return asValidationContext(name, null);
  }

  /**
   * Build a {@link ValidationIssue.Context} for this node.
   *
   * @param name the name of the context.
   * @param message the message of the context.
   * @return the context.
   */
  public ValidationIssue.Context asValidationContext(String name, @Nullable String message) {
    var builder = ValidationIssue.Context.builder().name(name).jsonpath(getJsonPath()).data(this);

    if (message != null) {
      builder.message(message);
    }

    return builder.build();
  }

  @Nonnull private final UUID id;
  @Nonnull private final String type;
  @JsonIgnore @Nullable private LoomGraph graph;
  @Nullable private String label;

  @JsonIgnore
  public final String getJsonPath() {
    return "$.nodes[@.id=='%s']".formatted(getId());
  }

  @Override
  public final String toString() {
    return "%s%s".formatted(getClass().getSimpleName(), toJsonString());
  }

  /**
   * Get the graph that this node belongs to.
   *
   * @return the graph.
   * @throws IllegalStateException if the node does not belong to a graph.
   */
  public final LoomGraph assertGraph() {
    if (graph == null) {
      throw new IllegalStateException("Node does not belong to a graph: " + id);
    }
    return graph;
  }

  /** Get the class type of the node body. */
  @JsonIgnore
  public final Class<BodyType> getBodyClass() {
    @SuppressWarnings("unchecked")
    var cls = (Class<BodyType>) getBodyClass((Class<? extends LoomNode<?, ?>>) getClass());
    return cls;
  }

  /**
   * Get the class type of the node body.
   *
   * <p>Introspects the node type class parameters to get the body type class.
   *
   * @param nodeTypeClass the node type class.
   * @return the body class.
   */
  public static Class<?> getBodyClass(Class<? extends LoomNode<?, ?>> nodeTypeClass) {
    return (Class<?>)
        ReflectionUtils.getTypeArgumentsForGenericSuperclass(nodeTypeClass, LoomNode.class)[1];
  }

  @Nonnull
  public abstract BodyType getBody();

  /**
   * Get the node body as a JSON string.
   *
   * @return the JSON string.
   */
  public final String getBodyAsJson() {
    return JsonUtil.toPrettyJson(getBody());
  }

  /**
   * Get the node body as a JSON tree.
   *
   * @return the JSON tree.
   */
  public final JsonNode getBodyAsJsonNode() {
    return JsonUtil.valueToJsonNodeTree(getBody());
  }

  public abstract void setBody(@Nonnull BodyType body);

  /**
   * Set the node body from a JSON string.
   *
   * @param json the JSON string.
   */
  public final void setBodyFromJson(String json) {
    setBody(JsonUtil.fromJson(json, getBodyClass()));
  }

  /**
   * Set the node body from a JSON tree.
   *
   * @param tree the JSON tree; either a {@code JsonNode} or a {@code Map<String, Object>}.
   */
  public final void setBodyFromValue(Object tree) {
    setBody(JsonUtil.convertValue(tree, getBodyClass()));
  }

  /**
   * Subclass type helper.
   *
   * @return this, cast to the subclass {@code NodeType} type.
   */
  @SuppressWarnings("unchecked")
  public final NodeType self() {
    return (NodeType) this;
  }

  @NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
  public static final class Serialization {
    /**
     * A Jackson serializer for Node. We use a custom serializer because {@code @Delegate} applied
     * to a method in subclasses to delegate the type methods of {@code body} does not honor
     * {@code @JsonIgnore}, and we otherwise generate data fields for every getter in the body.
     *
     * @param <B> the type of the node body.
     */
    public static final class NodeSerializer<N extends LoomNode<N, B>, B>
        extends JsonSerializer<LoomNode<N, B>> {
      @Override
      public void serialize(LoomNode<N, B> value, JsonGenerator gen, SerializerProvider serializers)
          throws IOException {
        gen.writeStartObject();
        gen.writeStringField("id", value.getId().toString());
        gen.writeStringField("type", value.getType());

        var label = value.getLabel();
        if (label != null) {
          gen.writeStringField("label", label);
        }

        gen.writeObjectField("body", value.getBody());
        gen.writeEndObject();
      }
    }
  }
}
