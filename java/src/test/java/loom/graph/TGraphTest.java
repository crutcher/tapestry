package loom.graph;

import loom.testing.CommonAssertions;
import org.junit.Test;

import java.util.stream.Collectors;

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
    public void test_json() {
        var graph = new TGraph();
        var sp1 = graph.addNode(new TSequencePoint());
        var sp2 = graph.addNode(new TSequencePoint());
        graph.addNode(new THappensAfter(sp2.id, sp1.id));

        assertJsonEquals(
                graph,
                "{\"nodes\":"
                        + graph.getNodeMap().values().stream()
                        .map(TNode::toJsonString)
                        .collect(Collectors.joining(",", "[", "]"))
                        + "}");
    }

    @Test
    public void test_validate() {
        var graph = new TGraph();
        var sp1 = graph.addNode(new TSequencePoint());
        var sp2 = graph.addNode(new TSequencePoint());
        graph.addNode(new THappensAfter(sp2.id, sp1.id));

        assertThat(sp2.barrierIds()).containsOnly(sp1.id);
        assertThat(sp2.barriers()).containsOnly(sp1);

        graph.validate();

        var gc = graph.copy();
        gc.validate();

        var simple = graph.addNode(new SimpleNode());
        graph.addNode(new THappensAfter(simple.id, simple.id));
        assertThatExceptionOfType(ClassCastException.class).isThrownBy(graph::validate);
    }
}
