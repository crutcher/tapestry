package org.tensortapestry.loom.graph;

import org.junit.Test;
import org.tensortapestry.loom.graph.constraints.OperationReferenceAgreementConstraint;
import org.tensortapestry.loom.graph.constraints.TensorDTypesAreValidConstraint;
import org.tensortapestry.loom.graph.nodes.ApplicationNode;
import org.tensortapestry.loom.graph.nodes.NoteNode;
import org.tensortapestry.loom.graph.nodes.OperationSignatureNode;
import org.tensortapestry.loom.graph.nodes.TensorNode;
import org.tensortapestry.loom.testing.BaseTestClass;

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
  }
}
