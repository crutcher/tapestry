package loom.graph.nodes;

import java.util.UUID;
import loom.testing.BaseTestClass;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;
import loom.zspace.ZPoint;
import org.junit.Test;

public class TensorNodeTest extends BaseTestClass {
  @Test
  public void test_protoType() {
    var proto = TensorNode.Prototype.builder().validDType("int32").build();

    proto.addValidDType("float32");

    assertThat(proto.getValidDTypes()).contains("int32", "float32");

    {
      var issueCollector = new ValidationIssueCollector();
      var tensor =
          TensorNode.withBody(b -> b.dtype("xyz").shape(ZPoint.of(-2)))
              .id(UUID.randomUUID())
              .build();
      proto.validateNode(tensor, issueCollector);
      assertThat(issueCollector.getIssues())
          .containsOnly(
              ValidationIssue.builder()
                  .type("NodeValidationError")
                  .summary("dtype (xyz) must be one of [int32, float32]")
                  .build(),
              ValidationIssue.builder()
                  .type("NodeValidationError")
                  .summary("shape must be positive and non-empty: [-2]")
                  .build());
    }
  }
}
