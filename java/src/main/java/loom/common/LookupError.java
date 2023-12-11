package loom.common;

public class LookupError extends RuntimeException {
  @java.io.Serial private static final long serialVersionUID = -2489308691886319272L;

  public LookupError(String msg) {
    super("Lookup failed: " + msg);
  }
}
