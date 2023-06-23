package loom.graph;

import loom.testing.CommonAssertions;
import org.junit.Test;

public class TNodeBaseTest implements CommonAssertions {
  static class ExtNode extends TNodeBase {
    ExtNode() {
      super();
    }

    @Override
    public ExtNode copy() {
      return new ExtNode();
    }
  }

  @Test
  public void testGraph() {
    var graph = new TGraph();
    var node = graph.addNode(new ExtNode());

    assertThat(node.hasGraph()).isTrue();
    assertThat(node.assertGraph()).isSameAs(graph);

    node.validate();

    var cp = node.copy();
    assertThat(cp.hasGraph()).isFalse();
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(cp::assertGraph);
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(cp::validate);
  }
}
