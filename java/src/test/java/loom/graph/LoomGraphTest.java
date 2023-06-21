package loom.graph;

import loom.testing.CommonAssertions;
import loom.zspace.ZPoint;
import org.junit.Test;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LoomGraphTest implements CommonAssertions {
    @Test
    public void test_json() {
        var graph = new LoomGraph();

        var node = graph.addNode(LoomTensor.builder().dtype("float32").shape(new ZPoint(2, 3)).build());

        var tag = graph.addNode(LoomTag.builder().sourceId(node.id).build());

        assertThat(node.hasGraph()).isTrue();
        assertThat(graph.lookupNode(node.id).assertGraph()).isSameAs(graph);
        assertThat(graph.lookupNode(node.id, LoomTensor.class).assertGraph()).isSameAs(graph);

        assertThat(graph.listNodes(LoomTensor.class)).contains(node);

        assertJsonEquals(
                graph,
                "{\"nodes\":"
                        + Stream.of(node, tag)
                        .map(LoomNode::toJsonString)
                        .collect(Collectors.joining(", ", "[", "]"))
                        + "}");
    }
}
