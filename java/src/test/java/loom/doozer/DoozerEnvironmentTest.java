package loom.doozer;

import loom.doozer.nodes.TensorNode;
import loom.testing.BaseTestClass;
import org.junit.Test;

public class DoozerEnvironmentTest extends BaseTestClass {
  @Test
  public void testCreateGraph() {
    var env = DoozerGraph.GENERIC_ENV;
    var graph = env.createGraph();

    assertThat(graph).hasFieldOrPropertyWithValue("env", env).isNotNull();

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
                      "type": "TreeNode",
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
        DoozerEnvironment.builder()
            .nodeMetaFactory(
                TypeMapNodeMetaFactory.builder()
                    .typeMapping(TensorNode.Meta.TYPE, new TensorNode.Meta())
                    .build())
            .build();

    var graph = env.graphFromJson(source);

    var node = (TensorNode) graph.assertNode("00000000-0000-0000-0000-000000000000");
    assertThat(node.getDtype()).isEqualTo("int32");
  }
}
