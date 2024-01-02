package loom.graph.constraints;

import loom.graph.CommonEnvironments;
import loom.graph.LoomConstants;
import loom.graph.LoomGraph;
import loom.graph.nodes.*;
import loom.testing.BaseTestClass;
import loom.validation.ListValidationIssueCollector;
import loom.validation.ValidationIssue;
import loom.zspace.ZPoint;
import loom.zspace.ZRange;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static loom.graph.LoomConstants.NODE_REFERENCE_ERROR;

public class OperationReferenceAgreementConstraintTest extends BaseTestClass {

  public LoomGraph createGraph() {
    var env = CommonEnvironments.expressionEnvironment();
    env.assertConstraint(OperationReferenceAgreementConstraint.class);
    return env.newGraph();
  }

  @Test
  public void test_Empty() {
    var graph = createGraph();
    graph.validate();
  }

  @Test
  public void test_valid() {
    var graph = createGraph();

    var tensorA =
        TensorNode.withBody(
                b -> {
                  b.dtype("int32");
                  b.shape(new ZPoint(2, 3));
                })
            .label("A")
            .buildOn(graph);

    var sourceOp =
        OperationSignatureNode.withBody(
                b -> {
                  b.name("source");
                  b.output(
                      "output",
                      List.of(new TensorSelection(tensorA.getId(), tensorA.getEffectiveRange())));
                })
            .buildOn(graph);

    ApplicationNode.withBody(
            b -> {
              b.operationId(sourceOp.getId());
              b.output(
                  "output",
                  List.of(
                      TensorSelection.builder()
                          .tensorId(tensorA.getId())
                          .range(new ZRange(ZPoint.of(0, 0), ZPoint.of(1, 3)))
                          .build()));
            })
        .buildOn(graph);
    ApplicationNode.withBody(
            b -> {
              b.operationId(sourceOp.getId());
              b.output(
                  "output",
                  List.of(
                      TensorSelection.builder()
                          .tensorId(tensorA.getId())
                          .range(new ZRange(ZPoint.of(1, 0), ZPoint.of(2, 3)))
                          .build()));
            })
        .buildOn(graph);

    var sinkOp =
        OperationSignatureNode.withBody(
                b -> {
                  b.name("sink");
                  b.input(
                      "input",
                      List.of(new TensorSelection(tensorA.getId(), tensorA.getEffectiveRange())));
                })
            .buildOn(graph);

    ApplicationNode.withBody(
            b -> {
              b.operationId(sinkOp.getId());
              b.input(
                  "input",
                  List.of(
                      TensorSelection.builder()
                          .tensorId(tensorA.getId())
                          .range(new ZRange(ZPoint.of(0, 0), ZPoint.of(2, 1)))
                          .build()));
            })
        .buildOn(graph);
    ApplicationNode.withBody(
            b -> {
              b.operationId(sinkOp.getId());
              b.input(
                  "input",
                  List.of(
                      TensorSelection.builder()
                          .tensorId(tensorA.getId())
                          .range(new ZRange(ZPoint.of(0, 1), ZPoint.of(2, 3)))
                          .build()));
            })
        .buildOn(graph);

    graph.validate();
  }

  @Test
  public void test_missing_tensor() {
    var graph = createGraph();

    var missingInputId = UUID.randomUUID();
    var missingOutputId = UUID.randomUUID();

    @SuppressWarnings("unused")
    var op =
        OperationSignatureNode.withBody(
                b -> {
                  b.name("source");
                  b.input(
                      "source",
                      List.of(new TensorSelection(missingInputId, ZRange.fromShape(1, 2))));
                  b.output(
                      "result",
                      List.of(new TensorSelection(missingOutputId, ZRange.fromShape(10))));
                })
            .buildOn(graph);
    ApplicationNode.withBody(
            b -> {
              b.operationId(op.getId());
              b.input(
                  "source",
                  List.of(
                      TensorSelection.builder()
                          .tensorId(missingInputId)
                          .range(ZRange.fromShape(1, 2))
                          .build()));
              b.output(
                  "result",
                  List.of(
                      TensorSelection.builder()
                          .tensorId(missingOutputId)
                          .range(ZRange.fromShape(10))
                          .build()));
            })
        .buildOn(graph);

    var constraint = graph.getEnv().assertConstraint(OperationReferenceAgreementConstraint.class);

    var issueCollector = new ListValidationIssueCollector();
    constraint.validateConstraint(graph.getEnv(), graph, issueCollector);
    assertValidationIssues(
        issueCollector.getIssues(),
        ValidationIssue.builder()
            .type(NODE_REFERENCE_ERROR)
            .param("nodeId", missingInputId)
            .param("nodeType", TensorNode.TYPE)
            .summary("Referenced node does not exist")
            .context(
                context ->
                    context
                        .name("Reference")
                        .jsonpath(op.getJsonPath(), "body.inputs.source[0]")
                        .data(missingInputId))
            .context(op.asValidationContext("Operation Node"))
            .build(),
        ValidationIssue.builder()
            .type(NODE_REFERENCE_ERROR)
            .param("nodeId", missingOutputId)
            .param("nodeType", TensorNode.TYPE)
            .summary("Referenced node does not exist")
            .context(
                context ->
                    context
                        .name("Reference")
                        .jsonpath(op.getJsonPath(), "body.outputs.result[0]")
                        .data(missingOutputId))
            .context(op.asValidationContext("Operation Node"))
            .build());
  }

  @Test
  public void test_wrong_reference_type() {
    var graph = createGraph();

    var noteNode = NoteNode.withBody(b -> b.message("hello")).buildOn(graph);

    @SuppressWarnings("unused")
    var op =
        OperationSignatureNode.withBody(
                b -> {
                  b.name("source");
                  b.input(
                      "source",
                      List.of(new TensorSelection(noteNode.getId(), ZRange.fromShape(1, 2))));
                  b.output(
                      "result",
                      List.of(new TensorSelection(noteNode.getId(), ZRange.fromShape(10))));
                })
            .buildOn(graph);
    ApplicationNode.withBody(
            b -> {
              b.operationId(op.getId());
              b.input(
                  "source",
                  List.of(
                      TensorSelection.builder()
                          .tensorId(noteNode.getId())
                          .range(ZRange.fromShape(1, 2))
                          .build()));
              b.output(
                  "result",
                  List.of(
                      TensorSelection.builder()
                          .tensorId(noteNode.getId())
                          .range(ZRange.fromShape(10))
                          .build()));
            })
        .buildOn(graph);

    var constraint = graph.getEnv().assertConstraint(OperationReferenceAgreementConstraint.class);

    var issueCollector = new ListValidationIssueCollector();
    constraint.validateConstraint(graph.getEnv(), graph, issueCollector);

    assertValidationIssues(
        issueCollector.getIssues(),
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
                        .jsonpath(op.getJsonPath(), "body.inputs.source[0]")
                        .data(noteNode.getId()))
            .context(op.asValidationContext("Operation Node"))
            .build(),
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
                        .jsonpath(op.getJsonPath(), "body.outputs.result[0]")
                        .data(noteNode.getId()))
            .context(op.asValidationContext("Operation Node"))
            .build());
  }

  @Test
  public void test_referenced_tensor_has_wrong_shape() {
    var graph = createGraph();

    var tensorA =
        TensorNode.withBody(
                b -> {
                  b.dtype("int32");
                  b.shape(new ZPoint(2, 3));
                })
            .label("A")
            .buildOn(graph);

    var sourceOp =
        OperationSignatureNode.withBody(
                b -> {
                  b.name("source");
                  b.output(
                      "output",
                      List.of(new TensorSelection(tensorA.getId(), ZRange.fromShape(200))));
                })
            .buildOn(graph);

    ApplicationNode.withBody(
            b -> {
              b.operationId(sourceOp.getId());
              b.output(
                  "output", List.of(new TensorSelection(tensorA.getId(), ZRange.fromShape(200))));
            })
        .buildOn(graph);

    var sinkOp =
        OperationSignatureNode.withBody(
                b -> {
                  b.name("sink");
                  b.input(
                      "input",
                      List.of(new TensorSelection(tensorA.getId(), ZRange.fromShape(200))));
                })
            .buildOn(graph);

    ApplicationNode.withBody(
            b -> {
              b.operationId(sinkOp.getId());
              b.input(
                  "input", List.of(new TensorSelection(tensorA.getId(), ZRange.fromShape(200))));
            })
        .buildOn(graph);

    var constraint = graph.getEnv().assertConstraint(OperationReferenceAgreementConstraint.class);

    var issueCollector = new ListValidationIssueCollector();
    constraint.validateConstraint(graph.getEnv(), graph, issueCollector);
    assertValidationIssues(
        issueCollector.getIssues(),
        ValidationIssue.builder()
            .type(LoomConstants.NODE_VALIDATION_ERROR)
            .param("nodeType", OperationSignatureNode.TYPE)
            .param("expectedDimensions", 1)
            .param("actualDimensions", 2)
            .summary("Tensor selection has the wrong number of dimensions")
            .context(
                context ->
                    context
                        .name("Selection Range")
                        .jsonpath(sourceOp.getJsonPath(), "body.outputs.output[0]")
                        .data(ZRange.fromShape(200)))
            .context(tensorA.asValidationContext("Tensor Node"))
            .context(sourceOp.asValidationContext("Operation Node"))
            .build(),
        ValidationIssue.builder()
            .type(LoomConstants.NODE_VALIDATION_ERROR)
            .param("nodeType", OperationSignatureNode.TYPE)
            .param("expectedDimensions", 1)
            .param("actualDimensions", 2)
            .summary("Tensor selection has the wrong number of dimensions")
            .context(
                context ->
                    context
                        .name("Selection Range")
                        .jsonpath(sinkOp.getJsonPath(), "body.inputs.input[0]")
                        .data(ZRange.fromShape(200)))
            .context(tensorA.asValidationContext("Tensor Node"))
            .context(sinkOp.asValidationContext("Operation Node"))
            .build());
  }

  @Test
  public void test_tensor_selection_is_outside_range() {
    var graph = createGraph();

    var tensorA =
        TensorNode.withBody(
                b -> {
                  b.dtype("int32");
                  b.shape(new ZPoint(2, 3));
                })
            .label("A")
            .buildOn(graph);

    var sourceOp =
        OperationSignatureNode.withBody(
                b -> {
                  b.name("source");
                  b.output(
                      "output",
                      List.of(new TensorSelection(tensorA.getId(), ZRange.fromShape(5, 10))));
                })
            .buildOn(graph);

    ApplicationNode.withBody(
            b -> {
              b.operationId(sourceOp.getId());
              b.output(
                  "output", List.of(new TensorSelection(tensorA.getId(), ZRange.fromShape(5, 10))));
            })
        .buildOn(graph);

    var sinkOp =
        OperationSignatureNode.withBody(
                b -> {
                  b.name("sink");
                  b.input(
                      "input",
                      List.of(new TensorSelection(tensorA.getId(), ZRange.fromShape(2, 8))));
                })
            .buildOn(graph);

    ApplicationNode.withBody(
            b -> {
              b.operationId(sinkOp.getId());
              b.input(
                  "input", List.of(new TensorSelection(tensorA.getId(), ZRange.fromShape(2, 8))));
            })
        .buildOn(graph);

    var constraint = graph.getEnv().assertConstraint(OperationReferenceAgreementConstraint.class);
    var issueCollector = new ListValidationIssueCollector();
    constraint.validateConstraint(graph.getEnv(), graph, issueCollector);
    assertValidationIssues(
        issueCollector.getIssues(),
        ValidationIssue.builder()
            .type(LoomConstants.NODE_VALIDATION_ERROR)
            .param("nodeType", OperationSignatureNode.TYPE)
            .summary("Tensor selection is out of bounds")
            .context(
                context ->
                    context
                        .name("Selection Range")
                        .jsonpath(sinkOp.getJsonPath(), "body.inputs.input[0]")
                        .data(ZRange.fromShape(2, 8)))
            .context(tensorA.asValidationContext("Tensor Node"))
            .context(sinkOp.asValidationContext("Operation Node"))
            .build(),
        ValidationIssue.builder()
            .type(LoomConstants.NODE_VALIDATION_ERROR)
            .param("nodeType", OperationSignatureNode.TYPE)
            .summary("Tensor selection is out of bounds")
            .context(
                context ->
                    context
                        .name("Selection Range")
                        .jsonpath(sourceOp.getJsonPath(), "body.outputs.output[0]")
                        .data(ZRange.fromShape(5, 10)))
            .context(tensorA.asValidationContext("Tensor Node"))
            .context(sourceOp.asValidationContext("Operation Node"))
            .build());
  }

  @Test
  public void test_Cycles() {
    var graph = createGraph();

    var tensorA =
        TensorNode.withBody(
                b -> {
                  b.dtype("int32");
                  b.shape(new ZPoint(2, 3));
                })
            .label("A")
            .buildOn(graph);

    var tensorB =
        TensorNode.withBody(
                b -> {
                  b.dtype("int32");
                  b.shape(new ZPoint(4, 5));
                })
            .label("B")
            .buildOn(graph);

    var tensorC =
        TensorNode.withBody(
                b -> {
                  b.dtype("int32");
                  b.shape(new ZPoint(6, 7));
                })
            .label("C")
            .buildOn(graph);

    var opNode =
        OperationSignatureNode.withBody(
                b -> {
                  b.name("increment");
                  b.input(
                      "x",
                      List.of(
                          TensorSelection.builder()
                              .tensorId(tensorA.getId())
                              .range(tensorA.getEffectiveRange())
                              .build(),
                          TensorSelection.builder()
                              .tensorId(tensorB.getId())
                              .range(tensorB.getEffectiveRange())
                              .build()));
                  b.output(
                      "y",
                      List.of(
                          TensorSelection.builder()
                              .tensorId(tensorC.getId())
                              .range(tensorC.getEffectiveRange())
                              .build(),
                          TensorSelection.builder()
                              .tensorId(tensorA.getId())
                              .range(tensorA.getEffectiveRange())
                              .build()));
                })
            .label("Add")
            .buildOn(graph);

    ApplicationNode.withBody(
            b ->
                b.operationId(opNode.getId())
                    .input(
                        "x",
                        List.of(
                            TensorSelection.builder()
                                .tensorId(tensorA.getId())
                                .range(tensorA.getEffectiveRange())
                                .build(),
                            TensorSelection.builder()
                                .tensorId(tensorB.getId())
                                .range(tensorB.getEffectiveRange())
                                .build()))
                    .output(
                        "y",
                        List.of(
                            TensorSelection.builder()
                                .tensorId(tensorC.getId())
                                .range(tensorC.getEffectiveRange())
                                .build(),
                            TensorSelection.builder()
                                .tensorId(tensorA.getId())
                                .range(tensorA.getEffectiveRange())
                                .build())))
        .buildOn(graph);

    var constraint = graph.getEnv().assertConstraint(OperationReferenceAgreementConstraint.class);
    var issueCollector = new ListValidationIssueCollector();
    constraint.validateConstraint(graph.getEnv(), graph, issueCollector);
    assertValidationIssues(
        issueCollector.getIssues(),
        ValidationIssue.builder()
            .type(LoomConstants.REFERENCE_CYCLE_ERROR)
            .summary("Reference Cycle detected")
            .context(
                ValidationIssue.Context.builder()
                    .name("Cycle")
                    .data(
                        List.of(
                            Map.of(
                                "id",
                                opNode.getId(),
                                "type",
                                OperationSignatureNode.TYPE,
                                "label",
                                "Add"),
                            Map.of("id", tensorA.getId(), "type", TensorNode.TYPE, "label", "A"))))
            .build());
  }
}
