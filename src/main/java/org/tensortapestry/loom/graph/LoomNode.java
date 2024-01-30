package org.tensortapestry.loom.graph;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.tensortapestry.common.json.HasToJsonString;
import org.tensortapestry.common.json.JsonViewWrapper;
import org.tensortapestry.common.json.ViewConversionError;
import org.tensortapestry.common.validation.ValidationIssue;

/**
 * A node in a Loom Graph.
 */
@Data
@Jacksonized
@Builder(builderClassName = "Builder")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public final class LoomNode implements LoomNodeWrapper, HasToJsonString {

  /**
   * Builder for a LoomNode.
   */
  public static class Builder {

    /**
     * Set the ID of the node.
     *
     * @param value the ID.
     * @return {@code this}
     */
    @Nonnull
    public Builder id(@Nonnull UUID value) {
      this.id = value;
      return this;
    }

    /**
     * Set the body of the node.
     *
     * @param value the body.
     * @return {@code this}
     */
    @Nonnull
    @JsonSetter
    public Builder body(@Nonnull Object value) {
      this.body = JsonViewWrapper.of(value);
      return this;
    }

    /**
     * Set the body of the node.
     * @param supplier supplier for the body.
     * @return {@code this}
     */
    @Nonnull
    public Builder body(@Nonnull Supplier<Object> supplier) {
      this.body = JsonViewWrapper.of(supplier);
      return this;
    }

    /**
     * Add an annotation to the node.
     *
     * @param type the type of the annotation.
     * @param value the value of the annotation.
     * @return {@code this}
     */
    @Nonnull
    public Builder annotation(@Nonnull String type, @Nonnull Object value) {
      if (this.annotations == null) {
        annotations(new HashMap<>());
      }
      this.annotations.put(type, JsonViewWrapper.of(value));
      return this;
    }

    /**
     * Add all the given annotations to the node.
     *
     * @param annotations the annotations to add.
     * @return {@code this}
     */
    @Nonnull
    public Builder withAnnotations(@Nonnull Map<String, Object> annotations) {
      annotations.forEach(this::annotation);
      return this;
    }

    /**
     * Build the node.
     *
     * <p>If the builder has an attached graph, the node will be added to the graph.
     *
     * <p>if the builder has an attached graph, and no id is set,
     * a random UUID will be assigned to the node from the graph.
     *
     * @return the built node.
     */
    @Nonnull
    public LoomNode build() {
      if (this.annotations == null) {
        this.annotations = new HashMap<>();
      }
      if (this.id == null && this.graph != null) {
        this.id = this.graph.genNodeId();
      }

      var node = new LoomNode(
        this.graph,
        Objects.requireNonNull(this.id, "id"),
        this.type,
        this.label,
        this.body,
        this.annotations
      );
      if (this.graph != null) {
        this.graph.addNode(node);
      }
      return node;
    }
  }

  /**
   * The graph that this node belongs to.
   */
  @JsonIgnore
  @Nullable private LoomGraph graph;

  /**
   * The ID of the node.
   */
  @Nonnull
  private final UUID id;

  /**
   * The type of the node.
   */
  @Nonnull
  public final String type;

  /**
   * The label of the node.
   */
  @Nullable private String label;

  /**
   * The body of the node.
   */
  @Nonnull
  private final JsonViewWrapper body;

  /**
   * The annotations of the node.
   */
  @Nonnull
  private final Map<String, JsonViewWrapper> annotations;

  @Override
  @Nonnull
  public LoomNode unwrap() {
    return this;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (other == null) return false;
    if (other instanceof LoomNodeWrapper wrapper) {
      other = wrapper.unwrap();
    }
    if (other instanceof LoomNode node) {
      return (
        Objects.equals(node.id, id) &&
        Objects.equals(node.type, type) &&
        Objects.equals(node.label, label) &&
        Objects.equals(node.body, body) &&
        Objects.equals(node.annotations, annotations)
      );
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, type, label, body, annotations);
  }

  /**
   * Get the graph that this node belongs to.
   *
   * @return the graph.
   * @throws IllegalStateException if the node does not belong to a graph.
   */
  @Nonnull
  public LoomGraph assertGraph() {
    if (graph == null) {
      throw new IllegalStateException("Node does not belong to a graph: " + id);
    }
    return graph;
  }

  /**
   * Get the environment that this node belongs to.
   * @return the environment.
   * @throws IllegalStateException if the node does not belong to an environment.
   */
  public LoomEnvironment assertEnvironment() {
    return assertGraph().assertEnv();
  }

  /**
   * Assert that the node has the given type.
   *
   * @param type the type to check.
   * @return {@code this}
   * @throws IllegalStateException if the node does not have the given type.
   */
  public LoomNode assertType(String type) {
    if (!this.type.equals(type)) {
      throw new IllegalStateException("Node type does not match: " + id);
    }
    return this;
  }

  /**
   * Get the type alias of the node.
   * @return the type alias.
   * @throws IllegalStateException if the node does not belong to an environment.
   */
  @JsonIgnore
  public String getTypeAlias() {
    return assertEnvironment().getTypeAlias(type);
  }

  /**
   * Get the json path of the node.
   * @return the json path.
   */
  @JsonIgnore
  public String getJsonPath() {
    return "$.nodes['" + id + "']";
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

  /**
   * View the body of the node as the given type.
   * <p>Equivalent to {@code this.getBody().viewAs(clazz)}</p>
   *
   * @param clazz the type to view the body as.
   * @param <T> the type to view the body as.
   * @return the body.
   * @throws ViewConversionError if the body cannot be converted to the given type.
   */
  public <T> T viewBodyAs(@Nonnull Class<T> clazz) {
    return body.viewAs(clazz);
  }

  /**
   * View the body of the node as a JsonNode.
   * <p>Equivalent to {@code this.getBody().viewAsJsonNode()}</p>
   *
   * @return the body.
   */
  @Nonnull
  public JsonNode viewBodyAsJsonNode() {
    return body.viewAsJsonNode();
  }

  /**
   * Does this node have an annotation with the given type?
   *
   * @param type the type to check.
   * @return true if the node has an annotation with the given type.
   */
  public boolean hasAnnotation(@Nonnull String type) {
    return annotations.containsKey(type);
  }

  /**
   * View the annotation with the given type as the given type.
   * <p>Equivalent to {@code this.getAnnotation(type).viewAs(clazz)}</p>
   *
   * @param type the type of the annotation.
   * @param clazz the type to view the annotation as.
   * @param <T> the type to view the annotation as.
   * @return the annotation.
   * @throws ViewConversionError if the annotation cannot be converted to the given type.
   */
  public <T> T viewAnnotationAs(@Nonnull String type, @Nonnull Class<T> clazz) {
    return annotations.get(type).viewAs(clazz);
  }

  /**
   * View the annotation with the given type as a JsonNode.
   * <p>Equivalent to {@code this.getAnnotation(type).viewAsJsonNode()}</p>
   *
   * @param type the type of the annotation.
   * @return the annotation.
   */
  @Nonnull
  public JsonNode viewAnnotationAsJsonNode(String type) {
    return annotations.get(type).viewAsJsonNode();
  }

  /**
   * Add an annotation to the node.
   *
   * @param type the type of the annotation.
   * @param value the value of the annotation.
   */
  public void addAnnotation(@Nonnull String type, @Nonnull Object value) {
    annotations.put(type, JsonViewWrapper.of(value));
  }

  /**
   * Remove an annotation from the node.
   *
   * @param type the type of the annotation.
   */
  public void removeAnnotation(@Nonnull String type) {
    annotations.remove(type);
  }
}
