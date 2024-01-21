package loom.common.json;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import com.networknt.schema.*;
import com.networknt.schema.uri.URIFetcher;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import lombok.Data;

/**
 * Context manager for {@link JsonSchemaFactory}.
 *
 * <p>Supports manually binding schemas to URIs.
 */
@Data
public class JsonSchemaFactoryManager {

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
  public JsonSchema getSchema(@Nonnull URI uri) {
    return factory.getSchema(uri, config);
  }

  private JsonSchema getMetaSchemaInstance() {
    return getSchema(URI.create(metaSchema.getUri()));
  }

  public void assertValidSchema(@Nonnull String schema) {
    var errors = getMetaSchemaInstance().validate(JsonUtil.parseToJsonNodeTree(schema));
    if (!errors.isEmpty()) {
      throw new AssertionError("Schema is invalid: " + errors);
    }
  }
}
