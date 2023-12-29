package loom.graph.constraints;

import static loom.graph.LoomConstants.MISSING_NODE_ERROR;
import static loom.graph.LoomConstants.NODE_VALIDATION_ERROR;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import loom.graph.CommonEnvironments;
import loom.graph.LoomGraph;
import loom.graph.nodes.NoteNode;
import loom.graph.nodes.OperationNode;
import loom.graph.nodes.TensorNode;
import loom.testing.BaseTestClass;
import loom.validation.ListValidationIssueCollector;
import loom.validation.ValidationIssue;
import loom.zspace.ZPoint;
import org.junit.Test;

public class OperationNodesSourcesAndResultsAreTensorsTest extends BaseTestClass {
  private final OperationNodesSourcesAndResultsAreTensors constraint =
      new OperationNodesSourcesAndResultsAreTensors();

  private LoomGraph testGraph() {
    var env = CommonEnvironments.simpleTensorEnvironment("int32").addConstraint(constraint);
    return env.createGraph();
  }

  @Test
  public void testCheckGraph() {
    var graph = testGraph();

    var badId = UUID.randomUUID();

    var note = NoteNode.withBody(b -> b.message("Hello World")).buildOn(graph);

    var tensor =
        TensorNode.withBody(
                b -> {
                  b.dtype("int32");
                  b.shape(ZPoint.of(1));
                })
            .buildOn(graph);

    var op =
        OperationNode.withBody(
                b -> {
                  b.opName("source");
                  b.inputs(Map.of("sources", List.of(note.getId(), tensor.getId(), badId)));
                  b.outputs(Map.of("results", List.of(tensor.getId(), note.getId())));
                })
            .buildOn(graph);

    var commonContexts = List.of(op.asContext("Operation"));

    var collector = new ListValidationIssueCollector();
    constraint.validateConstraint(graph.getEnv(), graph, collector);

    assertValidationIssues(
        collector.getIssues(),
        ValidationIssue.builder()
            .type(NODE_VALIDATION_ERROR)
            .summary("Operation .body.inputs.sources[0] references non-tensor node.")
            .context(
                note.asContext("Reference Node", "The node referenced by .body.inputs.sources[0]"))
            .withContexts(commonContexts)
            .build(),
        ValidationIssue.builder()
            .type(MISSING_NODE_ERROR)
            .param("nodeType", OperationNode.TYPE)
            .summary("Operation .body.inputs.sources[2] references non-existent node.")
            .context(
                ValidationIssue.Context.builder()
                    .name("Reference")
                    .message("Invalid reference to non-existent node.")
                    .jsonpath(op.getJsonPath(), ".body.inputs.sources[2]")
                    .data(badId))
            .withContexts(commonContexts)
            .build(),
        ValidationIssue.builder()
            .type(NODE_VALIDATION_ERROR)
            .summary("Operation .body.outputs.results[1] references non-tensor node.")
            .context(
                note.asContext("Reference Node", "The node referenced by .body.outputs.results[1]"))
            .withContexts(commonContexts)
            .build());
  }
}
