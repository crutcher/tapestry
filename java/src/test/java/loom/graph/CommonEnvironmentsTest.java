package loom.graph;

import java.util.stream.Collectors;
import loom.common.json.WithSchema;
import loom.graph.constraints.ApplicationNodeSelectionsAreWellFormedConstraint;
import loom.graph.constraints.NodeBodySchemaConstraint;
import loom.graph.constraints.TensorDTypesAreValidConstraint;
import loom.graph.constraints.ThereAreNoApplicationReferenceCyclesConstraint;
import loom.graph.nodes.ApplicationNode;
import loom.graph.nodes.NoteNode;
import loom.graph.nodes.OperationSignatureNode;
import loom.graph.nodes.TensorNode;
import loom.testing.BaseTestClass;
import org.junit.Test;

public class CommonEnvironmentsTest extends BaseTestClass {
  @Test
  public void test_expressionEnvironment() {
    var env = CommonEnvironments.expressionEnvironment();
    env.assertClassForType(NoteNode.TYPE, NoteNode.class);
    env.assertClassForType(TensorNode.TYPE, TensorNode.class);
    env.assertClassForType(OperationSignatureNode.TYPE, OperationSignatureNode.class);
    env.assertClassForType(ApplicationNode.TYPE, ApplicationNode.class);

    env.assertConstraint(TensorDTypesAreValidConstraint.class);
    env.assertConstraint(ThereAreNoApplicationReferenceCyclesConstraint.class);
    env.assertConstraint(ApplicationNodeSelectionsAreWellFormedConstraint.class);

    var m =
        env.getConstraints().stream()
            .filter(c -> c instanceof NodeBodySchemaConstraint)
            .collect(Collectors.toMap(c -> ((NodeBodySchemaConstraint) c).getBodySchema(), c -> c));
    assertThat(m.get(TensorNode.Body.class.getAnnotation(WithSchema.class).value()))
        .extracting("nodeTypes")
        .asList()
        .containsOnly(TensorNode.TYPE);
    assertThat(m.get(ApplicationNode.Body.class.getAnnotation(WithSchema.class).value()))
        .extracting("nodeTypes")
        .asList()
        .containsOnly(ApplicationNode.TYPE);
  }
}
