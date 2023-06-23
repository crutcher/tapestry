package loom.graph;

import java.util.UUID;
import loom.testing.CommonAssertions;
import org.junit.Test;

public class TTagBaseTest implements CommonAssertions {
  public static class ExampleTag<S extends TNodeBase> extends TTagBase<S> {
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
    TTagBase<?> tTag = new TTagBaseTest.ExampleTag<>(UUID.randomUUID());
  }

  @Test
  public void testSource() {
    TGraph g = new TGraph();
    var sp = g.addNode(new TSequencePoint());
    var tag = g.addNode(new ExampleTag<>(sp.id));

    assertThat(tag.getSource()).isSameAs(sp);
  }
}
