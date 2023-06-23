package loom.graph;

import java.util.UUID;
import loom.testing.CommonAssertions;
import org.junit.Test;

public class TTagTest implements CommonAssertions {
  public static class ExampleTag<S extends TNode> extends TTag<S> {
    public ExampleTag(UUID sourceId) {
      super(sourceId);
    }

    @Override
    public ExampleTag<S> copy() {
      return new ExampleTag<>(this.sourceId);
    }
  }

  @Test
  public void testConstructor() {
    @SuppressWarnings("unused")
    TTag<?> tTag = new TTagTest.ExampleTag<>(UUID.randomUUID());
  }

  @Test
  public void testSource() {
    TGraph g = new TGraph();
    var sp = g.addNode(new TSequencePoint());
    var tag = g.addNode(new ExampleTag<>(sp.id));

    assertThat(tag.getSource()).isSameAs(sp);
  }
}
