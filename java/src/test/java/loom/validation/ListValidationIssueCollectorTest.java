package loom.validation;

import loom.testing.BaseTestClass;
import org.junit.Test;

public class ListValidationIssueCollectorTest extends BaseTestClass {
  @Test
  public void testEmpty() {
    var collector = new ListValidationIssueCollector();
    assertThat(collector.getIssues()).isNull();
    collector.check();

    assertThat(collector.hasFailed()).isFalse();
  }

  @Test
  public void testAdd() {
    var collector = new ListValidationIssueCollector();
    collector.addIssue(ValidationIssue.builder("foo").summary("a test").build());
    collector.addIssue(ValidationIssue.builder("bar").summary("xyz"));
    assertThat(collector.hasFailed()).isTrue();

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
