package loom.graph;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import loom.common.json.JsonUtil;
import loom.graph.nodes.NoteNode;
import loom.graph.nodes.TensorNode;
import loom.testing.BaseTestClass;
import loom.validation.ListValidationIssueCollector;
import loom.validation.ValidationIssue;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LoomTypeSchemaTest extends BaseTestClass {
  @Value
  @Jacksonized
  @Builder
  public static class Example {
    Map<String, List<String>> inputs;
  }

  @Test
  public void test_collect() {
    var schema =
        LoomTypeSchema.builder()
            .referenceSchema(
                "inputs",
                LoomTypeSchema.ReferenceSchema.builder()
                    .path("$.inputs.*[*]")
                    .type(NoteNode.TYPE)
                    .build())
            .build();

    var env = CommonEnvironments.expressionEnvironment();
    var graph = env.newGraph();
    var note = NoteNode.withBody(b -> b.message("hello")).addTo(graph);

    var data = Example.builder().inputs(Map.of("b", List.of(note.getId().toString()))).build();

    assertThat(schema.collectFromGraph(graph, "inputs", JsonUtil.toJson(data))).containsOnly(note);
    assertThat(schema.collectFromGraph(graph, "inputs", data)).containsOnly(note);

    assertThat(schema.getReferenceSchemas().get("inputs").collectFromValue(data))
        .containsOnly(Pair.of("$.inputs.b[0]", note.getId()));
  }

  @Test
  public void test_validate() {
    var schema =
        LoomTypeSchema.builder()
            .referenceSchema(
                "inputs",
                LoomTypeSchema.ReferenceSchema.builder()
                    .path("$.inputs.*[*]")
                    .type(NoteNode.TYPE)
                    .build())
            .jsonSchema(
                """
                    {
                      "type": "object",
                      "properties": {
                        "inputs": {
                          "type": "object",
                          "patternProperties": {
                              "^[a-zA-Z_][a-zA-Z0-9_]*$": {
                                  "type": "array",
                                  "items": {
                                    "type": "string",
                                    "format": "uuid"
                                  }
                              }
                          },
                          "additionalProperties": false
                        }
                      },
                      "required": ["inputs"],
                      "additionalProperties": false
                    }
                    """)
            .build();

    var env = CommonEnvironments.expressionEnvironment();
    var graph = env.newGraph();
    var note = NoteNode.withBody(b -> b.message("hello")).addTo(graph);
    var tensor = TensorNode.withBody(b -> b.dtype("int32").shape(2)).addTo(graph);

    UUID missingId = UUID.randomUUID();
    String garbage = "garbage";
    var data =
        Example.builder()
            .inputs(
                Map.of(
                    "9",
                    List.of(),
                    "b",
                    List.of(
                        note.getId().toString(),
                        tensor.getId().toString(),
                        missingId.toString(),
                        garbage)))
            .build();

    ListValidationIssueCollector collector = new ListValidationIssueCollector();
    String prefix = "$.foo";
    schema.validateValue(env, graph, prefix, data, collector);
    assertValidationIssues(
        collector,
        ValidationIssue.builder()
            .type(LoomConstants.Errors.NODE_SCHEMA_ERROR)
            .summary(
                "Body /inputs/9 [null] :: The object must not have a property whose name is \"9\".")
            .message("Error Parameters: {name=9}")
            .context(
                ValidationIssue.Context.builder()
                    .name("Data")
                    .jsonpath(prefix, "inputs[9]")
                    .build())
            .context(ValidationIssue.Context.builder().name("Body").jsonpath(prefix).data(data))
            .build(),
        ValidationIssue.builder()
            .type(LoomConstants.NODE_REFERENCE_ERROR)
            .param("nodeId", tensor.getId())
            .param("expectedType", List.of(NoteNode.TYPE))
            .param("actualType", TensorNode.TYPE)
            .summary("Referenced node has the wrong type")
            .context(
                ValidationIssue.Context.builder()
                    .name("Reference")
                    .jsonpath("$.foo.inputs.b[1]")
                    .data(tensor.getId()))
            .context(tensor.asValidationContext("Target"))
            .context(
                ValidationIssue.Context.builder()
                    .name("ReferenceSchema")
                    .message("inputs")
                    .data(schema.getReferenceSchemas().get("inputs")))
            .context(ValidationIssue.Context.builder().name("Body").jsonpath(prefix).data(data))
            .build(),
        ValidationIssue.builder()
            .type(LoomConstants.NODE_REFERENCE_ERROR)
            .param("nodeId", missingId)
            .param("nodeType", List.of(NoteNode.TYPE))
            .summary("Referenced node does not exist")
            .context(
                ValidationIssue.Context.builder()
                    .name("Reference")
                    .jsonpath("$.foo.inputs.b[2]")
                    .data(missingId))
            .context(
                ValidationIssue.Context.builder()
                    .name("ReferenceSchema")
                    .message("inputs")
                    .data(schema.getReferenceSchemas().get("inputs")))
            .context(ValidationIssue.Context.builder().name("Body").jsonpath(prefix).data(data))
            .build(),
        ValidationIssue.builder()
            .type(LoomConstants.NODE_REFERENCE_ERROR)
            .summary("Malformed node reference is not an ID")
            .context(
                ValidationIssue.Context.builder()
                    .name("Reference")
                    .jsonpath("$.foo.inputs.b[3]")
                    .data(garbage))
            .context(
                ValidationIssue.Context.builder()
                    .name("ReferenceSchema")
                    .message("inputs")
                    .data(schema.getReferenceSchemas().get("inputs")))
            .context(ValidationIssue.Context.builder().name("Body").jsonpath(prefix).data(data))
            .build());
  }
}
