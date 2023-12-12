package loom.zspace;

import java.util.Iterator;

public enum CoordsBufferMode {
  /** The buffer is shared between subsequent calls to {@link Iterator#next()}. */
  REUSED,

  /** The buffer is not shared between subsequent calls to {@link Iterator#next()}. */
  DISTINCT,
}
