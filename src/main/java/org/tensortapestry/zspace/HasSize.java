package org.tensortapestry.zspace;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * An object that has a size.
 */
public interface HasSize {
  /**
   * Returns the number of elements in this object.
   */
  @JsonIgnore
  int getSize();

  /**
   * Returns true if this object contains no elements.
   */
  @JsonIgnore
  default boolean isEmpty() {
    return getSize() == 0;
  }
}
