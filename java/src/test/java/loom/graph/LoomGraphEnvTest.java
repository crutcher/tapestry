package loom.graph;

import static loom.graph.nodes.OperationNodeTypeBindings.OPERATION_TYPE;
import static loom.graph.nodes.TensorNodeTypeBindings.TENSOR_TYPE;
import static org.junit.Assert.assertNotNull;

import loom.common.LookupError;
import loom.common.serialization.JsonUtil;
import loom.graph.nodes.OperationNodeTypeBindings;
import loom.graph.nodes.TensorNodeTypeBindings;
import loom.testing.BaseTestClass;
import org.junit.Test;

public class LoomGraphEnvTest extends BaseTestClass {
  @Test
  public void testCreateDefault() {
    var env = CommonLoomGraphEnvironments.createDefault();
    assertNotNull(env);

    assertThat(env.hasNodeTypeOps(TENSOR_TYPE)).isTrue();
    assertThat(env.hasNodeTypeOps(OPERATION_TYPE)).isTrue();

    var tensorOps = (TensorNodeTypeBindings) env.getNodeTypeOps(TENSOR_TYPE);
    assertThat(tensorOps.getDatatypes()).containsExactly("int32");
  }

  @Test
  public void testHasNodeTypeOps() {
    var env = new LoomGraphEnv();

    assertThat(env.hasNodeTypeOps(OperationNodeTypeBindings.OPERATION_TYPE)).isFalse();
    assertThat(env.getNodeTypeOps(OperationNodeTypeBindings.OPERATION_TYPE)).isNull();
    assertThatExceptionOfType(LookupError.class)
        .isThrownBy(() -> env.assertNodeTypeOps(OperationNodeTypeBindings.OPERATION_TYPE));
    assertThatExceptionOfType(LookupError.class)
        .isThrownBy(
            () ->
                env.assertNodeTypeOps(
                    OperationNodeTypeBindings.OPERATION_TYPE, OperationNodeTypeBindings.class));

    var operationOps = env.addNodeTypeBindings(new OperationNodeTypeBindings());

    assertThat(env.hasNodeTypeOps(OperationNodeTypeBindings.OPERATION_TYPE)).isTrue();
    assertThat(env.getNodeTypeOps(OperationNodeTypeBindings.OPERATION_TYPE)).isSameAs(operationOps);
    assertThat(env.assertNodeTypeOps(OperationNodeTypeBindings.OPERATION_TYPE))
        .isSameAs(operationOps);
    assertThat(
            env.assertNodeTypeOps(
                OperationNodeTypeBindings.OPERATION_TYPE, OperationNodeTypeBindings.class))
        .isSameAs(operationOps);
  }

  @Test
  public void testWrap() {
    var doc = new LoomDoc();
    var idA = doc.addNode(LoomDoc.NodeDoc.builder().type("foo"));
    var idB = doc.addNode(LoomDoc.NodeDoc.builder().type("bar"));

    var env = CommonLoomGraphEnvironments.createDefault();
    var graph = env.wrap(doc);

    assertThat(graph.getDoc()).isSameAs(doc);

    assertThat(graph.assertNode(idA).getDoc()).isSameAs(doc.assertNode(idA));
    assertThat(graph.assertNode(idB).getDoc()).isSameAs(doc.assertNode(idB));
  }

  @Test
  public void testParse() {
    var nodeId = "00000000-0000-4000-8000-000000000001";

    String source =
        """
                    {
                      "nodes": [
                         {
                           "id": "%s",
                           "type": "test",
                           "label": "Foo",
                           "fields": {
                             "b": [2, 3],
                             "a": 1
                           }
                         }
                      ]
                    }
                    """
            .formatted(nodeId);

    var env = CommonLoomGraphEnvironments.createDefault();
    assertJsonEquals(env.wrap(JsonUtil.fromJson(source, LoomDoc.class)).getDoc(), source);
    assertJsonEquals(env.parse(source).getDoc(), source);
    assertJsonEquals(env.parse(JsonUtil.readTree(source)).getDoc(), source);
  }
}
