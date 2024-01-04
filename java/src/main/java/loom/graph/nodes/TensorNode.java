package loom.graph.nodes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.Delegate;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.common.json.HasToJsonString;
import loom.common.json.WithSchema;
import loom.graph.LoomNode;
import loom.zspace.*;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

@Jacksonized
@SuperBuilder
@Getter
@Setter
public final class TensorNode extends LoomNode<TensorNode, TensorNode.Body> {
  public static final String TYPE = "TensorNode";

  public abstract static class TensorNodeBuilder<
          C extends TensorNode, B extends TensorNodeBuilder<C, B>>
      extends LoomNodeBuilder<TensorNode, Body, C, B> {
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
        "dtype": {
            "type": "string"
        },
        "range": { "$ref": "#/definitions/ZRange" }
      },
      "required": ["dtype", "range"],
      "definitions": {
          "ZRange": {
              "type": "object",
              "properties": {
                  "start": { "$ref": "#/definitions/ZPoint" },
                  "end": { "$ref": "#/definitions/ZPoint" }
              },
              "required": ["start", "end"],
              "additionalProperties": false
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
  @Data
  @Jacksonized
  @Builder
  public static final class Body implements HasDimension, HasToJsonString, HasSize {
    public static class BodyBuilder {
      /**
       * Helper to set the range via {@code ZRange.fromShape(shape)}.
       *
       * @param shape the shape to set the range to.
       * @return this builder.
       */
      @JsonIgnore
      public BodyBuilder shape(@Nonnull ZPoint shape) {
        return range(ZRange.fromShape(shape));
      }

      /**
       * Helper to set the range via {@code ZRange.fromShape(shape)}.
       *
       * @param shape the shape to set the range to.
       * @return this builder.
       */
      @JsonIgnore
      public BodyBuilder shape(@Nonnull ZTensor shape) {
        return range(ZRange.fromShape(shape));
      }

      /**
       * Helper to set the range via {@code ZRange.fromShape(shape)}.
       *
       * @param shape the shape to set the range to.
       * @return this builder.
       */
      @JsonIgnore
      public BodyBuilder shape(int... shape) {
        return range(ZRange.fromShape(shape));
      }
    }

    @ToString.Include @Nonnull private final String dtype;
    @ToString.Include @Nonnull private final ZRange range;

    @Override
    public int getNDim() {
      return getRange().getNDim();
    }

    @Override
    public int getSize() {
      return getRange().getSize();
    }

    @JsonIgnore
    public ZTensor getShape() {
      return getRange().getShape();
    }
  }

  public static TensorNodeBuilder<?, ?> withBody(Consumer<Body.BodyBuilder> cb) {
    var bodyBuilder = Body.builder();
    cb.accept(bodyBuilder);
    return builder().body(bodyBuilder.build());
  }

  /* TODO: Node template BodyBuilderType, reflection annotation?
  * permits withBody() family to be base class methods.
  public abstract static BodyBuilderType bodyBuilder();
   */

  @Delegate(excludes = {HasToJsonString.class})
  @Nonnull
  private Body body;
}
