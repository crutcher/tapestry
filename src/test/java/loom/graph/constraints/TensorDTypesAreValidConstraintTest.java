package loom.graph.constraints;

import java.util.Set;
import loom.graph.LoomConstants;
import loom.graph.LoomEnvironment;
import loom.graph.LoomGraph;
import loom.graph.nodes.NoteNode;
import loom.graph.nodes.TensorNode;
import loom.testing.BaseTestClass;
import loom.validation.ListValidationIssueCollector;
import loom.validation.ValidationIssue;
import loom.zspace.ZPoint;
import org.junit.Test;

public class TensorDTypesAreValidConstraintTest extends BaseTestClass {

  public LoomEnvironment createEnv() {
    return LoomEnvironment
      .builder()
      .build()
      .addNodeTypeClass(NoteNode.TYPE, NoteNode.class)
      .addNodeTypeClass(TensorNode.TYPE, TensorNode.class)
      .addConstraint(new TensorDTypesAreValidConstraint(Set.of("int32", "float32")));
  }

  public LoomGraph createGraph() {
    return createEnv().newGraph();
  }

  @Test
  public void test() {
    var graph = createGraph();

    TensorNode
      .withBody(b -> {
        b.dtype("int32");
        b.shape(new ZPoint(2, 3));
      })
      .label("Good")
      .addTo(graph);

    var badTensor = TensorNode
      .withBody(b -> {
        b.dtype("nonesuch");
        b.shape(new ZPoint(2, 3));
      })
      .label("Bad")
      .addTo(graph);

    var collector = new ListValidationIssueCollector();
    graph.validate(collector);

    assertValidationIssues(
      collector.getIssues(),
      ValidationIssue
        .builder()
        .type(LoomConstants.NODE_VALIDATION_ERROR)
        .param("nodeType", TensorNode.TYPE)
        .summary("Tensor dtype (nonesuch) not a recognized type")
        .context(badTensor.asValidationContext("Tensor"))
        .build()
    );
  }
}
