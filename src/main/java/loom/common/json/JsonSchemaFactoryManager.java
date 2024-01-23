package loom.common.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import com.networknt.schema.*;
import com.networknt.schema.uri.URIFetcher;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Singular;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;

/**
 * Context manager for {@link JsonSchemaFactory}.
 *
 * <p>Supports manually binding schemas to URIs.
 */
public class JsonSchemaFactoryManager {

  public static final String JSD_ERROR = "JSD_ERROR";
  private final Map<URI, String> schemas = new HashMap<>();
  private final URIFetcher uriFetcher = uri -> {
    var schemaSource = schemas.get(uri.normalize());
    if (schemaSource != null) {
      return new ByteArrayInputStream(schemaSource.getBytes(StandardCharsets.UTF_8));
    }
    throw new RuntimeException("Unknown URI: " + uri);
  };

  private final JsonMetaSchema metaSchema = new Version202012().getInstance();

  private final JsonMetaSchema validationSchema = new JsonMetaSchema.Builder(
    metaSchema.getUri() + "/validation"
  )
    .idKeyword(metaSchema.getIdKeyword())
    .addFormats(JsonSchemaVersion.BUILTIN_FORMATS)
    .addKeywords(metaSchema.getKeywords().values())
    .addKeywords(
      Arrays.asList(
        new NonValidationKeyword("$vocabulary"),
        new NonValidationKeyword("$dynamicAnchor"),
        new NonValidationKeyword("$dynamicRef")
      )
    )
    .build();

  private final JsonSchemaFactory factory = JsonSchemaFactory
    .builder()
    .defaultMetaSchemaURI(metaSchema.getUri())
    .addMetaSchema(metaSchema)
    .addMetaSchema(validationSchema)
    .objectMapper(JsonUtil.getObjectMapper())
    .uriFetcher(uriFetcher, "http")
    .enableUriSchemaCache(true)
    .build();

  private final SchemaValidatorsConfig config = new SchemaValidatorsConfig();

  {
    config.setReadOnly(true);
    config.setCustomMessageSupported(true);
  }

  public boolean hasSchema(URI uri) {
    return schemas.containsKey(uri.normalize());
  }

  @CanIgnoreReturnValue
  public JsonSchemaFactoryManager addSchema(String schema) {
    var tree = JsonUtil.parseToJsonNodeTree(schema);
    var uri = tree.get(metaSchema.getIdKeyword()).asText();
    return addSchema(URI.create(uri), schema);
  }

  @CanIgnoreReturnValue
  public JsonSchemaFactoryManager addSchema(URI uri, String schema) {
    URI normUri = uri.normalize();
    if (hasSchema(normUri)) {
      throw new IllegalArgumentException("Schema already registered for URI: " + normUri);
    }
    assertValidSchema(schema);
    schemas.put(normUri, schema);
    return this;
  }

  @CanIgnoreReturnValue
  public JsonSchemaFactoryManager addSchema(@Nonnull String uri, @Nonnull String schema) {
    return addSchema(URI.create(uri), schema);
  }

  @Nonnull
  @CheckReturnValue
  public JsonSchema loadSchema(@Nonnull URI uri) {
    return factory.getSchema(uri, config);
  }

  @Nonnull
  @CheckReturnValue
  public JsonSchema loadSchemaFromSource(String source) {
    return factory.getSchema(source, config);
  }

  private JsonSchema getMetaSchemaInstance() {
    return loadSchema(URI.create(metaSchema.getUri()));
  }

  public void assertValidSchema(@Nonnull String schema) {
    var errors = getMetaSchemaInstance().validate(JsonUtil.parseToJsonNodeTree(schema));
    if (!errors.isEmpty()) {
      throw new AssertionError("Schema is invalid: " + errors);
    }
  }

  public IssueScan.IssueScanBuilder issueScan() {
    return IssueScan.builder();
  }

  @Builder
  public static final class IssueScan {

    @Nonnull
    private final JsonSchema schema;

    @Builder.Default
    private final String type = JSD_ERROR;

    @Builder.Default
    private final String summaryPrefix = null;

    @Singular
    private final Map<String, String> params;

    @Nonnull
    private final JsonNode data;

    @Nullable private final String jsonPathPrefix;

    private final Supplier<List<ValidationIssue.Context>> contexts;

    @Nonnull
    private final ValidationIssueCollector issueCollector;

    @SuppressWarnings("ConstantConditions")
    private ValidationIssue adaptValidationMessage(ValidationMessage error) {
      var builder = ValidationIssue.builder().type(type);

      String relPath = error.getInstanceLocation().toString();
      var actual = JsonUtil.jsonPathOnValue(data, relPath, JsonNode.class);

      var displayData = JsonUtil.toJson(actual);

      String absPath = JsonPathUtils.concatJsonPath(jsonPathPrefix, relPath);

      builder.param("path", absPath);
      builder.param("schemaPath", error.getSchemaLocation());
      builder.param("keyword", error.getType());
      builder.param("keywordArgs", JsonUtil.toSimpleJson(error.getArguments()));
      builder.params(params);

      String summary = "[%s] :: %s".formatted(error.getType(), relPath);
      if (displayData.length() < 50) {
        summary += ": " + displayData;
      }
      if (summaryPrefix != null) {
        summary = summaryPrefix + summary;
      }
      builder.summary(summary);
      builder.message(error.getMessage());

      builder.context(
        ValidationIssue.Context.builder().name("Data").jsonpath(absPath).data(actual)
      );

      builder.withContexts(contexts);

      return builder.build();
    }

    @SuppressWarnings("ConstantConditions")
    public void scan() {
      for (var error : schema.validate(data)) {
        issueCollector.addIssue(adaptValidationMessage(error));
      }
    }
  }
}
