package org.tensortapestry.loom.graph;

import org.junit.Test;
import org.tensortapestry.loom.graph.dialects.common.CommonNodes;
import org.tensortapestry.loom.graph.dialects.tensorops.OperationReferenceAgreementConstraint;
import org.tensortapestry.loom.graph.dialects.tensorops.TensorDTypesAreValidConstraint;
import org.tensortapestry.loom.graph.dialects.tensorops.TensorOpNodes;
import org.tensortapestry.loom.testing.BaseTestClass;

public class CommonEnvironmentsTest extends BaseTestClass {

  @Test
  public void test_expressionEnvironment() {
    var env = CommonEnvironments.expressionEnvironment();
    env.assertSupportsNodeType(CommonNodes.NOTE_NODE_TYPE);
    env.assertSupportsNodeType(TensorOpNodes.TENSOR_NODE_TYPE);
    env.assertSupportsNodeType(TensorOpNodes.OPERATION_NODE_TYPE);
    env.assertSupportsNodeType(TensorOpNodes.APPLICATION_NODE_TYPE);

    env.assertConstraint(TensorDTypesAreValidConstraint.class);
    env.assertConstraint(OperationReferenceAgreementConstraint.class);
  }
}
