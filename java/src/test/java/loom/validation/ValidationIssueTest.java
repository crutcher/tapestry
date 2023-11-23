package loom.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import loom.testing.BaseTestClass;
import org.junit.Test;

public class ValidationIssueTest extends BaseTestClass {
  @Test
  public void testContextFormat() {
    var context =
        ValidationIssue.Context.builder("Foo")
            .message("I like cheese\nand crackers")
            .jsonpath("$.foo", ".bar")
            .dataToJson(Map.of("foo", 2, "bar", 3))
            .build();

    var lines = new ArrayList<String>();
    lines.add("- Foo:: ($.foo.bar)");
    lines.add("  I like cheese");
    lines.add("  and crackers");
    lines.add("  |> {");
    lines.add("  |>   \"bar\" : 3,");
    lines.add("  |>   \"foo\" : 2");
    lines.add("  |> }");

    assertThat(context.toDisplayString()).isEqualTo(String.join("\n", lines));
  }

  @Test
  public void testIssueFormat() {
    var issue =
        ValidationIssue.builder()
            .type("foo")
            .param("type", "fooble")
            .param("foo", "bar")
            .summary("Foo bar")
            .message("    I like the night life\n    I like to boogie")
            .context(
                ValidationIssue.Context.builder("Foo")
                    .message("I like cheese\nand crackers")
                    .jsonpath("$.foo", ".bar")
                    .dataToJson(Map.of("foo", 2, "bar", 3))
                    .build())
            .context(
                ValidationIssue.Context.builder("Bar")
                    .message("I like cheese")
                    .jsonpath("$.foo", ".quux")
                    .dataToJson(List.of(12, 13))
                    .build())
            .build();

    var lines = new ArrayList<String>();
    lines.add("* Error [foo{foo=bar, type=fooble}]: Foo bar");
    lines.add("  I like the night life");
    lines.add("  I like to boogie");
    lines.add("");
    lines.add("  - Foo:: ($.foo.bar)");
    lines.add("    I like cheese");
    lines.add("    and crackers");
    lines.add("    |> {");
    lines.add("    |>   \"bar\" : 3,");
    lines.add("    |>   \"foo\" : 2");
    lines.add("    |> }");
    lines.add("");
    lines.add("  - Bar:: ($.foo.quux)");
    lines.add("    I like cheese");
    lines.add("    |> [ 12, 13 ]");

    assertThat(issue.toDisplayString()).isEqualTo(String.join("\n", lines));
  }
}
