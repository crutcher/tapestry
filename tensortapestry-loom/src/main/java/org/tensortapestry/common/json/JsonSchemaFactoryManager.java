package org.tensortapestry.common.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import com.networknt.schema.*;
import com.networknt.schema.uri.URIFetcher;
import com.networknt.schema.uri.URITranslator;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.tensortapestry.common.validation.ValidationIssue;
import org.tensortapestry.common.validation.ValidationIssueCollector;
import org.tensortapestry.loom.graph.LoomConstants;

/**
 * Context manager for {@link JsonSchemaFactory}.
 *
 * <p>Supports manually binding schemas to URIs.
 */
public class JsonSchemaFactoryManager {

  private final Map<URI, String> schemas = new HashMap<>();

  private static URI baseUrl(URI uri) {
    uri = uri.normalize();
    try {
      return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), uri.getQuery(), null);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private final Map<String, String> resourceDirMap = new HashMap<>();

  private final URIFetcher uriFetcher = uri -> {
    URI burl = baseUrl(uri);

    var schemaSource = schemas.get(burl);
    if (schemaSource != null) {
      return new ByteArrayInputStream(schemaSource.getBytes(StandardCharsets.UTF_8));
    }

    // TODO: Fix this hack.
    // There is a bug in JsonSchemaFactory which breaks the lookup of fragment references
    // in some cases. The *correct* approach to this is to use a URITranslator to map
    // the URI to a local file path, but the resulting mappings have broken fragment
    // mapping semantics.
    //
    // I fixed at least part of this upstream, waiting for a release:
    // See: https://github.com/networknt/json-schema-validator/pull/930

    var urlStr = burl.toString();
    for (var entry : resourceDirMap.entrySet()) {
      var prefix = entry.getKey();
      var dir = entry.getValue();

      if (urlStr.startsWith(prefix)) {
        var path = urlStr.substring(prefix.length());
        var resource = dir + path;
        var stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        if (stream != null) {
          return stream;
        }
      }
    }

    throw new FileNotFoundException("Unknown URI: " + burl);
  };

  @Getter
  private final URITranslator.CompositeURITranslator uriTranslator =
    new URITranslator.CompositeURITranslator();

  @Getter
  private final JsonMetaSchema metaSchema = new Version202012().getInstance();

  @Getter
  private final JsonSchemaFactory factory = JsonSchemaFactory
    .builder()
    .defaultMetaSchemaURI(metaSchema.getUri())
    .addMetaSchema(metaSchema)
    .objectMapper(JsonUtil.getObjectMapper())
    .addUriTranslator(uriTranslator)
    .uriFetcher(uriFetcher, "http")
    .enableUriSchemaCache(true)
    .build();

  @Getter
  private final SchemaValidatorsConfig config = new SchemaValidatorsConfig();

  {
    // See: https://github.com/networknt/json-schema-validator/blob/master/doc/ecma-262.md
    // config.setEcma262Validator(true);
    config.setReadOnly(true);
    config.setCustomMessageSupported(true);
  }

  public boolean hasSchema(URI uri) {
    return schemas.containsKey(uri.normalize());
  }

  @CanIgnoreReturnValue
  public JsonSchemaFactoryManager bindResourcePath(String urlPrefix, String resourceDir) {
    resourceDirMap.put(urlPrefix, resourceDir);
    return this;
  }

  @CanIgnoreReturnValue
  public JsonSchemaFactoryManager addSchema(String schema) {
    var tree = JsonUtil.parseToJsonNodeTree(schema);
    var uri = tree.get(metaSchema.getIdKeyword()).asText();
    return addSchema(URI.create(uri), schema);
  }

  @CanIgnoreReturnValue
  @SuppressWarnings("InconsistentOverloads")
  public JsonSchemaFactoryManager addSchema(URI uri, String schema) {
    if (uri.getFragment() != null) {
      throw new IllegalArgumentException("URI must not have a fragment: " + uri);
    }
    URI normUri = baseUrl(uri);
    if (hasSchema(normUri)) {
      throw new IllegalArgumentException("Schema already registered for URI: " + normUri);
    }
    assertValidSchema(schema);
    schemas.put(normUri, schema);
    return this;
  }

  @CanIgnoreReturnValue
  @SuppressWarnings("InconsistentOverloads")
  public JsonSchemaFactoryManager addSchema(@Nonnull String uri, @Nonnull String schema) {
    return addSchema(URI.create(uri), schema);
  }

  @CheckReturnValue
  public JsonSchema loadSchema(@Nonnull URI uri) {
    try {
      return factory.getSchema(uri, config);
    } catch (JsonSchemaException e) {
      throw new RuntimeException("Error loading schema: " + uri, e);
    }
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
    private final String type = LoomConstants.Errors.JSD_ERROR;

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

    public void scan() {
      for (var error : schema.validate(data)) {
        issueCollector.addIssue(adaptValidationMessage(error));
      }
    }
  }
}
