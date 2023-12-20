package loom.graph;

import loom.graph.nodes.*;
import loom.testing.BaseTestClass;
import loom.validation.ValidationIssueCollector;
import org.junit.Test;

public class LoomEnvironmentTest extends BaseTestClass {
  @Test
  public void test_toString() {
    var env = CommonEnvironments.simpleTensorEnvironment("int32");

    assertThat(env.toString()).contains("LoomEnvironment");
  }

  @Test
  public void testCreateGraph() {
    var env = LoomGraph.GENERIC_ENV;
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

    var env =
        LoomEnvironment.builder()
            .nodeMetaFactory(
                TypeMapNodeMetaFactory.builder()
                    .typeMapping(
                        TensorNode.TYPE, TensorNode.Prototype.builder().validDType("int32").build())
                    .build())
            .build();

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
    var env = CommonEnvironments.simpleTensorEnvironment("int32");

    env.assertNodeTypeClass(TensorNode.TYPE, TensorNode.class);
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> env.assertNodeTypeClass(TensorNode.TYPE, OperationNode.class));
  }

  @Test
  public void test_constraints() {
    var constraint =
        new LoomConstraint() {
          @Override
          public void checkRequirements(LoomEnvironment env) {
            env.assertConstraint(AllTensorsHaveExactlyOneSourceOperationConstraint.class);
          }

          @Override
          public void checkConstraint(
              @SuppressWarnings("unused") LoomEnvironment env,
              LoomGraph graph,
              ValidationIssueCollector issueCollector) {
            // pass

          }
        };

    var env = CommonEnvironments.simpleTensorEnvironment("int32");

    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> env.addConstraint(constraint));

    env.addConstraint(new OperationNodesSourcesAndResultsAreTensors())
        .addConstraint(new AllTensorsHaveExactlyOneSourceOperationConstraint());

    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> env.assertConstraint(constraint.getClass()));

    env.addConstraint(constraint);

    env.assertConstraint(constraint.getClass());
  }
}
