package loom.graph.nodes;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import lombok.*;
import lombok.experimental.Delegate;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.common.json.HasToJsonString;
import loom.common.json.WithSchema;
import loom.graph.LoomNode;

@Jacksonized
@SuperBuilder
@Getter
@Setter
public final class ApplicationNode extends LoomNode<ApplicationNode, ApplicationNode.Body> {
  public static final String TYPE = "ApplicationNode";

  @SuppressWarnings("unused")
  public abstract static class ApplicationNodeBuilder<
          C extends ApplicationNode, B extends ApplicationNodeBuilder<C, B>>
      extends LoomNodeBuilder<ApplicationNode, Body, C, B> {
    {
      // Set the node type.
      type(TYPE);
    }
  }

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
  """)
  @Value
  @Jacksonized
  @Builder
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public static class Body implements HasToJsonString {
    @JsonProperty(required = true)
    @Nonnull
    UUID operationId;

    @Singular @Nonnull Map<String, List<TensorSelection>> inputs;
    @Singular @Nonnull Map<String, List<TensorSelection>> outputs;
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

  @Delegate(excludes = {HasToJsonString.class})
  @Nonnull
  private Body body;

  public OperationSignatureNode getOperationSignatureNode() {
    return assertGraph()
        .assertNode(getOperationId(), OperationSignatureNode.TYPE, OperationSignatureNode.class);
  }
}
