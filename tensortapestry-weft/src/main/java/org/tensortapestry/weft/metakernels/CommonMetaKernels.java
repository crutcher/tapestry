package org.tensortapestry.weft.metakernels;

import java.util.Set;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CommonMetaKernels {

  public final MetaKernel ADD = new CellWiseAccumulatorMetaKernel(
    "op:add",
    Set.of("int32", "int64", "float32", "float64")
  );
}
