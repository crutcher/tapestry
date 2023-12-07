package loom.common.json;

import lombok.Data;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;
import net.jimblackler.jsonschemafriend.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

@Data
public class JsdManager {
  public final String JSD_ERROR = "JSD_ERROR";
  private final SchemaStore schemaStore;

  public JsdManager(@Nullable SchemaStore schemaStore) {
    this.schemaStore = schemaStore != null ? schemaStore : new SchemaStore(true);
  }

  /**
   * Apply a JSON schema to a JSON document.
   *
   * <p>Collects issues in the given issue collector.
   *
   * @param issueCollector the issue collector.
   * @param schemaUri the schema URI.
   * @param schemaDoc the schema document.
   * @param jpathPrefix the JSON path prefix to apply to the context.
   * @param json the JSON document.
   * @param contexts additional Issue contexts.
   */
  public void applySchemaJson(
      @Nonnull ValidationIssueCollector issueCollector,
      URI schemaUri,
      Object schemaDoc,
      @Nullable String jpathPrefix,
      String json,
      @Nullable Collection<ValidationIssue.Context> contexts) {
    Objects.requireNonNull(issueCollector);

    var validator = new Validator();

    Schema schema;
    {
      try {
        schemaStore.store(schemaUri, schemaDoc);
      } catch (IllegalStateException e) {
        // Ignore; already loaded.
      }
      try {
        schema = schemaStore.loadSchema(schemaUri, validator);
      } catch (SchemaException e) {
        throw new RuntimeException(e);
      }
    }

    Collection<ValidationError> jsdErrors;
    try {
      validator.validateJson(schema, json);
      return;
    } catch (ValidationException e) {
      if (!(e instanceof ListValidationException)) {
        throw new RuntimeException(e);
      }

      jsdErrors = ((ListValidationException) e).getErrors();
    }

    for (var error : jsdErrors) {
      String summary =
          error.getSchema().getUri().getFragment()
              + ": ("
              + error.getObject()
              + ") "
              + error.getMessage();

      var contextList = new ArrayList<ValidationIssue.Context>();
      contextList.add(
          ValidationIssue.Context.builder()
              .name("Instance")
              .jsonpath(
                  JsonPathUtils.concatJsonPath(
                      jpathPrefix,
                      JsonPathUtils.jsonPointerToJsonPath(error.getUri().toString().substring(1))))
              .dataFromTree(error.getObject())
              .build());

      if (contexts != null) {
        contextList.addAll(contexts);
      }

      issueCollector.add(
          ValidationIssue.builder()
              .type(JSD_ERROR)
              .param("error", error.getClass().getSimpleName())
              .summary(summary)
              .contexts(contextList));
    }
  }
}
