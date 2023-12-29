package loom.graph.nodes;

import loom.graph.LoomConstants;
import loom.graph.LoomEnvironment;
import loom.graph.LoomGraph;
import loom.testing.BaseTestClass;
import loom.validation.ListValidationIssueCollector;
import loom.validation.ValidationIssue;
import loom.zspace.ZPoint;
import org.junit.Test;

import java.util.Set;

public class TensorDTypesAreValidConstraintTest extends BaseTestClass {
  public LoomEnvironment createEnv() {
    return LoomEnvironment.builder()
        .nodeMetaFactory(
            TypeMapNodeMetaFactory.builder()
                .typeMapping(TensorNode.TYPE, TensorNode.Prototype.builder().build())
                .typeMapping(NoteNode.TYPE, NoteNode.Prototype.builder().build())
                .build())
        .build()
        .addConstraint(new TensorDTypesAreValidConstraint(Set.of("int32", "float32")));
  }

  public LoomGraph createGraph() {
    return createEnv().createGraph();
  }

  @Test
  public void test() {
    var graph = createGraph();

    TensorNode.withBody(
            b -> {
              b.dtype("int32");
              b.shape(new ZPoint(2, 3));
            })
        .label("Good")
        .buildOn(graph);

    var badTensor =
        TensorNode.withBody(
                b -> {
                  b.dtype("nonesuch");
                  b.shape(new ZPoint(2, 3));
                })
            .label("Bad")
            .buildOn(graph);

    var collector = new ListValidationIssueCollector();
    graph.validate(collector);

    assertValidationIssues(
        collector.getIssues(),
        ValidationIssue.builder()
            .type(LoomConstants.NODE_VALIDATION_ERROR)
            .param("nodeType", TensorNode.TYPE)
            .summary("Tensor dtype (nonesuch) not a recognized type")
            .context(badTensor.asContext("Tensor"))
            .build());
  }
}
