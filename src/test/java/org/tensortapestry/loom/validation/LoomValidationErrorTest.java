package org.tensortapestry.loom.validation;

import java.util.List;
import org.junit.Test;
import org.tensortapestry.loom.testing.BaseTestClass;

public class LoomValidationErrorTest extends BaseTestClass {

  @Test
  public void testConstructors() {
    var issue = ValidationIssue.builder("foo").summary("a test").build();
    var error = new LoomValidationError(issue);
    assertThat(error.getIssues()).hasSize(1);
    assertThat(error.getIssues().getFirst()).isEqualTo(issue);

    var error2 = new LoomValidationError(issue.toBuilder());
    assertThat(error2.getIssues()).hasSize(1);
    assertThat(error2.getIssues().getFirst()).isEqualTo(issue);

    var error3 = new LoomValidationError(List.of(issue));
    assertThat(error3.getIssues()).hasSize(1);
    assertThat(error3.getIssues().getFirst()).isEqualTo(issue);
  }
}
