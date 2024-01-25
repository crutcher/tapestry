package org.tensortapestry.loom.graph;

import org.junit.Test;
import org.tensortapestry.loom.graph.dialects.common.NoteNode;
import org.tensortapestry.loom.graph.dialects.tensorops.ApplicationNode;
import org.tensortapestry.loom.graph.dialects.tensorops.TensorNode;
import org.tensortapestry.loom.graph.dialects.tensorops.TensorOpNodes;
import org.tensortapestry.loom.testing.BaseTestClass;
import org.tensortapestry.loom.validation.ValidationIssueCollector;

public class LoomEnvironmentTest extends BaseTestClass {

  @Test
  public void test_builder() {
    var env = LoomEnvironment
      .builder()
      .build()
      .addNodeTypeClass(TensorOpNodes.TENSOR_NODE_TYPE, TensorNode.class)
      .addNodeTypeClass(NoteNode.NOTE_NODE_TYPE, NoteNode.class);

    assertThat(env.supportsNodeType(TensorOpNodes.TENSOR_NODE_TYPE)).isTrue();
    assertThat(env.supportsNodeType(NoteNode.NOTE_NODE_TYPE)).isTrue();

    assertThat(env.classForType(TensorOpNodes.TENSOR_NODE_TYPE)).isEqualTo(TensorNode.class);
    assertThat(env.classForType(NoteNode.NOTE_NODE_TYPE)).isEqualTo(NoteNode.class);

    assertThat(env.supportsNodeType(TensorOpNodes.APPLICATION_NODE_TYPE)).isFalse();
    assertThatExceptionOfType(IllegalStateException.class)
      .isThrownBy(() -> env.assertClassForType(TensorOpNodes.APPLICATION_NODE_TYPE));
  }

  @Test
  public void test_annotations() {
    var env = LoomEnvironment.builder().build();

    assertThat(env.getAnnotationTypeClasses()).isEmpty();

    env.addAnnotationTypeClass("foo", String.class).addAnnotationTypeClass("bar", Integer.class);

    assertThat(env.assertAnnotationClass("foo")).isEqualTo(String.class);
    assertThat(env.assertAnnotationClass("bar")).isEqualTo(Integer.class);

    assertThatExceptionOfType(IllegalStateException.class)
      .isThrownBy(() -> env.assertAnnotationClass("baz"));

    env.setDefaultAnnotationTypeClass(Object.class);
    assertThat(env.assertAnnotationClass("baz")).isEqualTo(Object.class);
  }

  @Test
  public void test_toString() {
    var env = CommonEnvironments.expressionEnvironment();

    assertThat(env.toString()).contains("LoomEnvironment");
  }

  @Test
  public void testCreateGraph() {
    var env = CommonEnvironments.genericEnvironment();
    var graph = env.newGraph();

    assertThat(graph.getEnv()).isNotNull();

    assertThat(graph.getId()).isNotNull();
  }

  @Test
  public void testGraphFromJson() {
    var source =
      """
                {
                  "id": "00000000-0000-4000-8000-00000000000A",
                  "nodes": [
                    {
                      "id": "00000000-0000-0000-0000-000000000000",
                      "type": "%1$s",
                      "label": "foo",
                      "body": {
                        "dtype": "int32",
                        "range": {"start": [0, 0], "end": [2, 3] }
                      }
                    }
                  ]
                 }
                """.formatted(
          TensorOpNodes.TENSOR_NODE_TYPE
        );

    var env = LoomEnvironment
      .builder()
      .build()
      .addNodeTypeClass(TensorOpNodes.TENSOR_NODE_TYPE, TensorNode.class);

    var graph = env.graphFromJson(source);

    env.validateGraph(graph);
    graph.validate();

    var node = (TensorNode) graph.assertNode("00000000-0000-0000-0000-000000000000");
    assertThat(node.getDtype()).isEqualTo("int32");

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> env.graphFromJson("{\"foo\": 2}"))
      .withMessageContaining("Unknown property: foo");
  }

  @Test
  public void test_assertNodeTypeClass() {
    var env = CommonEnvironments.expressionEnvironment();

    env.assertClassForType(TensorOpNodes.TENSOR_NODE_TYPE, TensorNode.class);
    assertThatExceptionOfType(IllegalStateException.class)
      .isThrownBy(() ->
        env.assertClassForType(TensorOpNodes.TENSOR_NODE_TYPE, ApplicationNode.class)
      );

    assertThat(env.supportsNodeType(TensorOpNodes.TENSOR_NODE_TYPE)).isTrue();
    env.assertSupportsNodeType(TensorOpNodes.TENSOR_NODE_TYPE);

    assertThat(env.supportsNodeType("foo")).isFalse();
    assertThatExceptionOfType(IllegalStateException.class)
      .isThrownBy(() -> env.assertSupportsNodeType("foo"));
  }

  @Test
  public void test_constraints() {
    var constraint = new LoomEnvironment.Constraint() {
      @Override
      public void checkRequirements(LoomEnvironment env) {
        env.assertSupportsNodeType(TensorOpNodes.TENSOR_NODE_TYPE);
      }

      @Override
      public void validateConstraint(
        @SuppressWarnings("unused") LoomEnvironment env,
        LoomGraph graph,
        ValidationIssueCollector issueCollector
      ) {
        // pass

      }
    };

    var env = LoomEnvironment.builder().build();

    assertThatExceptionOfType(IllegalStateException.class)
      .isThrownBy(() -> env.addConstraint(constraint));

    env.addNodeTypeClass(TensorOpNodes.TENSOR_NODE_TYPE, TensorNode.class);

    assertThat(env.lookupConstraint(constraint.getClass())).isNull();

    assertThatExceptionOfType(IllegalStateException.class)
      .isThrownBy(() -> env.assertConstraint(constraint.getClass()));

    env.addConstraint(constraint);

    env.assertConstraint(constraint.getClass());
  }
}
