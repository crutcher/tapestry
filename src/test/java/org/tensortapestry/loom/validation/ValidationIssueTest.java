package org.tensortapestry.loom.validation;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.Test;
import org.tensortapestry.loom.testing.BaseTestClass;

public class ValidationIssueTest extends BaseTestClass {

  @Test
  public void test_builder_params() {
    var issue = ValidationIssue
      .builder()
      .type("foo")
      .param("foo", "qux")
      .param("bar", 2)
      .params(Map.of("foo", "xyz", "jkl", "mmm"))
      .summary("Foo bar")
      .build();

    assertThat(issue.getParams())
      .containsEntry("foo", "xyz")
      .containsEntry("bar", "2")
      .containsEntry("jkl", "mmm");
  }

  @Test
  public void test_builder_formatting() {
    var issue = ValidationIssue
      .builder()
      .type("foo")
      .summary("Foo %s Bar", 12)
      .message("Qux %s Baz", 13)
      .build();

    assertThat(issue.getSummary()).isEqualTo("Foo 12 Bar");
    assertThat(issue.getMessage()).isEqualTo("Qux 13 Baz");
  }

  @Test
  public void test_issuesToDisplayString() {
    assertThat(new ValidationIssueTextFormatter().formatIssueList(List.of()))
      .isEqualTo("No Validation Issues");
  }

  @Test
  public void test_builder_context_variants() {
    var issue = ValidationIssue
      .builder()
      .type("foo")
      .summary("Foo bar")
      .context((ValidationIssue.Context) null)
      .context(ValidationIssue.Context.builder("From Context").build())
      .context((ValidationIssue.Context.ContextBuilder) null)
      .context(ValidationIssue.Context.builder("From Builder"))
      .context((Supplier<ValidationIssue.Context>) null)
      .context(() -> ValidationIssue.Context.builder("From Supplier").build())
      .context(context -> context.name("From Consumer"))
      .withContexts((List<ValidationIssue.Context>) null)
      .withContexts(List.of(ValidationIssue.Context.builder("From List").build()))
      .withContexts((Supplier<List<ValidationIssue.Context>>) null)
      .withContexts(() -> List.of(ValidationIssue.Context.builder("From List Supplier").build()))
      .build();

    assertThat(issue.getContexts())
      .containsExactly(
        ValidationIssue.Context.builder("From Context").build(),
        ValidationIssue.Context.builder("From Builder").build(),
        ValidationIssue.Context.builder("From Supplier").build(),
        ValidationIssue.Context.builder("From Consumer").build(),
        ValidationIssue.Context.builder("From List").build(),
        ValidationIssue.Context.builder("From List Supplier").build()
      );
  }

  @Test
  public void testContextJson() {
    var context = ValidationIssue.Context
      .builder("Foo")
      .message("I like cheese\nand crackers")
      .jsonpath("$.foo", ".bar")
      .data(Map.of("foo", 2, "bar", 3))
      .build();

    var expectedJson = context.toJsonString();

    assertJsonEquals(context, expectedJson);
  }

  @Test
  public void testContextFormat() {
    var context = ValidationIssue.Context
      .builder("Foo")
      .message("I like cheese\nand crackers")
      .jsonpath("$.foo", ".bar")
      .data(Map.of("foo", 2, "bar", 3))
      .build();

    var lines = List.of(
      "- Foo:: $.foo.bar",
      "",
      "  I like cheese",
      "  and crackers",
      "",
      "  |> {",
      "  |>   \"bar\" : 3,",
      "  |>   \"foo\" : 2",
      "  |> }"
    );

    assertThat(new ValidationIssueTextFormatter().formatContext(context))
      .isEqualTo(String.join("\n", lines));
  }

  @Test
  public void test_toDisplayString() {
    var issue = ValidationIssue
      .builder()
      .type("foo")
      .param("type", "qux")
      .param("foo", "bar")
      .summary("Foo bar")
      .message("    I like the night life\n    I like to boogie")
      .context(
        ValidationIssue.Context
          .builder("Foo")
          .message("I like cheese\nand crackers")
          .jsonpath("$.foo", ".bar")
          .data(Map.of("foo", 2, "bar", 3))
      )
      .context(
        ValidationIssue.Context.builder("Bar").message("I like cheese").data(List.of(12, 13))
      )
      .build();

    var lines = List.of(
      "* Error [foo]: Foo bar",
      "   └> foo: bar",
      "   └> type: qux",
      "",
      "  I like the night life",
      "  I like to boogie",
      "",
      "  - Foo:: $.foo.bar",
      "",
      "    I like cheese",
      "    and crackers",
      "",
      "    |> {",
      "    |>   \"bar\" : 3,",
      "    |>   \"foo\" : 2",
      "    |> }",
      "",
      "  - Bar::",
      "",
      "    I like cheese",
      "",
      "    |> [ 12, 13 ]"
    );

    assertThat(new ValidationIssueTextFormatter().formatIssue(issue))
      .isEqualTo(String.join("\n", lines));
  }

  @Test
  public void test_empty_params_toDisplayString() {
    var issue = ValidationIssue.builder().type("foo").summary("Foo bar").build();
    assertThat(new ValidationIssueTextFormatter().formatIssue(issue))
      .isEqualTo("* Error [foo]: Foo bar");
  }
}
