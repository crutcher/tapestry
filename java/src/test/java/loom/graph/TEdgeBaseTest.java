package loom.graph;

import java.util.UUID;
import loom.testing.CommonAssertions;
import org.junit.Test;

public class TEdgeBaseTest implements CommonAssertions {
  public static class ExampleEdge<S extends TNodeBase, T extends TNodeBase>
      extends TEdgeBase<S, T> {
    public ExampleEdge(UUID sourceId, UUID targetId) {
      super(sourceId, targetId);
    }

    @Override
    public ExampleEdge<S, T> copy() {
      return new ExampleEdge<>(this.sourceId, this.targetId);
    }
  }

  @Test
  public void testConstructor() {
    @SuppressWarnings("unused")
    TEdgeBase<?, ?> tEdge = new ExampleEdge<>(UUID.randomUUID(), UUID.randomUUID());
  }
}
