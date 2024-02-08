package org.tensortapestry.zspace.indexing;

import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
public class NewAxis extends Selector {

  int size;

  public NewAxis() {
    this(1);
  }

  public NewAxis(int size) {
    if (size < 1) {
      throw new IllegalArgumentException("Size must be greater than 0: " + size);
    }
    this.size = size;
  }

  @Override
  public String toString() {
    if (size > 1) {
      return "+" + size;
    } else {
      return "+";
    }
  }
}
