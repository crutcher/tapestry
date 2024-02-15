package org.tensortapestry.weft.metakernels;

import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CommonMetaKernels {

  public final MetaKernel ADD = new CellwiseGroupOp(
    "op:add",
    Set.of("int32", "int64", "float32", "float64")
  );
}
