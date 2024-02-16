package org.tensortapestry.weft.metakernels;

import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CommonMetaKernels {

  public static final Set<String> DATA_TYPES = Set.of("int32", "int64", "float32", "float64");
  public final MetaKernel ADD = new CellWiseAccumulatorMetaKernel("op:add", DATA_TYPES);

  public final MetaKernel LINEAR = new LinearMetaKernel(DATA_TYPES);
}
