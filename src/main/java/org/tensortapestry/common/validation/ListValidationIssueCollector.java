package org.tensortapestry.common.validation;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Getter;
import org.tensortapestry.common.json.HasToJsonString;

@Getter
public class ListValidationIssueCollector implements ValidationIssueCollector, HasToJsonString {

  @JsonValue
  @Nullable private List<ValidationIssue> issues;

  @Override
  public boolean hasFailed() {
    return issues != null && !issues.isEmpty();
  }

  @Override
  public void addIssue(ValidationIssue issue) {
    synchronized (this) {
      if (issues == null) {
        issues = new ArrayList<>();
      }
      issues.add(issue);
    }
  }

  /**
   * Format the issues.
   *
   * @return the formatted issues.
   */
  public String toDisplayString() {
    return new ValidationIssueTextFormatter().formatIssueList(issues);
  }

  /**
   * Throw a LoomValidationError if there are any issues.
   *
   * @throws LoomValidationError if there are any issues.
   */
  public void check() throws LoomValidationError {
    if (issues != null && !issues.isEmpty()) {
      throw new LoomValidationError(issues);
    }
  }
}
