package org.tensortapestry.loom.graph.dialects.tensorops;

import org.junit.Test;
import org.tensortapestry.common.testing.BaseTestClass;
import org.tensortapestry.common.validation.ListValidationIssueCollector;
import org.tensortapestry.common.validation.ValidationIssue;
import org.tensortapestry.loom.graph.CommonEnvironments;
import org.tensortapestry.loom.graph.LoomConstants;
import org.tensortapestry.loom.graph.LoomEnvironment;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.zspace.ZPoint;

public class TensorDTypesAreValidConstraintTest extends BaseTestClass {

  public LoomEnvironment createEnv() {
    return CommonEnvironments.expressionEnvironment();
  }

  public LoomGraph createGraph() {
    return createEnv().newGraph();
  }

  @Test
  public void test() {
    var graph = createGraph();

    TensorNode
      .builder(graph)
      .label("Good")
      .configure(b -> {
        b.dtype("int32");
        b.shape(new ZPoint(2, 3));
      })
      .build();

    var badTensor = TensorNode
      .builder(graph)
      .label("Bad")
      .configure(b -> {
        b.dtype("nonesuch");
        b.shape(new ZPoint(2, 3));
      })
      .build();

    var collector = new ListValidationIssueCollector();
    graph.validate(collector);

    assertValidationIssues(
      collector.getIssues(),
      ValidationIssue
        .builder()
        .type(LoomConstants.Errors.NODE_VALIDATION_ERROR)
        .param("nodeType", TensorNode.TYPE)
        .summary("Tensor dtype (nonesuch) not a recognized type")
        .context(badTensor.asValidationContext("Tensor"))
        .build()
    );
  }
}
