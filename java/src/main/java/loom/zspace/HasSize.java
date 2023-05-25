package loom.zspace;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface HasSize {
  /** Returns the number of elements in this object. */
  int size();

  /** Returns true if this object contains no elements. */
  @JsonIgnore
  default boolean isEmpty() {
    return size() == 0;
  }
}
