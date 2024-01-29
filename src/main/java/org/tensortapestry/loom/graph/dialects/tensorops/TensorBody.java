package org.tensortapestry.loom.graph.dialects.tensorops;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.tensortapestry.loom.common.json.HasToJsonString;
import org.tensortapestry.loom.graph.dialects.common.JsdType;
import org.tensortapestry.loom.zspace.*;

@Value
@Jacksonized
@Builder
@JsdType(TensorOpNodes.TENSOR_NODE_TYPE)
@ToString(onlyExplicitlyIncluded = true)
public class TensorBody implements HasDimension, HasToJsonString, HasSize {

  public static class TensorBodyBuilder {

    /**
     * Helper to set the range via {@code ZRange.fromShape(shape)}.
     *
     * @param shape the shape to set the range to.
     * @return this builder.
     */
    @JsonIgnore
    public TensorBodyBuilder shape(int... shape) {
      return range(ZRange.newFromShape(shape));
    }

    /**
     * Helper to set the range via {@code ZRange.fromShape(shape)}.
     *
     * @param shape the shape to set the range to.
     * @return this builder.
     */
    @JsonIgnore
    public TensorBodyBuilder shape(@Nonnull HasZTensor shape) {
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
