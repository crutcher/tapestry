package loom.alt.densegraph;

import java.util.UUID;
import javax.annotation.Nullable;

public class ValidationError extends RuntimeException {
  public static final long serialVersionUID = 1L;

  @Nullable public final UUID nodeId;

  public ValidationError(String message) {
    super(message);
    nodeId = null;
  }

  public ValidationError(UUID nodeId, String message) {
    super(String.format("Node %s: %s", nodeId, message));
    this.nodeId = nodeId;
  }
}
