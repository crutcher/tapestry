package loom.graph;

import loom.common.LookupError;
import loom.testing.CommonAssertions;
import org.junit.Test;

import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;

public class TGraphTest implements CommonAssertions {
    static class SimpleNode extends TNodeBase {
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
    public void testLookupNode() {
        var graph = new TGraph();
        var simple = graph.addNode(new SimpleNode());

        assertThat(graph.lookupNode(simple.id)).isSameAs(simple);

        assertThatExceptionOfType(LookupError.class)
                .isThrownBy(() -> graph.lookupNode(null))
                .withMessage("Lookup failed: null not found in graph");

        assertThat(graph.lookupNode(simple.id, SimpleNode.class)).isSameAs(simple);

        assertThatExceptionOfType(ClassCastException.class)
                .isThrownBy(() -> graph.lookupNode(simple.id, ExtNode.class))
                .withMessageContaining("Cannot cast");
    }

    @Test
    public void test_query_tags() {
        var graph = new TGraph();
        var sp1 = graph.addNode(new TSequencePoint());
        var sp2 = graph.addNode(new TSequencePoint());
        var barrier = graph.addNode(new THappensAfter(sp2.id, sp1.id));

        assertThat(graph.queryTags(THappensAfter.class).toSingleton()).isSameAs(barrier);
    }

    @Test
    public void test_query() {
        var graph = new TGraph();

        var simple = graph.addNode(new SimpleNode());
        var ext = graph.addNode(new ExtNode());

        assertThat(graph.summary()).isEqualTo("TGraph (2 nodes)");

        assertThat(simple.hasGraph()).isTrue();
        assertThat(ext.hasGraph()).isTrue();

        assertThat(graph.queryNodes(ExtNode.class).toSingleton()).isSameAs(ext);

        assertThat(graph.queryNodes(SimpleNode.class).excluding(ExtNode.class).toSingleton())
                .isSameAs(simple);

        assertThat(
                graph
                        .queryNodes(TNodeBase.class)
                        .restrictedTo(SimpleNode.class)
                        .excluding(ExtNode.class)
                        .toSingleton())
                .isSameAs(simple);

        var lst = new ArrayList<UUID>();
        graph
                .queryNodes(TNodeBase.class)
                .restrictedTo(SimpleNode.class)
                .excluding(ExtNode.class)
                .forEach(n -> lst.add(n.id));
        assertThat(lst).containsExactly(simple.id);

        assertThat(
                graph.queryNodes(TNodeBase.class).withFilter(n -> n.id.equals(simple.id)).toSingleton())
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
                        .map(TNodeBase::toJsonString)
                        .collect(Collectors.joining(",", "[", "]"))
                        + "}");
    }

    @Test
    public void test_validate() {
        var graph = new TGraph();
        var sp1 = graph.addNode(new TSequencePoint());
        var sp2 = graph.addNode(new TSequencePoint());
        var barrier = graph.addNode(new THappensAfter(sp2.id, sp1.id));

        assertThat(graph.queryEdges(THappensAfter.class).withTargetId(sp1.id).toSingleton())
                .isSameAs(barrier);

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
