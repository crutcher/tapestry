package loom.matrix;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import loom.zspace.ZRange;

@Jacksonized
@Data
@Builder
public class BlockIndex {
  private final ZRange range;
}
