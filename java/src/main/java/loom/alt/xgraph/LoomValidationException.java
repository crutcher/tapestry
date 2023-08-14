package loom.alt.xgraph;

public class LoomValidationException extends RuntimeException {
  public long serialVersionUID = 1L;

  public LoomValidationException(String message) {
    super(message);
  }

  public LoomValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
