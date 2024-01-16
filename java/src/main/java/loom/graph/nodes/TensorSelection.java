package loom.graph.nodes;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import loom.zspace.ZRange;

import javax.annotation.Nonnull;
import java.util.UUID;

/** Describes the sub-range of a tensor that is selected by an application node. */
@Value
@Jacksonized
@Builder
@RequiredArgsConstructor
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
    return TensorSelection.builder()
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
          String.format("TensorNode %s does not contain range %s", tensorNode.getRange(), range));
    }
    return TensorSelection.builder().tensorId(tensorNode.getId()).range(range).build();
  }

  @Nonnull UUID tensorId;
  @Nonnull ZRange range;
}
