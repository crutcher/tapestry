package loom.graph;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import loom.graph.nodes.NoteNode;
import loom.graph.nodes.TensorNode;
import loom.testing.BaseTestClass;
import loom.validation.ListValidationIssueCollector;
import loom.validation.ValidationIssue;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LoomTypeSchemaTest extends BaseTestClass {
  @Value
  @Jacksonized
  @Builder
  public static class Example {
    Map<String, List<String>> a;
  }

  @Test
  public void test_validate() {
    var schema =
        LoomTypeSchema.builder()
            .referenceSchema(
                LoomTypeSchema.ReferenceSchema.builder()
                    .path("$.a.*[*]")
                    .type(NoteNode.TYPE)
                    .build())
            .build();

    var env = CommonEnvironments.expressionEnvironment();
    var graph = env.newGraph();
    var note = NoteNode.withBody(b -> b.message("hello")).addTo(graph);
    var tensor = TensorNode.withBody(b -> b.dtype("int32").shape(2)).addTo(graph);

    UUID missingId = UUID.randomUUID();
    String garbage = "garbage";
    var data =
        Example.builder()
            .a(
                Map.of(
                    "b",
                    List.of(
                        note.getId().toString(),
                        tensor.getId().toString(),
                        missingId.toString(),
                        garbage)))
            .build();

    ListValidationIssueCollector collector = new ListValidationIssueCollector();
    String prefix = "$.foo";
    schema.validateValue(graph, prefix, data, collector);
    assertValidationIssues(
        collector,
        ValidationIssue.builder()
            .type(LoomConstants.NODE_REFERENCE_ERROR)
            .param("nodeId", tensor.getId())
            .param("expectedType", List.of(NoteNode.TYPE))
            .param("actualType", TensorNode.TYPE)
            .summary("Referenced node has the wrong type")
            .context(
                ValidationIssue.Context.builder()
                    .name("Reference")
                    .jsonpath("$.foo.a.b[1]")
                    .data(tensor.getId()))
            .context(tensor.asValidationContext("Target"))
            .context(
                ValidationIssue.Context.builder()
                    .name("ReferenceSchema")
                    .data(schema.getReferenceSchemas().getFirst()))
            .context(ValidationIssue.Context.builder().name("Data").jsonpath(prefix).data(data))
            .build(),
        ValidationIssue.builder()
            .type(LoomConstants.NODE_REFERENCE_ERROR)
            .param("nodeId", missingId)
            .param("nodeType", List.of(NoteNode.TYPE))
            .summary("Referenced node does not exist")
            .context(
                ValidationIssue.Context.builder()
                    .name("Reference")
                    .jsonpath("$.foo.a.b[2]")
                    .data(missingId))
            .context(
                ValidationIssue.Context.builder()
                    .name("ReferenceSchema")
                    .data(schema.getReferenceSchemas().getFirst()))
            .context(ValidationIssue.Context.builder().name("Data").jsonpath(prefix).data(data))
            .build(),
        ValidationIssue.builder()
            .type(LoomConstants.NODE_REFERENCE_ERROR)
            .summary("Malformed node reference is not an ID")
            .context(
                ValidationIssue.Context.builder()
                    .name("Reference")
                    .jsonpath("$.foo.a.b[3]")
                    .data(garbage))
            .context(
                ValidationIssue.Context.builder()
                    .name("ReferenceSchema")
                    .data(schema.getReferenceSchemas().getFirst()))
            .context(ValidationIssue.Context.builder().name("Data").jsonpath(prefix).data(data))
            .build());
  }
}
