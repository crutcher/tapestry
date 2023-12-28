package loom.graph.nodes;

import loom.graph.LoomConstants;
import loom.graph.LoomEnvironment;
import loom.graph.LoomGraph;
import loom.testing.BaseTestClass;
import loom.validation.ListValidationIssueCollector;
import loom.validation.ValidationIssue;
import loom.zspace.ZPoint;
import loom.zspace.ZRange;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static loom.graph.LoomConstants.NODE_REFERENCE_ERROR;

public class ApplicationNodeSelectionsAreWellFormedConstraintTest extends BaseTestClass {
  public LoomEnvironment createEnv() {
    return LoomEnvironment.builder()
        .nodeMetaFactory(
            TypeMapNodeMetaFactory.builder()
                .typeMapping(
                    TensorNode.TYPE, TensorNode.Prototype.builder().validDType("int32").build())
                .typeMapping(ApplicationNode.TYPE, ApplicationNode.Prototype.builder().build())
                .typeMapping(NoteNode.TYPE, NoteNode.Prototype.builder().build())
                .build())
        .build()
        .addConstraint(new ApplicationNodeSelectionsAreWellFormedConstraint());
  }

  public LoomGraph createGraph() {
    return createEnv().createGraph();
  }

  @Test
  public void testValid() {
    var graph = createGraph();

    var inputTensor =
        TensorNode.withBody(b -> b.dtype("int32").shape(ZPoint.of(2, 3))).label("A").buildOn(graph);

    var outputTensor =
        TensorNode.withBody(b -> b.dtype("int32").shape(ZPoint.of(10)).origin(ZPoint.of(-100)))
            .label("B")
            .buildOn(graph);

    var operationId = UUID.randomUUID();

    var appNode =
        ApplicationNode.withBody(
                b ->
                    b.operationId(operationId)
                        .input(
                            "source",
                            List.of(
                                ApplicationNode.TensorSelection.builder()
                                    .tensorId(inputTensor.getId())
                                    .range(ZRange.fromShape(1, 2))
                                    .build()))
                        .output(
                            "result",
                            List.of(
                                ApplicationNode.TensorSelection.builder()
                                    .tensorId(outputTensor.getId())
                                    .range(ZRange.fromStartWithShape(ZPoint.of(-95), ZPoint.of(5)))
                                    .build())))
            .label("app")
            .buildOn(graph);

    graph.assertNode(inputTensor.getId(), TensorNode.TYPE, TensorNode.class);
    graph.assertNode(outputTensor.getId(), TensorNode.TYPE, TensorNode.class);
    graph.assertNode(appNode.getId(), ApplicationNode.TYPE, ApplicationNode.class);

    graph.validate();
  }

  @Test
  public void test_missing_tensor() {
    var graph = createGraph();

    var operationId = UUID.randomUUID();

    var missingTensorId = UUID.randomUUID();

    var apNode =
        ApplicationNode.withBody(
                b ->
                    b.operationId(operationId)
                        .input(
                            "source",
                            List.of(
                                ApplicationNode.TensorSelection.builder()
                                    .tensorId(missingTensorId)
                                    .range(ZRange.fromShape(1, 2))
                                    .build())))
            .label("app")
            .buildOn(graph);

    var collector = new ListValidationIssueCollector();
    graph.validate(collector);
    assertValidationIssues(
        collector.getIssues(),
        ValidationIssue.builder()
            .type(NODE_REFERENCE_ERROR)
            .param("nodeId", missingTensorId)
            .param("nodeType", TensorNode.TYPE)
            .summary("Referenced node does not exist")
            .context(
                context ->
                    context
                        .name("Reference")
                        .jsonpath(apNode.getJsonPath(), "body.inputs.source[0]")
                        .data(missingTensorId))
            .context(apNode.asContext("Application Node"))
            .build());
  }

  @Test
  public void test_wrong_reference_type() {
    var graph = createGraph();

    var operationId = UUID.randomUUID();

    var noteNode = NoteNode.withBody(b -> b.message("hello")).label("note").buildOn(graph);

    var apNode =
        ApplicationNode.withBody(
                b ->
                    b.operationId(operationId)
                        .input(
                            "source",
                            List.of(
                                ApplicationNode.TensorSelection.builder()
                                    .tensorId(noteNode.getId())
                                    .range(ZRange.fromShape(1, 2))
                                    .build())))
            .label("app")
            .buildOn(graph);

    var collector = new ListValidationIssueCollector();
    graph.validate(collector);
    assertValidationIssues(
        collector.getIssues(),
        ValidationIssue.builder()
            .type(NODE_REFERENCE_ERROR)
            .param("nodeId", noteNode.getId())
            .param("expectedType", TensorNode.TYPE)
            .param("actualType", noteNode.getType())
            .summary("Referenced node has the wrong type")
            .context(
                context ->
                    context
                        .name("Reference")
                        .jsonpath(apNode.getJsonPath(), "body.inputs.source[0]")
                        .data(noteNode.getId()))
            .context(apNode.asContext("Application Node"))
            .build());
  }

  @Test
  public void test_referenced_tensor_has_wrong_dimensions() {
    var graph = createGraph();

    var inputTensor =
        TensorNode.withBody(b -> b.dtype("int32").shape(ZPoint.of(2, 3))).label("A").buildOn(graph);

    var operationId = UUID.randomUUID();

    var appNode =
        ApplicationNode.withBody(
                b ->
                    b.operationId(operationId)
                        .input(
                            "source",
                            List.of(
                                ApplicationNode.TensorSelection.builder()
                                    .tensorId(inputTensor.getId())
                                    .range(ZRange.fromShape(100))
                                    .build())))
            .label("app")
            .buildOn(graph);

    var collector = new ListValidationIssueCollector();
    graph.validate(collector);
    assertValidationIssues(
        collector.getIssues(),
        ValidationIssue.builder()
            .type(LoomConstants.NODE_VALIDATION_ERROR)
            .param("nodeType", ApplicationNode.TYPE)
            .param("expectedDimensions", 1)
            .param("actualDimensions", 2)
            .summary("Tensor selection has the wrong number of dimensions")
            .context(
                context ->
                    context
                        .name("Selection Range")
                        .jsonpath(appNode.getJsonPath(), "body.inputs.source[0]")
                        .data(ZRange.fromShape(100)))
            .context(inputTensor.asContext("Tensor Node"))
            .context(appNode.asContext("Application Node"))
            .build());
  }

  @Test
  public void test_referenced_tensor_selection_is_outside_range() {
    var graph = createGraph();

    var inputTensor =
        TensorNode.withBody(b -> b.dtype("int32").shape(ZPoint.of(2, 3))).label("A").buildOn(graph);

    var operationId = UUID.randomUUID();

    var appNode =
        ApplicationNode.withBody(
                b ->
                    b.operationId(operationId)
                        .input(
                            "source",
                            List.of(
                                ApplicationNode.TensorSelection.builder()
                                    .tensorId(inputTensor.getId())
                                    .range(
                                        ZRange.fromStartWithShape(ZPoint.of(1, 2), ZPoint.of(5, 3)))
                                    .build())))
            .label("app")
            .buildOn(graph);

    var collector = new ListValidationIssueCollector();
    graph.validate(collector);
    assertValidationIssues(
        collector.getIssues(),
        ValidationIssue.builder()
            .type(LoomConstants.NODE_VALIDATION_ERROR)
            .param("nodeType", ApplicationNode.TYPE)
            .summary("Tensor selection is out of bounds")
            .context(
                context ->
                    context
                        .name("Selection Range")
                        .jsonpath(appNode.getJsonPath(), "body.inputs.source[0]")
                        .data(ZRange.fromStartWithShape(ZPoint.of(1, 2), ZPoint.of(5, 3))))
            .context(inputTensor.asContext("Tensor Node"))
            .context(appNode.asContext("Application Node"))
            .build());
  }
}
