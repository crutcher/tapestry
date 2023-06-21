package loom.graph;

import loom.testing.CommonAssertions;
import org.junit.Test;

public class LoomNodeTest implements CommonAssertions {
  @Test
  public void test_json() {
    var node = LoomNode.builder().build();

    assertJsonEquals(node, "{\"@type\":\"node\",\"id\":\"" + node.id + "\"}}");
  }
}
