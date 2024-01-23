package org.tensortapestry.loom.graph;

import java.util.stream.Collectors;
import org.tensortapestry.loom.common.json.WithSchema;
import org.tensortapestry.loom.graph.constraints.NodeBodySchemaConstraint;
import org.tensortapestry.loom.graph.constraints.OperationReferenceAgreementConstraint;
import org.tensortapestry.loom.graph.constraints.TensorDTypesAreValidConstraint;
import org.tensortapestry.loom.graph.nodes.ApplicationNode;
import org.tensortapestry.loom.graph.nodes.NoteNode;
import org.tensortapestry.loom.graph.nodes.OperationSignatureNode;
import org.tensortapestry.loom.graph.nodes.TensorNode;
import org.tensortapestry.loom.testing.BaseTestClass;
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
    env.assertConstraint(OperationReferenceAgreementConstraint.class);

    var m = env
      .getConstraints()
      .stream()
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
