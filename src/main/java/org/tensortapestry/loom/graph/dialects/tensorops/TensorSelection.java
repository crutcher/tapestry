package org.tensortapestry.loom.graph.dialects.tensorops;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.UUID;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.tensortapestry.loom.graph.LoomNodeWrapper;
import org.tensortapestry.loom.zspace.ZRange;

/** Describes the sub-range of a tensor that is selected by an application node. */
@Value
@Jacksonized
@Builder
@RequiredArgsConstructor
@JsonPropertyOrder({ "tensorId", "range" })
public class TensorSelection {

  /**
   * Creates a TensorSelection from a TensorNode.
   *
   * <p>The TensorSelection will have the same range as the TensorNode.
   *
   * @param tensorNode the TensorNode
   * @return the TensorSelection
   */
  public static TensorSelection from(LoomNodeWrapper tensorNode) {
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
   * <p>The TensorSelection will have the given range, which must be contained in the TensorNode's
   * range.
   *
   * @param tensorNode the TensorNode
   * @param range the range
   * @return the TensorSelection
   */
  public static TensorSelection from(LoomNodeWrapper tensorNode, ZRange range) {
    tensorNode.unwrap().assertType(TensorNode.TYPE);
    var tensorRange = tensorNode.unwrap().viewBodyAs(TensorNode.Body.class).getRange();
    if (!tensorRange.contains(range)) {
      throw new IllegalArgumentException(
        String.format("Tensor range %s does not contain selection range %s", tensorRange, range)
      );
    }
    return TensorSelection.builder().tensorId(tensorNode.getId()).range(range).build();
  }

  @JsonProperty(required = true)
  @Nonnull
  UUID tensorId;

  @JsonProperty(required = true)
  @Nonnull
  ZRange range;
}
