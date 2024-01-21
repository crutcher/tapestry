package loom.validation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.errorprone.annotations.FormatMethod;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import loom.common.json.HasToJsonString;
import loom.common.json.JsonPathUtils;
import loom.common.json.JsonUtil;

/** A Description of a validation failure. */
@Data
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public final class ValidationIssue {

  /** A named Context for a ValidationIssue. */
  @Data
  @Jacksonized
  @Builder
  @JsonInclude(JsonInclude.Include.NON_NULL)
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
      public ContextBuilder jsonpath(Object... parts) {
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

    @Nonnull
    private final String name;

    @Nullable
    private final String message;

    @Nullable
    private final String jsonpath;

    /** This should always be a simple JSON Java value. */
    @Nullable
    private final Object data;
  }

  /** Extensions to the ValidationIssueBuilder. */
  public static final class ValidationIssueBuilder {

    /**
     * Add each param to the issue.
     *
     * @param params the params to add.
     * @return this builder, for chaining.
     */
    public ValidationIssueBuilder params(@Nullable Map<String, String> params) {
      if (params != null) {
        params.forEach(this::param);
      }
      return this;
    }

    /**
     * Add a param to the issue.
     *
     * @param key the key of the param.
     * @param value the value of the param.
     * @return this builder, for chaining.
     */
    public ValidationIssueBuilder param(String key, Object value) {
      if (this.params == null) {
        // TODO: it would be nice if param insertion order was preserved.
        // this is currently getting lost in the build() step.
        this.params = new LinkedHashMap<>();
      }
      this.params.put(key, value.toString());
      return this;
    }

    /**
     * Set the summary for the issue.
     *
     * @param summary the summary.
     * @return this builder, for chaining.
     */
    public ValidationIssueBuilder summary(String summary) {
      this.summary = summary;
      return this;
    }

    /**
     * Set the summary for the issue.
     *
     * @param format the format string.
     * @param args the arguments.
     * @return this builder, for chaining.
     */
    @FormatMethod
    public ValidationIssueBuilder summary(String format, Object... args) {
      this.summary = String.format(format, args);
      return this;
    }

    /**
     * Set the message for the issue.
     *
     * @param message the message.
     * @return this builder, for chaining.
     */
    public ValidationIssueBuilder message(String message) {
      this.message = message;
      return this;
    }

    /**
     * Set the message for the issue.
     *
     * @param format the format string.
     * @param args the arguments.
     * @return this builder, for chaining.
     */
    @FormatMethod
    public ValidationIssueBuilder message(String format, Object... args) {
      this.message = String.format(format, args);
      return this;
    }

    /**
     * Add a context to the issue.
     *
     * @param context the context to add.
     * @return this builder, for chaining.
     */
    public ValidationIssueBuilder context(@Nullable Context context) {
      if (context == null) {
        return this;
      }
      if (this.contexts == null) {
        this.contexts = new ArrayList<>();
      }

      this.contexts.add(context);
      return this;
    }

    /**
     * Add a context to the issue.
     *
     * @param supplier the supplier to get the context from.
     * @return this builder, for chaining.
     */
    public ValidationIssueBuilder context(@Nullable Supplier<Context> supplier) {
      if (supplier == null) {
        return this;
      }
      return context(supplier.get());
    }

    /**
     * Add a context to the issue.
     *
     * @param builder the builder to build a Context from.
     * @return this builder, for chaining.
     */
    public ValidationIssueBuilder context(@Nullable Context.ContextBuilder builder) {
      if (builder == null) {
        return this;
      }
      return context(builder::build);
    }

    /**
     * Add a context to the issue.
     *
     * @param consumer the consumer to fill in the ContextBuilder.
     * @return this builder, for chaining.
     */
    public ValidationIssueBuilder context(@Nonnull Consumer<Context.ContextBuilder> consumer) {
      var builder = Context.builder();
      consumer.accept(builder);
      return context(builder);
    }

    /**
     * Add each context to the issue.
     *
     * @param contexts the contexts to add.
     * @return this builder, for chaining.
     */
    public ValidationIssueBuilder withContexts(@Nullable List<Context> contexts) {
      if (contexts == null) {
        return this;
      }
      contexts.forEach(this::context);
      return this;
    }

    /**
     * Add each context to the issue.
     *
     * @param supplier the supplier to get the contexts from.
     * @return this builder, for chaining.
     */
    public ValidationIssueBuilder withContexts(@Nullable Supplier<List<Context>> supplier) {
      if (supplier == null) {
        return this;
      }
      return withContexts(supplier.get());
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

  @Nonnull
  private final String type;

  @Nullable
  private final Map<String, String> params;

  @Nonnull
  private final String summary;

  @Nullable
  private final String message;

  @Nullable
  private final List<Context> contexts;
}
