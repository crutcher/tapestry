package org.tensortapestry.loom.graph.dialects.tensorops;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import lombok.*;
import lombok.experimental.Delegate;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.tensortapestry.loom.common.json.HasToJsonString;
import org.tensortapestry.loom.graph.LoomNode;
import org.tensortapestry.loom.graph.dialects.common.JsdType;
import org.tensortapestry.loom.zspace.*;

@Jacksonized
@SuperBuilder
@Getter
@Setter
public final class TensorNode extends LoomNode<TensorNode, TensorNode.Body> {

  @SuppressWarnings("unused")
  public abstract static class TensorNodeBuilder<
    C extends TensorNode, B extends TensorNodeBuilder<C, B>
  >
    extends LoomNodeBuilder<TensorNode, Body, C, B> {
    {
      // Set the node type.
      type(TensorOpNodes.TENSOR_NODE_TYPE);
    }
  }

  @Value
  @Jacksonized
  @Builder
  @JsdType(TensorOpNodes.TENSOR_NODE_TYPE)
  @ToString(onlyExplicitlyIncluded = true)
  public static class Body implements HasDimension, HasToJsonString, HasSize {

    public static class BodyBuilder {

      /**
       * Helper to set the range via {@code ZRange.fromShape(shape)}.
       *
       * @param shape the shape to set the range to.
       * @return this builder.
       */
      @JsonIgnore
      public BodyBuilder shape(int... shape) {
        return range(ZRange.newFromShape(shape));
      }

      /**
       * Helper to set the range via {@code ZRange.fromShape(shape)}.
       *
       * @param shape the shape to set the range to.
       * @return this builder.
       */
      @JsonIgnore
      public BodyBuilder shape(@Nonnull HasZTensor shape) {
        return range(ZRange.newFromShape(shape));
      }
    }

    @ToString.Include
    @Nonnull
    String dtype;

    @ToString.Include
    @Nonnull
    ZRange range;

    @Override
    public int getNDim() {
      return getRange().getNDim();
    }

    @Override
    public int getSize() {
      return getRange().getSize();
    }

    @JsonIgnore
    public ZPoint getShape() {
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

  @Delegate(excludes = { HasToJsonString.class })
  @Nonnull
  private Body body;
}
