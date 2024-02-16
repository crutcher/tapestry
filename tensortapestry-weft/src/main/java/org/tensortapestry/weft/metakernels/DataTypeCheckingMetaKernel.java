package org.tensortapestry.weft.metakernels;

import java.util.Set;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class DataTypeCheckingMetaKernel extends MetaKernel {

  protected final Set<String> dataTypes;

  protected void checkDataType(String dataType) {
    if (!dataTypes.contains(dataType)) {
      throw new IllegalArgumentException(
        "Unexpected dtype %s, expected one of %s".formatted(dataType, dataTypes)
      );
    }
  }
}
