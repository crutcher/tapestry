package org.tensortapestry.zspace.indexing;

import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
public class Ellipsis extends Selector {

  @Override
  public String toString() {
    return "...";
  }
}
