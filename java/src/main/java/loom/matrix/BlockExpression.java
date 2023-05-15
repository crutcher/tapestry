package loom.matrix;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import loom.BlockOp;

@Jacksonized
@Data
@Builder
public class BlockExpression {
  private final BlockOp op;
  private final BlockIndex index;

  // input parameters

  // output parameters
}
