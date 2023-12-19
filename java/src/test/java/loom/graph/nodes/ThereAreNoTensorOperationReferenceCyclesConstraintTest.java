package loom.graph.nodes;

import java.util.List;
import java.util.Map;
import loom.graph.CommonEnvironments;
import loom.graph.LoomConstants;
import loom.graph.LoomEnvironment;
import loom.testing.BaseTestClass;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;
import loom.zspace.ZPoint;
import org.junit.Test;

public class ThereAreNoTensorOperationReferenceCyclesConstraintTest extends BaseTestClass {
  private final ThereAreNoTensorOperationReferenceCyclesConstraint constraint =
      new ThereAreNoTensorOperationReferenceCyclesConstraint();

  public LoomEnvironment createEnvironment() {
    return CommonEnvironments.expressionEnvironment();
  }

  @Test
  public void test_Empty() {
    var env = createEnvironment();
    var graph = env.createGraph();

    var issueCollector = new ValidationIssueCollector();
    constraint.checkConstraint(env, graph, issueCollector);
    assertThat(issueCollector.isEmpty()).isTrue();
  }

  @Test
  public void test_Cycles() {
    var env = createEnvironment();
    var graph = env.createGraph();

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
        OperationNode.withBody(
                b -> {
                  b.opName("f");
                  b.input("x", List.of(tensorA.getId(), tensorB.getId()));
                  b.output("y", List.of(tensorC.getId(), tensorA.getId()));
                })
            .label("Add")
            .buildOn(graph);

    var issueCollector = new ValidationIssueCollector();
    constraint.checkConstraint(env, graph, issueCollector);

    System.out.println(issueCollector.toDisplayString());

    assertThat(issueCollector.getIssues())
        .containsOnly(
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
                                    OperationNode.TYPE,
                                    "label",
                                    "Add"),
                                Map.of(
                                    "id", tensorA.getId(), "type", TensorNode.TYPE, "label", "A"))))
                .build());
  }
}
