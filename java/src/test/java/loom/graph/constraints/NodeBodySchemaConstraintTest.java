package loom.graph.constraints;

import loom.common.json.WithSchema;
import loom.graph.LoomEnvironment;
import loom.graph.nodes.TensorNode;
import loom.testing.BaseTestClass;
import loom.zspace.ZPoint;
import org.junit.Test;

public class NodeBodySchemaConstraintTest extends BaseTestClass {
  @SuppressWarnings("unused")
  public static class BodyWithoutSchema {
    public String message;
  }

  @Test
  public void test_bodyWithSchema() {
    var env =
        LoomEnvironment.builder()
            .build()
            .addNodeTypeClass(TensorNode.TYPE, TensorNode.class)
            .addConstraint(
                NodeBodySchemaConstraint.builder()
                    .nodeType(TensorNode.TYPE)
                    .withSchemaFromNodeClass(TensorNode.class)
                    .build());
    var graph = env.newGraph();

    TensorNode.withBody(b -> b.dtype("int32").shape(ZPoint.of(2, 3))).buildOn(graph);

    graph.validate();
  }

  @Test
  public void test_builder() {
    String tensorBodySchema = TensorNode.Body.class.getAnnotation(WithSchema.class).value();
    {
      var constraint =
          NodeBodySchemaConstraint.builder()
              .nodeType(TensorNode.TYPE)
              .withSchemaFromBodyClass(TensorNode.Body.class)
              .build();

      assertThat(constraint.getNodeTypes()).containsOnly(TensorNode.TYPE);
      assertThat(constraint.getBodySchema()).isEqualTo(tensorBodySchema);
    }
    {
      var constraint =
          NodeBodySchemaConstraint.builder()
              .nodeType(TensorNode.TYPE)
              .withSchemaFromBodyClass(TensorNode.Body.class)
              .build();

      assertThat(constraint.getNodeTypes()).containsOnly(TensorNode.TYPE);
      assertThat(constraint.getBodySchema()).isEqualTo(tensorBodySchema);
    }

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () ->
                NodeBodySchemaConstraint.builder().withSchemaFromBodyClass(BodyWithoutSchema.class))
        .withMessageContaining("does not have a @WithSchema annotation");
  }
}
