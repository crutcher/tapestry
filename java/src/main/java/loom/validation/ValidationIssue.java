package loom.validation;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.extern.jackson.Jacksonized;
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
  @Jacksonized
  @Builder
  public static final class Context implements HasToJsonString {

    /** Extensions to the ContextBuilder. */
    public static class ContextBuilder {
      /**
       * Set the jsonpath for the context.
       *
       * @param jsonpath the jsonpath.
       * @return the builder.
       */
      public ContextBuilder jsonpath(String jsonpath) {
        this.jsonpath = jsonpath;
        return this;
      }

      /**
       * Set the jsonpath for the context.
       *
       * <p>Concatenates the parts of the jsonpath.
       *
       * @param parts the parts of the jsonpath.
       * @return the builder.
       */
      public ContextBuilder jsonpath(String... parts) {
        return jsonpath(JsonPathUtils.concatJsonPath(parts));
      }

      /**
       * Set the data for the context by converting an object to JsonNode.
       *
       * <p>Deep-copies the value.
       *
       * @param value the object to convert.
       * @return the builder.
       */
      public ContextBuilder data(Object value) {
        this.data = JsonUtil.treeToSimpleJson(JsonUtil.valueToJsonNodeTree(value));
        return this;
      }

      /**
       * Set the data for the context by parsing a JSON string.
       *
       * @param json the string to parse.
       * @return the builder.
       */
      public ContextBuilder dataFromJson(String json) {
        return data(JsonUtil.fromJson(json, Object.class));
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

    /** This should always be a simple JSON Java value. */
    @Nullable private final Object data;

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
        var m = message.trim();
        if (!m.isEmpty())
          sb.append("\n\n")
              .append(IndentUtils.indent(2, IndentUtils.splitAndRemoveCommonIndent(m)));
      }

      if (data != null) {
        sb.append("\n\n").append(IndentUtils.indent("  |> ", JsonUtil.toPrettyJson(data)));
      }

      return sb.toString();
    }
  }

  /** Extensions to the ValidationIssueBuilder. */
  public static final class ValidationIssueBuilder {

    /**
     * Add a context to the issue.
     *
     * @param context the context to add.
     * @return this builder, for chaining.
     */
    public ValidationIssueBuilder context(Context context) {
      if (this.contexts == null) {
        this.contexts = new ArrayList<>();
      }

      this.contexts.add(context);
      return this;
    }

    /**
     * Add a context to the issue.
     *
     * @param builder the builder to build a Context from..
     * @return this builder, for chaining.
     */
    public ValidationIssueBuilder context(Context.ContextBuilder builder) {
      return context(builder.build());
    }

    /**
     * Add each context to the issue.
     *
     * @param contexts the contexts to add.
     * @return this builder, for chaining.
     */
    public ValidationIssueBuilder withContexts(Iterable<Context> contexts) {
      contexts.forEach(this::context);
      return this;
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

  @Nullable private final List<Context> contexts;

  @VisibleForTesting
  String paramsToString() {
    var parts = new ArrayList<String>();
    new TreeMap<>(params).forEach((k, v) -> parts.add("   └> %s: %s".formatted(k, v)));
    return String.join("\n", parts);
  }

  /**
   * Format the issue as a string.
   *
   * @return the formatted string.
   */
  public String toDisplayString() {
    var sb = new StringBuilder();
    sb.append("* Error [").append(type).append("]: ").append(summary);
    if (params != null && !params.isEmpty()) {
      sb.append("\n").append(paramsToString());
    }

    if (message != null) {
      sb.append("\n\n").append(IndentUtils.reindent(2, message));
    }

    if (contexts != null) {
      for (var context : contexts) {
        sb.append("\n\n").append(IndentUtils.reindent(2, context.toDisplayString()));
      }
    }

    return sb.toString();
  }
}
