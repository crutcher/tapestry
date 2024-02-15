package org.tensortapestry.weft.metakernels;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CommonMetaKernels {
  public final MetaKernel ADD = new CellwiseGroupOp("op:add");
}
