package org.tensortapestry.loom.graph.dialects.tensorops;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.annotation.Nonnull;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Delegate;
import lombok.extern.jackson.Jacksonized;
import org.tensortapestry.loom.common.json.HasToJsonString;
import org.tensortapestry.loom.graph.AbstractNodeWrapper;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.LoomNode;
import org.tensortapestry.loom.graph.dialects.common.JsdType;
import org.tensortapestry.loom.zspace.*;

@JsdType(TensorNode.TYPE)
public final class TensorNode extends AbstractNodeWrapper<TensorNode.Body> {

  public static final String TYPE =
    "http://tensortapestry.org/schemas/loom/2024-01/node_types.jsd#/$defs/Tensor";

  public static final class Builder
    extends AbstractNodeWrapperBuilder<TensorNode, Builder, Body, Body.BodyBuilder> {

    private Builder() {
      super(TYPE, Body::builder, Body.BodyBuilder::build, TensorNode::wrap);
    }
  }

  @Value
  @Jacksonized
  @lombok.Builder
  @JsdType(TYPE)
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
      public BodyBuilder shape(@Nonnull ZTensorWrapper shape) {
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

  @Nonnull
  public static Builder builder() {
    return new Builder();
  }

  @Nonnull
  public static Builder builder(LoomGraph graph) {
    return new Builder().graph(graph);
  }

  @Nonnull
  public static TensorNode wrap(@Nonnull LoomNode node) {
    return new TensorNode(node);
  }

  public TensorNode(@Nonnull LoomNode loomNode) {
    super(loomNode.assertType(TYPE), Body.class);
  }

  @Delegate
  @SuppressWarnings("unused")
  private Body delegateBodyMethods() {
    return getBody();
  }
}
