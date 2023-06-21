package loom.graph;

import loom.testing.CommonAssertions;
import org.junit.Test;

public class TNodeTest implements CommonAssertions {
  static class ExtNode extends TNode {
    ExtNode() {
      super();
    }

    @Override
    public ExtNode copy() {
      return new ExtNode();
    }
  }

  @Test
  public void test_json() {
    var node = new ExtNode();

    assertJsonEquals(node, "{\"@type\":\"node\",\"id\":\"" + node.id + "\"}}");
  }
}
