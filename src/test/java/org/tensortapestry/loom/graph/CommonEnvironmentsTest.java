package org.tensortapestry.loom.graph;

import org.junit.Test;
import org.tensortapestry.loom.graph.dialects.common.NoteNode;
import org.tensortapestry.loom.graph.dialects.tensorops.ApplicationNode;
import org.tensortapestry.loom.graph.dialects.tensorops.OperationReferenceAgreementConstraint;
import org.tensortapestry.loom.graph.dialects.tensorops.OperationSignatureNode;
import org.tensortapestry.loom.graph.dialects.tensorops.TensorDTypesAreValidConstraint;
import org.tensortapestry.loom.graph.dialects.tensorops.TensorNode;
import org.tensortapestry.loom.graph.dialects.tensorops.TensorOpNodes;
import org.tensortapestry.loom.testing.BaseTestClass;

public class CommonEnvironmentsTest extends BaseTestClass {

  @Test
  public void test_expressionEnvironment() {
    var env = CommonEnvironments.expressionEnvironment();
    env.assertClassForType(NoteNode.NOTE_NODE_TYPE, NoteNode.class);
    env.assertClassForType(TensorOpNodes.TENSOR_NODE_TYPE, TensorNode.class);
    env.assertClassForType(
      TensorOpNodes.OPERATION_SIGNATURE_NODE_TYPE,
      OperationSignatureNode.class
    );
    env.assertClassForType(TensorOpNodes.APPLICATION_NODE_TYPE, ApplicationNode.class);

    env.assertConstraint(TensorDTypesAreValidConstraint.class);
    env.assertConstraint(OperationReferenceAgreementConstraint.class);
  }
}
