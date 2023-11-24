package loom.validation;

import java.io.Serial;
import java.util.List;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public final class LoomValidationError extends RuntimeException {
  @Serial private static final long serialVersionUID = -7423226360443690951L;

  @NotNull private final List<ValidationIssue> issues;

  public LoomValidationError(@NotNull List<ValidationIssue> issues) {
    super(ValidationIssue.issuesToDisplayString(issues));
    this.issues = List.copyOf(issues);
  }
}
