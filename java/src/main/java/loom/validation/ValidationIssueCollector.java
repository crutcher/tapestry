package loom.validation;

/** Collects ValidationIssues. */
public interface ValidationIssueCollector {

  /** True if the collector has failed. */
  boolean hasFailed();

  /**
   * Add an issue to the collector.
   *
   * @param issue the issue to add.
   */
  void add(ValidationIssue issue);

  /**
   * Add an issue to the collector.
   *
   * @param issueBuilder the issue builder to add.
   */
  default void add(ValidationIssue.ValidationIssueBuilder issueBuilder) {
    add(issueBuilder.build());
  }
}
