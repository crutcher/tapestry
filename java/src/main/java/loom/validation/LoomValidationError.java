package loom.validation;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** An error that contains a list of ValidationIssues. */
public class LoomValidationError extends RuntimeException {
  private static final long serialVersionUID = -7423226360443690951L;

  @Getter @NotNull private final List<ValidationIssue> issues;

  public LoomValidationError(@NotNull List<ValidationIssue> issues) {
    super(ValidationIssue.issuesToDisplayString(issues));
    this.issues = List.copyOf(issues);
  }
}
