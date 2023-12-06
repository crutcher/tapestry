package loom.validation;

import java.util.List;
import loom.testing.BaseTestClass;
import org.junit.Test;

public class LoomValidationErrorTest extends BaseTestClass {
  @Test
  public void testConstructors() {
    var issue = ValidationIssue.builder("foo").summary("a test").build();
    var error = new LoomValidationError(issue);
    assertThat(error.getIssues()).hasSize(1);
    assertThat(error.getIssues().get(0)).isEqualTo(issue);

    var error2 = new LoomValidationError(issue.toBuilder());
    assertThat(error2.getIssues()).hasSize(1);
    assertThat(error2.getIssues().get(0)).isEqualTo(issue);

    var error3 = new LoomValidationError(List.of(issue));
    assertThat(error3.getIssues()).hasSize(1);
    assertThat(error3.getIssues().get(0)).isEqualTo(issue);
  }
}
