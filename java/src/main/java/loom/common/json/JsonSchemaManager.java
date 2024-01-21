package loom.common.json;

import com.fasterxml.jackson.databind.util.LRUMap;
import com.fasterxml.jackson.databind.util.LookupCache;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import jakarta.json.stream.JsonParsingException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Singular;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;
import org.leadpony.justify.api.JsonSchema;
import org.leadpony.justify.api.JsonValidationService;
import org.leadpony.justify.api.Problem;
import org.leadpony.justify.api.ProblemHandler;

/**
 * Manages JSON schemas and provides validation services.
 *
 * <p>Wrapper around provider implementation.
 */
public class JsonSchemaManager {
  public static final String JSD_ERROR = "JSD_ERROR";
  private final JsonValidationService service;
  private final LookupCache<String, JsonSchema> schemaCache;

  public JsonSchemaManager() {
    this(null);
  }

  public JsonSchemaManager(JsonValidationService service) {
    this(service, 1000);
  }

  public JsonSchemaManager(JsonValidationService service, int maxCacheSize) {
    this.service = service != null ? service : JsonValidationService.newInstance();
    schemaCache = new LRUMap<>(10, maxCacheSize);
  }

  /**
   * Format a parse error.
   *
   * @param e The exception.
   * @param source The source string.
   * @return The formatted error.
   */
  @VisibleForTesting
  static String formatParseError(JsonParsingException e, String source) {
    StringBuilder sb = new StringBuilder();
    sb.append(e.getMessage()).append("\n");

    var location = e.getLocation();
    var k = location.getLineNumber() - 1;
    var lines = Splitter.on("\n").splitToList(source);
    for (int i = 0; i < lines.size(); i++) {
      if (i == k) {
        sb.append(">>> ");
      } else {
        sb.append("    ");
      }
      sb.append(lines.get(i)).append("\n");
    }

    return sb.toString();
  }

  /**
   * Get a cached schema from a JSON string.
   *
   * @param schemaJson The JSON string.
   * @return The schema.
   */
  public JsonSchema loadSchema(String schemaJson) {
    var schema = schemaCache.get(schemaJson);
    if (schema == null) {
      try {
        schema =
            service.readSchema(
                new ByteArrayInputStream(schemaJson.getBytes(StandardCharsets.UTF_8)));
      } catch (JsonParsingException e) {
        String sb = "Error parsing JSON schema:\n" + formatParseError(e, schemaJson);
        throw new IllegalArgumentException(sb, e);
      }
      schemaCache.put(schemaJson, schema);
    }
    return schema;
  }

  /**
   * Validate a JSON string against a schema.
   *
   * @param schema The schema.
   * @param json The JSON string.
   * @return A list of problems.
   */
  public List<Problem> validationProblems(JsonSchema schema, String json) {
    var problems = new ArrayList<Problem>();
    var reader =
        service.createReader(
            new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
            schema,
            ProblemHandler.collectingTo(problems));
    reader.readValue();
    return problems;
  }

  public IssueScan.IssueScanBuilder issueScan() {
    return IssueScan.builder().manager(this);
  }

  @Builder
  public static final class IssueScan {
    @Builder.Default private final String type = JSD_ERROR;

    @Builder.Default private final String summaryPrefix = null;

    @Singular private final Map<String, String> params;

    @Nonnull private final JsonSchemaManager manager;

    @Nonnull private final JsonSchema schema;

    @Nonnull private final String json;

    @Nullable private final String jsonPathPrefix;

    @Singular private final List<ValidationIssue.Context> contexts;

    @Nonnull private final ValidationIssueCollector issueCollector;

    @SuppressWarnings("ConstantConditions")
    public void scan() {
      for (var problem : manager.validationProblems(schema, json)) {
        var builder = ValidationIssue.builder();
        builder.type(type);
        params.forEach(builder::param);

        var keyword = problem.getKeyword();
        if (keyword != null) {
          builder.param("keyword", keyword);
        }

        String summary =
            "%s [%s] :: %s"
                .formatted(problem.getPointer(), problem.getKeyword(), problem.getMessage());
        if (summaryPrefix != null) {
          summary = summaryPrefix + summary;
        }
        builder.summary(summary);

        String message = "Error Parameters: %s".formatted(problem.parametersAsMap());
        builder.message(message);

        var pointer = problem.getPointer();
        if (pointer != null) {
          var path =
              JsonPathUtils.concatJsonPath(
                  jsonPathPrefix, JsonPathUtils.jsonPointerToJsonPath(problem.getPointer()));
          builder.context(
              ValidationIssue.Context.builder()
                  .name("Data")
                  .jsonpath(path)
                  .data(problem.parametersAsMap().get("actual"))
                  .build());
        }

        if (contexts != null) {
          contexts.forEach(builder::context);
        }

        issueCollector.addIssue(builder);
      }
    }
  }
}
