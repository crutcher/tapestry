package org.tensortapestry.loom.graph;

import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.CommonAssertions;
import org.tensortapestry.common.validation.ValidationIssueCollector;
import org.tensortapestry.loom.graph.dialects.tensorops.TensorNode;

public class LoomEnvironmentTest implements CommonAssertions {

  @Test
  public void test_toString() {
    var env = CommonEnvironments.APPLICATION_EXPRESSION_ENVIRONMENT;

    assertThat(env.toString()).contains("LoomEnvironment");
  }

  @Test
  public void testCreateGraph() {
    var env = CommonEnvironments.APPLICATION_EXPRESSION_ENVIRONMENT;
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

    var env = CommonEnvironments.APPLICATION_EXPRESSION_ENVIRONMENT;

    var graph = env.graphFromJson(source);

    env.validateGraph(graph);
    graph.validate();

    {
      var tensor = graph.assertNode("00000000-0000-0000-0000-000000000000", TensorNode.TYPE);
      assertThat(tensor.viewBodyAs(TensorNode.Body.class).getDtype()).isEqualTo("int32");
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

    var env = CommonEnvironments.APPLICATION_EXPRESSION_ENVIRONMENT;

    assertThat(env.lookupConstraint(constraint.getClass())).isNull();

    assertThatExceptionOfType(IllegalStateException.class)
      .isThrownBy(() -> env.assertConstraint(constraint.getClass()));

    var extEnv = env.toBuilder().constraint(constraint).build();

    extEnv.assertConstraint(constraint.getClass());
  }
}
