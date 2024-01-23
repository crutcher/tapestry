package org.tensortapestry.loom.validation;

import java.util.List;

/** A formatter for ValidationIssues. */
public interface ValidationIssueFormatter {
  /**
   * Format a list of issues as a string.
   *
   * @param issues the issues.
   * @return the formatted string.
   */
  String formatIssueList(List<ValidationIssue> issues);

  /**
   * Format the issue as a string.
   *
   * @param issue the issue.
   * @return the formatted string.
   */
  String formatIssue(ValidationIssue issue);

  /**
   * Format the context as a string.
   *
   * @param context the context.
   * @return the formatted string.
   */
  String formatContext(ValidationIssue.Context context);
}
