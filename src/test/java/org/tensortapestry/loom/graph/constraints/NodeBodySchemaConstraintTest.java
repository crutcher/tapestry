package org.tensortapestry.loom.graph.constraints;

import java.util.regex.Pattern;
import org.tensortapestry.loom.common.json.WithSchema;
import org.tensortapestry.loom.graph.LoomEnvironment;
import org.tensortapestry.loom.graph.nodes.NoteNode;
import org.tensortapestry.loom.graph.nodes.TensorNode;
import org.tensortapestry.loom.testing.BaseTestClass;
import org.tensortapestry.loom.zspace.ZPoint;
import org.junit.Test;

public class NodeBodySchemaConstraintTest extends BaseTestClass {

  @SuppressWarnings("unused")
  public static class BodyWithoutSchema {

    public String message;
  }

  @Test
  public void test_bodyWithSchema_byName() {
    var env = LoomEnvironment
      .builder()
      .build()
      .addNodeTypeClass(NoteNode.TYPE, NoteNode.class)
      .addNodeTypeClass(TensorNode.TYPE, TensorNode.class)
      .addConstraint(
        NodeBodySchemaConstraint
          .builder()
          .nodeType(TensorNode.TYPE)
          .withSchemaFromNodeClass(TensorNode.class)
          .build()
      );
    var graph = env.newGraph();

    NoteNode.withBody(b -> b.message("hello")).addTo(graph);

    TensorNode.withBody(b -> b.dtype("int32").shape(ZPoint.of(2, 3))).addTo(graph);

    graph.validate();
  }

  @Test
  public void test_bodyWithSchema_byPattern() {
    var env = LoomEnvironment
      .builder()
      .build()
      .addNodeTypeClass(NoteNode.TYPE, NoteNode.class)
      .addNodeTypeClass(TensorNode.TYPE, TensorNode.class)
      .addConstraint(
        NodeBodySchemaConstraint
          .builder()
          .nodeTypePattern(Pattern.compile("TensorNode"))
          .withSchemaFromNodeClass(TensorNode.class)
          .build()
      );
    var graph = env.newGraph();

    NoteNode.withBody(b -> b.message("hello")).addTo(graph);

    TensorNode.withBody(b -> b.dtype("int32").shape(ZPoint.of(2, 3))).addTo(graph);

    graph.validate();
  }

  @Test
  public void test_builder() {
    String tensorBodySchema = TensorNode.Body.class.getAnnotation(WithSchema.class).value();
    {
      var constraint = NodeBodySchemaConstraint
        .builder()
        .nodeType(TensorNode.TYPE)
        .withSchemaFromBodyClass(TensorNode.Body.class)
        .build();

      assertThat(constraint.getNodeTypes()).containsOnly(TensorNode.TYPE);
      assertThat(constraint.getBodySchema()).isEqualTo(tensorBodySchema);
    }
    {
      var constraint = NodeBodySchemaConstraint
        .builder()
        .nodeType(TensorNode.TYPE)
        .withSchemaFromBodyClass(TensorNode.Body.class)
        .build();

      assertThat(constraint.getNodeTypes()).containsOnly(TensorNode.TYPE);
      assertThat(constraint.getBodySchema()).isEqualTo(tensorBodySchema);
    }

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() ->
        NodeBodySchemaConstraint.builder().withSchemaFromBodyClass(BodyWithoutSchema.class)
      )
      .withMessageContaining("does not have a @WithSchema annotation");
  }
}
