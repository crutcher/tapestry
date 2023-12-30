package loom.graph.constraints;

import loom.common.json.WithSchema;
import loom.graph.nodes.TensorNode;
import loom.testing.BaseTestClass;
import org.junit.Test;

public class NodeBodySchemaConstraintTest extends BaseTestClass {
  @SuppressWarnings("unused")
  public static class BodyWithoutSchema {
    public String message;
  }

  @Test
  public void test_builder() {
    String tensorBodySchema = TensorNode.Body.class.getAnnotation(WithSchema.class).value();
    {
      var constraint =
          NodeBodySchemaConstraint.builder()
              .nodeType(TensorNode.TYPE)
              .withSchemaFrom(TensorNode.Body.class)
              .build();

      assertThat(constraint.getNodeType()).isEqualTo(TensorNode.TYPE);
      assertThat(constraint.getBodySchema()).isEqualTo(tensorBodySchema);
    }
    {
      var constraint =
          NodeBodySchemaConstraint.builder()
              .nodeType(TensorNode.TYPE)
              .withSchemaFrom(TensorNode.Body.class)
              .build();

      assertThat(constraint.getNodeType()).isEqualTo(TensorNode.TYPE);
      assertThat(constraint.getBodySchema()).isEqualTo(tensorBodySchema);
    }

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> NodeBodySchemaConstraint.builder().withSchemaFrom(BodyWithoutSchema.class))
        .withMessageContaining("does not have a @WithSchema annotation");
  }
}
