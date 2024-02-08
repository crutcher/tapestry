package org.tensortapestry.zspace.indexing;

import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
public class Index extends Selector {

  int index;

  public Index(int index) {
    this.index = index;
  }

  @Override
  public String toString() {
    return Integer.toString(index);
  }
}
