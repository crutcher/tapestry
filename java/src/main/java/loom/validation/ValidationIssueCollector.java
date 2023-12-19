package loom.validation;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import loom.common.json.HasToJsonString;
import org.jetbrains.annotations.Nullable;

/** Collects ValidationIssues. */
@Getter
public class ValidationIssueCollector implements HasToJsonString {
  @JsonValue @Nullable private List<ValidationIssue> issues;

  /**
   * Check if the collector is empty.
   *
   * @return true if the collector is empty.
   */
  public boolean isEmpty() {
    return issues == null || issues.isEmpty();
  }

  /**
   * Format the issues.
   *
   * @return the formatted issues.
   */
  public String toDisplayString() {
    return ValidationIssue.issuesToDisplayString(issues);
  }

  /**
   * Add an issue to the collector.
   *
   * @param issue the issue to add.
   */
  public void add(ValidationIssue issue) {
    if (issues == null) {
      issues = new ArrayList<>();
    }
    issues.add(issue);
  }

  /**
   * Add an issue to the collector.
   *
   * @param issueBuilder the issue builder to add.
   */
  public void add(ValidationIssue.ValidationIssueBuilder issueBuilder) {
    add(issueBuilder.build());
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
