package loom.graph;

import loom.testing.CommonAssertions;
import loom.zspace.ZPoint;
import org.junit.Test;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TGraphTest implements CommonAssertions {

    @Test
    public void test_json() {
        var graph = new TGraph();

        var tensor = graph.addNode(TTensor.builder().dtype("float32").shape(new ZPoint(2, 3)).build());

        var tag = graph.addNode(TTag.builder().sourceId(tensor.id).build());

        assertThat(tensor.hasGraph()).isTrue();

        assertThat(graph.queryNodes(TTensor.class).toSingleton()).isSameAs(tensor);

        assertJsonEquals(
                graph,
                "{\"nodes\":"
                        + Stream.of(tensor, tag)
                        .map(TNode::toJsonString)
                        .collect(Collectors.joining(", ", "[", "]"))
                        + "}");
    }
}
