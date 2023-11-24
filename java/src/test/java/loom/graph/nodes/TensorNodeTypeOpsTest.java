package loom.graph.nodes;

import java.util.List;
import loom.graph.LoomDoc;
import loom.graph.LoomGraphEnv;
import loom.testing.BaseTestClass;
import org.junit.Test;

public class TensorNodeTypeOpsTest extends BaseTestClass {
  /**
   * Build a graph environment with the tensor node type registered.
   *
   * @return the graph environment.
   */
  public LoomGraphEnv buildEnv() {
    var env = new LoomGraphEnv();

    var tensorOps = env.registerNodeTypeOps(new TensorNodeTypeOps());
    tensorOps.addDatatype("int32");

    return env;
  }

  @Test
  public void testParseFields() {
    var env = buildEnv();

    var tensorA = "00000000-0000-4000-8000-000000000001";

    String source =
        """
                        {
                          "nodes": [
                             {
                               "id": "%1$s",
                               "type": "%2$s",
                               "fields": {
                                 "shape": [2, 3, 1],
                                 "dtype": "int32"
                               }
                             }
                          ]
                        }
                        """
            .formatted(tensorA, TensorNodeTypeOps.TENSOR_TYPE);

    var graph = env.parse(source);
    graph.validate();

    var node = graph.assertNode(tensorA);

    assertThat(TensorNodeTypeOps.parseFields(node))
        .isInstanceOf(TensorNodeTypeOps.TensorFields.class)
        .hasFieldOrPropertyWithValue("shape", new int[] {2, 3, 1})
        .hasFieldOrPropertyWithValue("dtype", "int32");
  }

  @Test
  public void testBuilder() {
    var env = buildEnv();
    var graph = env.createGraph();

    var id =
        graph
            .getDoc()
            .addNode(
                LoomDoc.NodeDoc.builder()
                    .type(TensorNodeTypeOps.TENSOR_TYPE)
                    .asFields(
                        TensorNodeTypeOps.TensorFields.builder()
                            .shape(new int[] {2, 3, 1})
                            .dtype("int32")
                            .build()));

    var node = graph.assertNode(id);
    assertThat(node.getFieldAsObject("shape")).isEqualTo(List.of(2, 3, 1));
    assertThat(node.getFieldAsObject("dtype")).isEqualTo("int32");
  }
}
