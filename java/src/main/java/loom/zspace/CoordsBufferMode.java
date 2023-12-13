package loom.zspace;

import java.util.Iterator;

public enum CoordsBufferMode {
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
  SAFE,
}