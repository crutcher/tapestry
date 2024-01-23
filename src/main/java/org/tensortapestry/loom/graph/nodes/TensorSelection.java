package org.tensortapestry.loom.graph.nodes;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.UUID;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
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
  public static TensorSelection from(TensorNode tensorNode) {
    return TensorSelection
      .builder()
      .tensorId(tensorNode.getId())
      .range(tensorNode.getRange())
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
  public static TensorSelection from(TensorNode tensorNode, ZRange range) {
    if (!tensorNode.getRange().contains(range)) {
      throw new IllegalArgumentException(
        String.format("TensorNode %s does not contain range %s", tensorNode.getRange(), range)
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
