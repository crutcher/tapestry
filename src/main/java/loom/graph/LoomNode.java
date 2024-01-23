package loom.graph;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import loom.common.json.HasToJsonString;
import loom.common.json.JsonUtil;
import loom.common.runtime.ReflectionUtils;
import loom.validation.ValidationIssue;

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
@SuppressWarnings("unused") // NodeType
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
    B extends LoomNodeBuilder<NodeType, BodyType, C, B>
  > {

    /**
     * Set an annotation on the node.
     *
     * @param key the annotation key.
     * @param value the annotation value.
     * @return {@code this}
     */
    public B annotation(String key, Object value) {
      if (annotations$value == null) {
        annotations(new HashMap<>());
      }
      annotations$value.put(key, value);
      return self();
    }

    /**
     * Complete the build of the node on the graph.
     *
     * <p>Equivalent to {@code buildOn(graph)}.
     *
     * <p>Assigns a random UUID to the node if the node does not have an ID.
     *
     * @param graph the node type.
     * @return {@code this}
     */
    public NodeType addTo(LoomGraph graph) {
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

  @JsonIgnore
  @Nullable private LoomGraph graph;

  @Nonnull
  private final UUID id;

  @Nonnull
  private final String type;

  @Nullable private String label;

  @Builder.Default
  @Nonnull
  private final Map<String, Object> annotations = new HashMap<>();

  /**
   * Get the environment configured type alias for the node type.
   *
   * @return the type alias.
   */
  public String getTypeAlias() {
    return assertGraph().getEnv().getTypeAlias(getType());
  }

  /**
   * Does the node have an annotation with the given key?
   *
   * @param key the annotation key.
   * @return true if the node has the annotation.
   */
  public final boolean hasAnnotation(String key) {
    return annotations.containsKey(key);
  }

  /**
   * Does the node have an annotation with the given key and class?
   *
   * @param key the annotation key.
   * @param cls the annotation class.
   * @return true if the node has the annotation with the given key and class.
   */
  public final boolean hasAnnotation(String key, Class<?> cls) {
    return (annotations.containsKey(key) && cls.isInstance(annotations.get(key)));
  }

  /**
   * Get an annotation from the node.
   *
   * @param key the annotation key.
   * @return the annotation value.
   */
  @Nullable public final Object getAnnotation(String key) {
    return annotations.get(key);
  }

  /**
   * Get an annotation from the node.
   *
   * @param key the annotation key.
   * @param cls the annotation class.
   * @return the annotation value.
   * @param <T> the annotation type.
   */
  @Nullable public final <T> T getAnnotation(@Nonnull String key, @Nonnull Class<? extends T> cls) {
    return cls.cast(annotations.get(key));
  }

  /**
   * Get an annotation from the node.
   *
   * @param key the annotation key.
   * @param cls the annotation class.
   * @return the annotation value.
   * @param <T> the annotation type.
   */
  @Nonnull
  public final <T> T assertAnnotation(@Nonnull String key, @Nonnull Class<? extends T> cls) {
    var value = getAnnotation(key, cls);
    if (value == null) {
      throw new IllegalStateException("Annotation not found: " + key);
    }
    return value;
  }

  /**
   * Remove an annotation from the node.
   *
   * @param key the annotation key.
   */
  public final void removeAnnotation(@Nonnull String key) {
    annotations.remove(key);
  }

  /**
   * Set an annotation on the node.
   *
   * @param key the annotation key.
   * @param value the annotation value.
   * @throws IllegalStateException if the annotation value is not assignable to the annotation.
   */
  public final void setAnnotation(@Nonnull String key, @Nonnull Object value) {
    annotations.put(key, value);
  }

  /**
   * Set an annotation on the node from a JSON string.
   *
   * <p>The annotation will be converted to the environment's type class for the annotation.
   *
   * @param key the annotation key.
   * @param json the JSON string.
   */
  public final void setAnnotationFromJson(@Nonnull String key, @Nonnull String json) {
    setAnnotation(key, JsonUtil.fromJson(json, assertGraph().getEnv().assertAnnotationClass(key)));
  }

  /**
   * Set an annotation on the node from an Object tree.
   *
   * <p>The annotation will be converted to the environment's type class for the annotation.
   *
   * @param key the annotation key.
   * @param value the Object tree.
   */
  public final void setAnnotationFromValue(@Nonnull String key, @Nonnull Object value) {
    setAnnotation(
      key,
      JsonUtil.convertValue(value, assertGraph().getEnv().assertAnnotationClass(key))
    );
  }

  /**
   * Does the node belong to a graph?
   *
   * @return true if the node belongs to a graph.
   */
  public final boolean hasGraph() {
    return graph != null;
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

  @JsonIgnore
  public final String getJsonPath() {
    return "$.nodes[@.id=='%s']".formatted(getId());
  }

  @Override
  public final String toString() {
    return "%s%s".formatted(getClass().getSimpleName(), toJsonString());
  }

  /**
   * Get the class type of the node body.
   *
   * @return the body class.
   */
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
    return (Class<?>) ReflectionUtils.getTypeArgumentsForGenericSuperclass(
      nodeTypeClass,
      LoomNode.class
    )[1];
  }

  /**
   * Get the node body.
   *
   * @return the node body.
   */
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
  public final ObjectNode getBodyAsJsonNode() {
    return (ObjectNode) JsonUtil.valueToJsonNodeTree(getBody());
  }

  /**
   * Set the node body.
   *
   * @param body the node body.
   */
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

  @NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
  public static final class Serialization {

    /**
     * A Jackson serializer for Node. We use a custom serializer because {@code @Delegate} applied
     * to a method in subclasses to delegate the type methods of {@code body} does not honor
     * {@code @JsonIgnore}, and we otherwise generate data fields for every getter in the body.
     *
     * @param <N> the node type.
     * @param <B> the type of the node body.
     */
    public static final class NodeSerializer<N extends LoomNode<N, B>, B>
      extends JsonSerializer<LoomNode<N, B>> {

      @Override
      public void serialize(
        LoomNode<N, B> value,
        JsonGenerator gen,
        SerializerProvider serializers
      ) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("id", value.getId().toString());
        gen.writeStringField("type", value.getType());

        var label = value.getLabel();
        if (label != null) {
          gen.writeStringField("label", label);
        }
        if (!value.getAnnotations().isEmpty()) {
          gen.writeObjectField("annotations", value.getAnnotations());
        }

        gen.writeObjectField("body", value.getBody());
        gen.writeEndObject();
      }
    }
  }
}
