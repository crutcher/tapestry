package loom.graph.validation;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import loom.common.HasToJsonString;
import loom.common.json.JsonPathUtils;
import loom.common.serialization.JsonUtil;
import loom.common.text.IndentUtils;

/** A Description of a validation failure. */
@Data
@Builder(toBuilder = true)
public final class ValidationIssue {

  /**
   * Format a list of issues as a string.
   *
   * @param issues the issues.
   * @return the formatted string.
   */
  public static String issuesToDisplayString(@Nullable List<ValidationIssue> issues) {
    if (issues == null || issues.isEmpty()) {
      return "No Validation Issues";
    }

    return "Validation failed with "
        + issues.size()
        + " issues:\n\n"
        + issues.stream().map(ValidationIssue::toDisplayString).collect(Collectors.joining("\n\n"))
        + "\n";
  }

  /** A named Context for a ValidationIssue. */
  @Data
  @Builder
  public static final class Context implements HasToJsonString {
    /** Extensions to the ContextBuilder. */
    public static class ContextBuilder {
      /**
       * Set the jsonpath for the context.
       *
       * <p>Concatenates the parts of the jsonpath.
       *
       * @param parts the parts of the jsonpath.
       * @return the builder.
       */
      public ContextBuilder jsonpath(String... parts) {
        this.jsonpath = JsonPathUtils.concatJsonPath(parts);
        return this;
      }

      /**
       * Set the jsonData for the context.
       *
       * <p>Converts the data to pretty json.
       *
       * @param data the data to set.
       * @return the builder.
       */
      public ContextBuilder dataFromTree(Object data) {
        this.jsonData = JsonUtil.toPrettyJson(data);
        return this;
      }
    }

    /**
     * Create a ContextBuilder.
     *
     * @return the builder.
     */
    public static Context.ContextBuilder builder() {
      return new ContextBuilder();
    }

    /**
     * Create a ContextBuilder with a name.
     *
     * @param name the name of the context.
     * @return the builder.
     */
    public static Context.ContextBuilder builder(String name) {
      return Context.builder().name(name);
    }

    @Nonnull private final String name;

    @Nullable private final String message;

    @Nullable private final String jsonpath;

    @Nullable private final String jsonData;

    /**
     * Format the context as a string.
     *
     * @return the formatted string.
     */
    public String toDisplayString() {
      var sb = new StringBuilder();

      sb.append("- %s::".formatted(name));
      if (jsonpath != null) {
        sb.append(" ").append(jsonpath);
      }

      if (message != null) {
        sb.append("\n")
            .append(IndentUtils.indent(2, IndentUtils.splitAndRemoveCommonIndent(message)));
      }

      if (jsonData != null) {
        sb.append("\n")
            .append(IndentUtils.indent("  |> ", JsonUtil.reformatToPrettyJson(jsonData)));
      }

      return sb.toString();
    }
  }

  /**
   * Create a ValidationIssueBuilder.
   *
   * @return the builder.
   */
  public static ValidationIssueBuilder builder() {
    return new ValidationIssueBuilder();
  }

  /**
   * Create a ValidationIssueBuilder with a type.
   *
   * @param type the type of the issue.
   * @return the builder.
   */
  public static ValidationIssueBuilder builder(String type) {
    return ValidationIssue.builder().type(type);
  }

  @Nonnull private final String type;

  @Nullable @Singular private final Map<String, String> params;

  @Nonnull private final String summary;

  @Nullable private final String message;

  @Singular private final List<Context> contexts;

  /**
   * Format the type and params as a string.
   *
   * @return the formatted string.
   */
  public String typeDescription() {
    var sb = new StringBuilder();
    sb.append(type);
    if (params != null && !params.isEmpty()) {
      sb.append(new TreeMap<>(params));
    }
    return sb.toString();
  }

  /**
   * Format the issue as a string.
   *
   * @return the formatted string.
   */
  public String toDisplayString() {
    var sb = new StringBuilder();
    sb.append("* Error [").append(typeDescription()).append("]: ").append(summary);

    if (message != null) {
      sb.append("\n").append(IndentUtils.reindent(2, message));
    }

    if (contexts != null)
      for (var context : contexts) {
        sb.append("\n\n").append(IndentUtils.reindent(2, context.toDisplayString()));
      }

    return sb.toString();
  }
}
