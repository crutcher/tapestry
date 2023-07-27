package loom.alt.objgraph;

import java.util.List;
import java.util.Map;
import loom.testing.CommonAssertions;
import loom.zspace.ZPoint;
import org.junit.Test;

public class OGGraphTest implements CommonAssertions {
  @Test
  public void testJson() {
    OGGraph graph = new OGGraph();

    @SuppressWarnings("unused")
    var tensorA =
        graph.addNode(
            OGNode.builder()
                .type(OGBuiltins.TENSOR)
                .attr(OGBuiltins.SHAPE, new ZPoint(2, 3))
                .attr(OGBuiltins.DTYPE, "int32"));

    @SuppressWarnings("unused")
    var signature =
        graph.addNode(OGNode.builder().type(OGBuiltins.SIGNATURE).attr(OGBuiltins.EXTERNAL, true));

    @SuppressWarnings("unused")
    var operation =
        graph.addNode(
            OGNode.builder()
                .type(OGBuiltins.OPERATION)
                .attr(OGBuiltins.SIGNATURE, signature.id)
                .attr(OGBuiltins.INPUTS, Map.of("source", List.of(tensorA.id))));

    @SuppressWarnings("unused")
    var p = graph.toPrettyJsonString();

    assertJsonEquals(graph, p);
  }
}
