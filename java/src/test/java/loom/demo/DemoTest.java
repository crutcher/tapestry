package loom.demo;

import loom.doozer.DoozerEnvironment;
import loom.doozer.TypeMapNodeMetaFactory;
import loom.doozer.nodes.TensorNode;
import loom.testing.BaseTestClass;
import loom.zspace.ZPoint;
import org.junit.Test;

public class DemoTest extends BaseTestClass {
  @Test
  public void testDemo() {
    var env =
        DoozerEnvironment.builder()
            .nodeMetaFactory(
                TypeMapNodeMetaFactory.builder()
                    .typeMapping(TensorNode.Meta.TYPE, TensorNode.META)
                    .build())
            .build();

    var graph = env.createGraph();

    var tensorA =
        graph.addNode(
            TensorNode.builder()
                .type(TensorNode.Meta.TYPE)
                .body(TensorNode.Body.builder().dtype("int32").shape(new ZPoint(2, 3)).build()));

    assertThat(tensorA)
        .isInstanceOf(TensorNode.class)
        .hasFieldOrPropertyWithValue("graph", graph)
        .hasFieldOrPropertyWithValue("meta", TensorNode.META);
  }
}
