package org.tensortapestry.loom.graph.dialects.tensorops;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.UUID;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.tensortapestry.loom.graph.LoomNodeWrapper;
import org.tensortapestry.zspace.ZPoint;
import org.tensortapestry.zspace.ZRange;

/**
 * Describes the sub-range of a tensor that is selected by an application node.
 */
@Value
@Jacksonized
@Builder
@RequiredArgsConstructor
@JsonPropertyOrder({ "tensorId", "range" })
public class TensorSelection implements TensorSelectionSupplier {

  /**
   * Creates a TensorSelection from a TensorNode.
   *
   * <p>The TensorSelection will have the same range as the TensorNode.
   *
   * @param tensorNode the TensorNode
   * @return the TensorSelection
   */
  public static TensorSelection from(LoomNodeWrapper<?> tensorNode) {
    tensorNode.unwrap().assertType(TensorNode.TYPE);
    return TensorSelection
      .builder()
      .tensorId(tensorNode.getId())
      .range(tensorNode.unwrap().viewBodyAs(TensorNode.Body.class).getRange())
      .build();
  }

  /**
   * Creates a TensorSelection from a TensorNode and a range.
   *
   * <p>The TensorSelection will have the given range, which must be contained in the
   * TensorNode's range.
   *
   * @param tensorNode the TensorNode
   * @param range the range
   * @return the TensorSelection
   */
  public static TensorSelection from(LoomNodeWrapper<?> tensorNode, ZRange range) {
    tensorNode.unwrap().assertType(TensorNode.TYPE);
    var tensorRange = tensorNode.unwrap().viewBodyAs(TensorNode.Body.class).getRange();
    if (!tensorRange.contains(range)) {
      throw new IllegalArgumentException(
        String.format("Tensor range %s does not contain selection range %s", tensorRange, range)
      );
    }
    return TensorSelection.builder().tensorId(tensorNode.getId()).range(range).build();
  }

  @Nonnull
  UUID tensorId;

  @Nonnull
  ZRange range;

  @Override
  @JsonIgnore
  public int getNDim() {
    return range.getNDim();
  }

  @JsonIgnore
  public ZPoint getStart() {
    return range.getStart();
  }

  @JsonIgnore
  public ZPoint getEnd() {
    return range.getEnd();
  }

  @JsonIgnore
  public ZPoint getShape() {
    return range.getShape();
  }

  @Nonnull
  @JsonIgnore
  @Override
  public TensorSelection getTensorSelection() {
    return this;
  }
}
