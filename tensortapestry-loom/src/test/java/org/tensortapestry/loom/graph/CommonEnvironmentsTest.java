package org.tensortapestry.loom.graph;

import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.BaseTestClass;
import org.tensortapestry.loom.graph.dialects.common.NoteNode;
import org.tensortapestry.loom.graph.dialects.tensorops.*;

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
