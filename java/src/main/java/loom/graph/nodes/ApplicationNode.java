package loom.graph.nodes;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.*;
import lombok.experimental.Delegate;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.common.json.HasToJsonString;
import loom.common.json.WithSchema;
import loom.graph.LoomNode;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A node representing an application of an operation signature to a set of tensors.
 */
@Jacksonized
@SuperBuilder
@Getter
@Setter
public final class ApplicationNode extends LoomNode<ApplicationNode, ApplicationNode.Body> {

  /**
   * The node type.
   */
  public static final String TYPE = "ApplicationNode";

  /**
   * Extensions to the ApplicationNodeBuilder.
   * @param <C> the concrete ApplicationNode type.
   * @param <B> the concrete ApplicationNodeBuilder type.
   */
  @SuppressWarnings("unused")
  public abstract static class ApplicationNodeBuilder<
    C extends ApplicationNode, B extends ApplicationNodeBuilder<C, B>
  >
    extends LoomNodeBuilder<ApplicationNode, Body, C, B> {
    {
      // Set the node type.
      type(TYPE);
    }
  }

  /**
   * The body of the ApplicationNode.
   */
  @WithSchema(
    """
  {
      "type": "object",
      "properties": {
          "operationId": {
              "type": "string",
              "format": "uuid"
          },
          "inputs": { "$ref": "#/definitions/TensorSelectionMap" },
          "outputs": { "$ref": "#/definitions/TensorSelectionMap" }
      },
      "required": ["operationId"],
      "additionalProperties": false,
      "definitions": {
          "TensorSelectionMap": {
              "type": "object",
              "patternProperties": {
                  "^[a-zA-Z_][a-zA-Z0-9_]*$": {
                      "type": "array",
                      "items": { "$ref": "#/definitions/TensorSelection" },
                      "minItems": 1
                  }
              },
              "additionalProperties": false
          },
          "TensorSelection": {
              "type": "object",
              "properties": {
                  "tensorId": {
                      "type": "string",
                      "format": "uuid"
                  },
                  "range": { "$ref": "#/definitions/ZRange" }
              },
              "required": ["tensorId", "range"],
              "additionalProperties": false
          },
          "ZRange": {
              "type": "object",
              "properties": {
                 "start": { "$ref": "#/definitions/ZPoint" },
                 "end": { "$ref": "#/definitions/ZPoint" }
              },
              "required": ["start", "end"]
          },
          "ZPoint": {
              "type": "array",
              "items": {
                  "type": "integer"
              }
          }
      }
  }
  """
  )
  @Value
  @Jacksonized
  @Builder
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @JsonPropertyOrder({ "operationId", "inputs", "outputs" })
  public static class Body implements HasToJsonString {

    @Nonnull
    UUID operationId;

    @Singular
    @Nonnull
    Map<String, List<TensorSelection>> inputs;

    @Singular
    @Nonnull
    Map<String, List<TensorSelection>> outputs;
  }

  /**
   * Create an ApplicationNodeBuilder with the given body.
   *
   * <p>Example:
   *
   * <pre>{@code
   * var node = ApplicationNode.builder()
   *  .withBody(body -> body
   *    .input("x", List.of(
   *      new TensorSelection(tensor.getId(), tensor.getShape()))))
   *  .buildOnGraph(graph);
   * }</pre>
   *
   * @param cb the body builder callback.
   * @return the ApplicationNodeBuilder.
   */
  public static ApplicationNodeBuilder<?, ?> withBody(Consumer<Body.BodyBuilder> cb) {
    var bodyBuilder = Body.builder();
    cb.accept(bodyBuilder);
    return builder().body(bodyBuilder.build());
  }

  @Delegate(excludes = { HasToJsonString.class })
  @Nonnull
  private Body body;

  /**
   * Get the operation signature node.
   * @return the operation signature node.
   */
  public OperationSignatureNode getOperationSignatureNode() {
    return assertGraph()
      .assertNode(getOperationId(), OperationSignatureNode.TYPE, OperationSignatureNode.class);
  }
}
