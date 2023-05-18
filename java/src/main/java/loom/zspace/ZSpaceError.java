package loom.zspace;

/** The base class for all ZSpace errors. */
public class ZSpaceError extends Exception {
  ZSpaceError(String message) {
    super(message);
  }
}
