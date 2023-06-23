package loom.graph;

import java.util.UUID;
import loom.testing.CommonAssertions;
import org.junit.Test;

public class TEdgeTest implements CommonAssertions {
  public static class ExampleEdge<S extends TNode, T extends TNode> extends TEdge<S, T> {
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
    TEdge<?, ?> tEdge = new ExampleEdge<>(UUID.randomUUID(), UUID.randomUUID());
  }
}
