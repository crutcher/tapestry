package loom.graph.validation;

import loom.testing.BaseTestClass;
import org.junit.Test;

public class ValidationIssueCollectorTest extends BaseTestClass {
  @Test
  public void testEmpty() {
    var collector = new ValidationIssueCollector();
    assertThat(collector.getIssues()).isNull();
    collector.check();
  }

  @Test
  public void testAdd() {
    var collector = new ValidationIssueCollector();
    collector.add(ValidationIssue.builder("foo").summary("a test").build());
    collector.add(ValidationIssue.builder("bar").summary("xyz").build());

    assertThat(collector.getIssues()).hasSize(2);

    assertThat(collector.toDisplayString())
        .isEqualTo(
            """
        Validation failed with 2 issues:

        * Error [foo]: a test

        * Error [bar]: xyz
        """);

    assertThatExceptionOfType(LoomValidationError.class).isThrownBy(collector::check);
  }
}
