package org.tensortapestry.loom.graph;

import org.junit.Test;
import org.tensortapestry.loom.graph.dialects.tensorops.TensorBody;
import org.tensortapestry.loom.graph.dialects.tensorops.TensorNode;
import org.tensortapestry.loom.testing.BaseTestClass;
import org.tensortapestry.loom.validation.ValidationIssueCollector;

public class LoomEnvironmentTest extends BaseTestClass {

  @Test
  public void test_toString() {
    var env = CommonEnvironments.expressionEnvironment();

    assertThat(env.toString()).contains("LoomEnvironment");
  }

  @Test
  public void testCreateGraph() {
    var env = CommonEnvironments.expressionEnvironment();
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
          TensorNode.TYPE
        );

    var env = CommonEnvironments.expressionEnvironment();

    var graph = env.graphFromJson(source);

    env.validateGraph(graph);
    graph.validate();

    {
      var tensor = graph.assertNode("00000000-0000-0000-0000-000000000000", TensorNode.TYPE);
      assertThat(tensor.viewBodyAs(TensorBody.class).getDtype()).isEqualTo("int32");
    }

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> env.graphFromJson("{\"foo\": 2}"));
  }

  @Test
  public void test_constraints() {
    var constraint = new LoomEnvironment.Constraint() {
      @Override
      public void checkRequirements(LoomEnvironment env) {
        env.assertSupportsNodeType(TensorNode.TYPE);
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

    var env = CommonEnvironments.expressionEnvironment();

    assertThat(env.lookupConstraint(constraint.getClass())).isNull();

    assertThatExceptionOfType(IllegalStateException.class)
      .isThrownBy(() -> env.assertConstraint(constraint.getClass()));

    env.addConstraint(constraint);

    env.assertConstraint(constraint.getClass());
  }
}
