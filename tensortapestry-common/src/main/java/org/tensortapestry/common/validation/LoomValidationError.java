package org.tensortapestry.common.validation;

import java.io.Serial;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.Getter;

@Getter
public final class LoomValidationError extends RuntimeException {

  @Serial
  private static final long serialVersionUID = -7423226360443690951L;

  @Nonnull
  private final List<ValidationIssue> issues;

  public LoomValidationError(@Nonnull List<ValidationIssue> issues) {
    super(new ValidationIssueTextFormatter().formatIssueList(issues));
    this.issues = List.copyOf(issues);
  }

  public LoomValidationError(@Nonnull ValidationIssue issue) {
    this(List.of(issue));
  }

  public LoomValidationError(@Nonnull ValidationIssue.ValidationIssueBuilder issueBuilder) {
    this(issueBuilder.build());
  }
}
