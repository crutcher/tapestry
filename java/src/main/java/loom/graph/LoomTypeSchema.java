package loom.graph;

import com.jayway.jsonpath.EvaluationListener;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import loom.common.json.JsonPathUtils;
import loom.common.json.JsonUtil;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;
import org.apache.commons.lang3.tuple.Pair;

/** A schema for validating the graph structure of a type in a LoomGraph. */
@Value
@ThreadSafe
@Jacksonized
@Builder
public class LoomTypeSchema {

  /** A schema for validating a link in a LoomGraph. */
  @Value
  @ThreadSafe
  @Jacksonized
  @Builder
  public static class ReferenceSchema {
    /** A list of json paths to node references described by the schema. */
    @Singular List<String> paths;

    /** A list of legal types that the referenced nodes can have. */
    @Singular List<String> types;

    @Nonnull
    public List<Pair<String, UUID>> collectFromValue(@Nonnull Object data) {
      return collectFromJson(JsonUtil.toJson(data));
    }

    @Nonnull
    public List<Pair<String, UUID>> collectFromJson(@Nonnull String json) {
      List<Pair<String, UUID>> results = new ArrayList<>();
      var documentContext = JsonPath.parse(json);
      for (var path : paths) {
        try {
          documentContext
              .withListeners(
                  found -> {
                    results.add(
                        Pair.of(
                            JsonPathUtils.normalizePath(found.path()),
                            UUID.fromString((String) found.result())));
                    return EvaluationListener.EvaluationContinuation.CONTINUE;
                  })
              .read(path);
        } catch (PathNotFoundException e) {
          // Ignore
        }
      }
      return results;
    }

    @Nonnull
    public List<LoomNode<?, ?>> collectFromGraph(@Nonnull LoomGraph graph, @Nonnull Object data) {
      String source;
      if (data instanceof String json) {
        source = json;
      } else {
        source = JsonUtil.toJson(data);
      }

      List<LoomNode<?, ?>> nodes = new ArrayList<>();
      for (var pair : collectFromJson(source)) {
        nodes.add(graph.getNode(pair.getRight()));
      }
      return nodes;
    }
  }

  @Singular Map<String, ReferenceSchema> referenceSchemas;

  @Nullable String jsonSchema;

  @Nonnull
  public List<LoomNode<?, ?>> collectFromGraph(
      @Nonnull LoomGraph graph, @Nonnull String reference, @Nonnull Object data) {
    return referenceSchemas.get(reference).collectFromGraph(graph, data);
  }

  public void validateValue(
      @Nonnull LoomEnvironment env,
      @Nonnull LoomGraph graph,
      @Nullable String prefix,
      @Nonnull Object data,
      @Nonnull ValidationIssueCollector collector) {
    validateJson(env, graph, prefix, JsonUtil.toJson(data), collector);
  }

  public void validateJson(
      @Nonnull LoomEnvironment env,
      @Nonnull LoomGraph graph,
      @Nullable String prefix,
      @Nonnull String json,
      @Nonnull ValidationIssueCollector collector) {
    var documentContext = JsonPath.parse(json);

    Supplier<List<ValidationIssue.Context>> dataContexts =
        () ->
            List.of(
                ValidationIssue.Context.builder()
                    .name("Body")
                    .jsonpath(prefix)
                    .dataFromJson(json)
                    .build());

    if (jsonSchema != null) {
      var schema = env.getJsonSchemaManager().loadSchema(jsonSchema);

      env.getJsonSchemaManager()
          .issueScan()
          .issueCollector(collector)
          .type(LoomConstants.Errors.NODE_SCHEMA_ERROR)
          .summaryPrefix("Body ")
          .jsonPathPrefix(prefix)
          .schema(schema)
          .json(json)
          .context(
              ValidationIssue.Context.builder()
                  .name("Body")
                  .jsonpath(prefix)
                  .dataFromJson(json)
                  .build())
          .build()
          .scan();
    }

    for (var entry : referenceSchemas.entrySet()) {
      var name = entry.getKey();
      var rschema = entry.getValue();

      for (var path : rschema.paths) {
        Supplier<List<ValidationIssue.Context>> referenceContexts =
            () -> {
              var contexts = new ArrayList<ValidationIssue.Context>();
              contexts.add(
                  ValidationIssue.Context.builder()
                      .name("ReferenceSchema")
                      .message(name)
                      .data(rschema)
                      .build());
              contexts.addAll(dataContexts.get());
              return contexts;
            };

        try {
          documentContext
              .withListeners(
                  found -> {
                    var result = found.result();
                    UUID id;
                    try {
                      id = UUID.fromString((String) result);
                    } catch (Exception e) {
                      collector.addIssue(
                          ValidationIssue.builder()
                              .type(LoomConstants.NODE_REFERENCE_ERROR)
                              .summary("Malformed node reference is not an ID")
                              .context(
                                  ValidationIssue.Context.builder()
                                      .name("Reference")
                                      .jsonpath(prefix, found.path())
                                      .data(result))
                              .withContexts(referenceContexts)
                              .build());
                      return EvaluationListener.EvaluationContinuation.CONTINUE;
                    }

                    var node = graph.getNode(id);
                    if (node == null) {
                      collector.addIssue(
                          ValidationIssue.builder()
                              .type(LoomConstants.NODE_REFERENCE_ERROR)
                              .param("nodeId", id)
                              .param("nodeType", rschema.types)
                              .summary("Referenced node does not exist")
                              .context(
                                  ValidationIssue.Context.builder()
                                      .name("Reference")
                                      .jsonpath(prefix, found.path())
                                      .data(id))
                              .withContexts(referenceContexts)
                              .build());

                    } else if (!rschema.types.isEmpty()
                        && !rschema.types.contains(node.getType())) {
                      collector.addIssue(
                          ValidationIssue.builder()
                              .type(LoomConstants.NODE_REFERENCE_ERROR)
                              .param("nodeId", id)
                              .param("expectedType", rschema.types)
                              .param("actualType", node.getType())
                              .summary("Referenced node has the wrong type")
                              .context(
                                  ValidationIssue.Context.builder()
                                      .name("Reference")
                                      .jsonpath(prefix, found.path())
                                      .data(id))
                              .context(node.asValidationContext("Target"))
                              .withContexts(referenceContexts)
                              .build());
                    }

                    return EvaluationListener.EvaluationContinuation.CONTINUE;
                  })
              .read(path);
        } catch (PathNotFoundException e) {
          // Ignore
        }
      }
    }
  }
}
