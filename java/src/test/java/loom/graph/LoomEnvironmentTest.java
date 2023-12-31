package loom.graph;

import loom.graph.constraints.NodeBodySchemaConstraint;
import loom.graph.nodes.ApplicationNode;
import loom.graph.nodes.NoteNode;
import loom.graph.nodes.TensorNode;
import loom.testing.BaseTestClass;
import loom.validation.ValidationIssueCollector;
import org.junit.Test;

public class LoomEnvironmentTest extends BaseTestClass {
  @Test
  public void test_builder() {
    var env =
        LoomEnvironment.builder()
            .build()
            .addNodeTypeClass(TensorNode.TYPE, TensorNode.class)
            .addNodeTypeClass(NoteNode.TYPE, NoteNode.class);

    assertThat(env.supportsNodeType(TensorNode.TYPE)).isTrue();
    assertThat(env.supportsNodeType(NoteNode.TYPE)).isTrue();

    assertThat(env.classForType(TensorNode.TYPE)).isEqualTo(TensorNode.class);
    assertThat(env.classForType(NoteNode.TYPE)).isEqualTo(NoteNode.class);

    assertThat(env.supportsNodeType(ApplicationNode.TYPE)).isFalse();
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> env.assertClassForType(ApplicationNode.TYPE));
  }

  @Test
  public void test_toString() {
    var env = CommonEnvironments.expressionEnvironment();

    assertThat(env.toString()).contains("LoomEnvironment");
  }

  @Test
  public void testCreateGraph() {
    var env = CommonEnvironments.genericEnvironment();
    var graph = env.createGraph();

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
                      "type": "TensorNode",
                      "label": "foo",
                      "body": {
                        "dtype": "int32",
                        "shape": [2, 3]
                      }
                    }
                  ]
                 }
                """;

    var env = LoomEnvironment.builder().build().addNodeTypeClass(TensorNode.TYPE, TensorNode.class);

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

    env.assertClassForType(TensorNode.TYPE, TensorNode.class);
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> env.assertClassForType(TensorNode.TYPE, ApplicationNode.class));

    assertThat(env.supportsNodeType(TensorNode.TYPE)).isTrue();
    env.assertSupportsNodeType(TensorNode.TYPE);

    assertThat(env.supportsNodeType("foo")).isFalse();
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> env.assertSupportsNodeType("foo"));
  }

  @Test
  public void test_constraints() {
    var constraint =
        new LoomEnvironment.Constraint() {
          @Override
          public void checkRequirements(LoomEnvironment env) {
            env.assertSupportsNodeType(TensorNode.TYPE);
          }

          @Override
          public void validateConstraint(
              @SuppressWarnings("unused") LoomEnvironment env,
              LoomGraph graph,
              ValidationIssueCollector issueCollector) {
            // pass

          }
        };

    var env = LoomEnvironment.builder().build();

    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> env.addConstraint(constraint));

    env.addNodeTypeClass(TensorNode.TYPE, TensorNode.class);

    assertThat(env.lookupConstraint(constraint.getClass())).isNull();

    env.addConstraint(constraint);

    assertThat(env.lookupConstraint(NodeBodySchemaConstraint.class)).isNull();

    env.assertConstraint(constraint.getClass());
  }
}
