package loom.graph;

import loom.testing.CommonAssertions;
import loom.zspace.ZPoint;
import org.junit.Test;

public class LoomGraphTest implements CommonAssertions {
  @Test
  public void test_json() {
    var graph = new LoomGraph();

    var node = graph.addNode(TensorNode.builder().dtype("float32").shape(new ZPoint(2, 3)).build());

    assertThat(node.hasGraph()).isTrue();
    assertThat(graph.lookupNode(node.id).assertGraph()).isSameAs(graph);
    assertThat(graph.lookupNode(node.id, TensorNode.class).assertGraph()).isSameAs(graph);

    assertJsonEquals(
        graph,
        "{\"nodes\":[{\"@type\":\"tensor\",\"id\":\""
            + node.id
            + "\",\"dtype\":\"float32\",\"shape\":[2,3]}]}");
  }
}
