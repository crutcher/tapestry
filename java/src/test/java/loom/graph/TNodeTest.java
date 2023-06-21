package loom.graph;

import loom.testing.CommonAssertions;
import org.junit.Test;

public class TNodeTest implements CommonAssertions {
  @Test
  public void test_json() {
    var node = TNode.builder().build();

    assertJsonEquals(node, "{\"@type\":\"node\",\"id\":\"" + node.id + "\"}}");
  }
}
