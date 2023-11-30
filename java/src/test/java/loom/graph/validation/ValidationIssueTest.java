package loom.graph.validation;

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
            .dataFromTree(Map.of("foo", 2, "bar", 3))
            .build();

    var lines =
        List.of(
            "- Foo:: $.foo.bar",
            "  I like cheese",
            "  and crackers",
            "  |> {",
            "  |>   \"bar\" : 3,",
            "  |>   \"foo\" : 2",
            "  |> }");

    assertThat(context.toDisplayString()).isEqualTo(String.join("\n", lines));
  }

  @Test
  public void testIssueFormat() {
    var issue =
        ValidationIssue.builder()
            .type("foo")
            .param("type", "qux")
            .param("foo", "bar")
            .summary("Foo bar")
            .message("    I like the night life\n    I like to boogie")
            .context(
                ValidationIssue.Context.builder("Foo")
                    .message("I like cheese\nand crackers")
                    .jsonpath("$.foo", ".bar")
                    .dataFromTree(Map.of("foo", 2, "bar", 3))
                    .build())
            .context(
                ValidationIssue.Context.builder("Bar")
                    .message("I like cheese")
                    .dataFromTree(List.of(12, 13))
                    .build())
            .build();

    var lines =
        List.of(
            "* Error [foo{foo=bar, type=qux}]: Foo bar",
            "  I like the night life",
            "  I like to boogie",
            "",
            "  - Foo:: $.foo.bar",
            "    I like cheese",
            "    and crackers",
            "    |> {",
            "    |>   \"bar\" : 3,",
            "    |>   \"foo\" : 2",
            "    |> }",
            "",
            "  - Bar::",
            "    I like cheese",
            "    |> [ 12, 13 ]");

    assertThat(issue.toDisplayString()).isEqualTo(String.join("\n", lines));
  }
}
