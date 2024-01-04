package loom.graph.nodes;

import java.util.UUID;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import loom.zspace.ZRange;

/** Describes the sub-range of a tensor that is selected by an application node. */
@Value
@Jacksonized
@Builder
@RequiredArgsConstructor
public class TensorSelection {
  public static TensorSelection from(TensorNode tensorNode) {
    return TensorSelection.builder()
        .tensorId(tensorNode.getId())
        .range(tensorNode.getEffectiveRange())
        .build();
  }

  @Nonnull UUID tensorId;
  @Nonnull ZRange range;
}
