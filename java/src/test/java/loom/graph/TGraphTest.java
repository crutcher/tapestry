package loom.graph;

import loom.testing.CommonAssertions;
import org.junit.Test;

public class TGraphTest implements CommonAssertions {
    static class SimpleNode extends TNode {
        SimpleNode() {
            super();
        }

        @Override
        public SimpleNode copy() {
            return new SimpleNode();
        }
    }

    static class ExtNode extends SimpleNode {
        ExtNode() {
            super();
        }

        @Override
        public ExtNode copy() {
            return new ExtNode();
        }
    }

    @Test
    public void test_query() {
        var graph = new TGraph();

        var simple = graph.addNode(new SimpleNode());
        var ext = graph.addNode(new ExtNode());

        assertThat(simple.hasGraph()).isTrue();
        assertThat(ext.hasGraph()).isTrue();

        assertThat(graph.queryNodes(ExtNode.class).toSingleton()).isSameAs(ext);
        assertThat(graph.queryNodes(SimpleNode.class).excluding(ExtNode.class).toSingleton())
                .isSameAs(simple);
    }

    @Test
    public void test_validate() {
        var graph = new TGraph();
        var sp = graph.addNode(new TSequencePoint());
        var simple = graph.addNode(new SimpleNode());
        graph.addNode(new TWaitsOn(simple.id, sp.id));

        graph.validate();

        graph.addNode(new TWaitsOn(simple.id, simple.id));
        assertThatExceptionOfType(ClassCastException.class).isThrownBy(graph::validate);
    }
}
