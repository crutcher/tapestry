package org.tensortapestry.loom.graph;

import org.junit.Test;
import org.tensortapestry.loom.graph.dialects.common.NoteNode;
import org.tensortapestry.loom.graph.dialects.tensorops.*;
import org.tensortapestry.loom.testing.BaseTestClass;

public class CommonEnvironmentsTest extends BaseTestClass {

  @Test
  public void test_expressionEnvironment() {
    var env = CommonEnvironments.expressionEnvironment();
    env.assertSupportsNodeType(NoteNode.TYPE);
    env.assertSupportsNodeType(TensorNode.TYPE);
    env.assertSupportsNodeType(OperationNode.TYPE);
    env.assertSupportsNodeType(ApplicationNode.TYPE);

    env.assertConstraint(TensorDTypesAreValidConstraint.class);
    env.assertConstraint(OperationReferenceAgreementConstraint.class);
  }
}
