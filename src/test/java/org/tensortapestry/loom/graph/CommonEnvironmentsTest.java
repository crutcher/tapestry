package org.tensortapestry.loom.graph;

import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.CommonAssertions;
import org.tensortapestry.loom.graph.dialects.common.NoteNode;
import org.tensortapestry.loom.graph.dialects.tensorops.*;
import org.tensortapestry.loom.graph.dialects.tensorops.constraints.NoTensorOperationCyclesConstraint;
import org.tensortapestry.loom.graph.dialects.tensorops.constraints.OperationApplicationAgreementConstraint;
import org.tensortapestry.loom.graph.dialects.tensorops.constraints.TensorDTypesAreValidConstraint;
import org.tensortapestry.loom.graph.dialects.tensorops.constraints.TensorOperationAgreementConstraint;

public class CommonEnvironmentsTest implements CommonAssertions {

  @Test
  public void test_expressionEnvironment() {
    var env = ApplicationExpressionDialect.ENVIRONMENT;
    env.assertSupportsNodeType(NoteNode.TYPE);
    env.assertSupportsNodeType(TensorNode.TYPE);
    env.assertSupportsNodeType(OperationNode.TYPE);
    env.assertSupportsNodeType(ApplicationNode.TYPE);

    env.assertConstraint(TensorDTypesAreValidConstraint.class);
    env.assertConstraint(TensorOperationAgreementConstraint.class);
    env.assertConstraint(NoTensorOperationCyclesConstraint.class);
    env.assertConstraint(OperationApplicationAgreementConstraint.class);
  }
}
