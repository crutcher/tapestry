package loom.graph;

import com.jayway.jsonpath.EvaluationListener;
import com.jayway.jsonpath.JsonPath;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import loom.common.json.JsonUtil;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** A schema for validating the graph structure of a type in a LoomGraph. */
@Value
@Jacksonized
@Builder
public class LoomTypeSchema {

  /** A schema for validating a link in a LoomGraph. */
  @Value
  @Jacksonized
  @Builder
  public static class ReferenceSchema {
    /** A list of jsonpaths to node references described by the schema. */
    @Singular List<String> paths;

    /** A list of legal types that the referenced nodes can have. */
    @Singular List<String> types;
  }

  @Singular List<ReferenceSchema> referenceSchemas;

  public void validateValue(
      LoomGraph graph, String prefix, Object data, ValidationIssueCollector collector) {
    validateJson(graph, prefix, JsonUtil.toJson(data), collector);
  }

  public void validateJson(
      LoomGraph graph, String prefix, String json, ValidationIssueCollector collector) {
    var documentContext = JsonPath.parse(json);

    Supplier<List<ValidationIssue.Context>> dataContexts =
        () ->
            List.of(
                ValidationIssue.Context.builder()
                    .name("Data")
                    .jsonpath(prefix)
                    .dataFromJson(json)
                    .build());

    for (var link : referenceSchemas) {
      for (var path : link.paths) {
        Supplier<List<ValidationIssue.Context>> referenceContexts =
            () -> {
              var contexts = new ArrayList<ValidationIssue.Context>();
              contexts.add(
                  ValidationIssue.Context.builder().name("ReferenceSchema").data(link).build());
              contexts.addAll(dataContexts.get());
              return contexts;
            };

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
                            .param("nodeType", link.types)
                            .summary("Referenced node does not exist")
                            .context(
                                ValidationIssue.Context.builder()
                                    .name("Reference")
                                    .jsonpath(prefix, found.path())
                                    .data(id))
                            .withContexts(referenceContexts)
                            .build());

                  } else if (!link.types.isEmpty() && !link.types.contains(node.getType())) {
                    collector.addIssue(
                        ValidationIssue.builder()
                            .type(LoomConstants.NODE_REFERENCE_ERROR)
                            .param("nodeId", id)
                            .param("expectedType", link.types)
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
      }
    }
  }
}
