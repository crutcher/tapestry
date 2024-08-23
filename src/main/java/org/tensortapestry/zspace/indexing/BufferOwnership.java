package org.tensortapestry.zspace.indexing;

import java.util.Iterator;

public enum BufferOwnership {
  /**
   * The buffer is shared between subsequent calls to {@link Iterator#next()}.
   *
   * <p>The caller does not own the buffer, and should not modify it.
   */
  REUSED,

  /**
   * The buffer is not shared between subsequent calls to {@link Iterator#next()}.
   *
   * <p>The caller owns the buffer, and may modify it.
   */
  CLONED;

  /**
   * Apply the buffer ownership policy to the given array.
   *
   * @param arr the array.
   * @return the array, possibly cloned.
   */
  public int[] apply(int[] arr) {
    if (this == CLONED) {
      arr = arr.clone();
    }
    return arr;
  }
}
