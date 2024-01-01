package loom.graph.nodes;

import com.fasterxml.jackson.annotation.JsonInclude;
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
import loom.graph.LoomEnvironment;
import loom.graph.LoomNode;
import loom.graph.constraints.ApplicationNodeSelectionsAreWellFormedConstraint;
import loom.graph.constraints.ThereAreNoApplicationReferenceCyclesConstraint;
import loom.zspace.ZRange;

@Jacksonized
@SuperBuilder
@Getter
@Setter
@LoomEnvironment.WithConstraints({
  ThereAreNoApplicationReferenceCyclesConstraint.class,
  ApplicationNodeSelectionsAreWellFormedConstraint.class
})
public class ApplicationNode extends LoomNode<ApplicationNode, ApplicationNode.Body> {
  public static final String TYPE = "ApplicationNode";

  public abstract static class ApplicationNodeBuilder<
          C extends ApplicationNode, B extends ApplicationNodeBuilder<C, B>>
      extends LoomNodeBuilder<ApplicationNode, Body, C, B> {
    {
      // Set the node type.
      type(TYPE);
    }
  }

  @Value
  @Jacksonized
  @Builder
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
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class Body implements HasToJsonString {
    UUID operationId;
    @Singular @Nonnull Map<String, List<TensorSelection>> inputs;
    @Singular @Nonnull Map<String, List<TensorSelection>> outputs;
  }

  /** Describes the sub-range of a tensor that is selected by an application node. */
  @Value
  @Jacksonized
  @Builder
  @RequiredArgsConstructor
  public static class TensorSelection {
    @Nonnull UUID tensorId;
    @Nonnull ZRange range;
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
}
