package org.tensortapestry.loom.zspace;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface HasZTensor {
  @JsonIgnore
  ZTensor getTensor();
}
