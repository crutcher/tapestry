package loom.graph;

import loom.testing.CommonAssertions;

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
}
