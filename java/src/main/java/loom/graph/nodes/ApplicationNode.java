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
import loom.graph.LoomGraph;
import loom.zspace.ZRange;

@Jacksonized
@SuperBuilder
@Getter
@Setter
public class ApplicationNode extends LoomGraph.Node<ApplicationNode, ApplicationNode.Body> {
  public static final String TYPE = "ApplicationNode";

  public abstract static class ApplicationNodeBuilder<
          C extends ApplicationNode, B extends ApplicationNodeBuilder<C, B>>
      extends NodeBuilder<ApplicationNode, Body, C, B> {
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
          }
      },
      "required": ["operationId"],
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

  @Value
  @Jacksonized
  @Builder
  public static class TensorSelection {
    UUID tensorId;
    ZRange range;
  }

  public static ApplicationNodeBuilder<?, ?> withBody(Consumer<Body.BodyBuilder> cb) {
    var bodyBuilder = Body.builder();
    cb.accept(bodyBuilder);
    return builder().body(bodyBuilder.build());
  }

  @Delegate(excludes = {HasToJsonString.class})
  @Nonnull
  private Body body;
}
