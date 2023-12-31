package loom.graph.constraints;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import loom.graph.CommonEnvironments;
import loom.graph.LoomConstants;
import loom.graph.LoomEnvironment;
import loom.graph.nodes.ApplicationNode;
import loom.graph.nodes.TensorNode;
import loom.testing.BaseTestClass;
import loom.validation.ListValidationIssueCollector;
import loom.validation.ValidationIssue;
import loom.zspace.ZPoint;
import org.junit.Test;

public class ThereAreNoApplicationReferenceCyclesConstraintTest extends BaseTestClass {
  private final ThereAreNoApplicationReferenceCyclesConstraint constraint =
      new ThereAreNoApplicationReferenceCyclesConstraint();

  public LoomEnvironment createEnvironment() {
    return CommonEnvironments.expressionEnvironment();
  }

  @Test
  public void test_Empty() {
    var env = createEnvironment();
    var graph = env.newGraph();

    var collector = new ListValidationIssueCollector();
    constraint.validateConstraint(env, graph, collector);
    assertThat(collector.hasFailed()).isFalse();
  }

  @Test
  public void test_Cycles() {
    var env = createEnvironment();
    var graph = env.newGraph();

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

    var operationId = UUID.randomUUID();

    var opNode =
        ApplicationNode.withBody(
                b -> {
                  b.operationId(operationId);
                  b.input(
                      "x",
                      List.of(
                          ApplicationNode.TensorSelection.builder()
                              .tensorId(tensorA.getId())
                              .range(tensorA.getEffectiveRange())
                              .build(),
                          ApplicationNode.TensorSelection.builder()
                              .tensorId(tensorB.getId())
                              .range(tensorB.getEffectiveRange())
                              .build()));
                  b.output(
                      "y",
                      List.of(
                          ApplicationNode.TensorSelection.builder()
                              .tensorId(tensorC.getId())
                              .range(tensorC.getEffectiveRange())
                              .build(),
                          ApplicationNode.TensorSelection.builder()
                              .tensorId(tensorA.getId())
                              .range(tensorA.getEffectiveRange())
                              .build()));
                })
            .label("Add")
            .buildOn(graph);

    var issueCollector = new ListValidationIssueCollector();
    constraint.validateConstraint(env, graph, issueCollector);

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
                                "id", opNode.getId(), "type", ApplicationNode.TYPE, "label", "Add"),
                            Map.of("id", tensorA.getId(), "type", TensorNode.TYPE, "label", "A"))))
            .build());
  }
}
