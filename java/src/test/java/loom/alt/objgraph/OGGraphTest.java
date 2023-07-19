package loom.alt.objgraph;

import loom.common.JsonUtil;
import loom.testing.CommonAssertions;
import loom.zspace.ZPoint;
import org.junit.Test;

public class OGGraphTest implements CommonAssertions {
  @Test
  public void testJson() {
    OGGraph graph = new OGGraph();

    @SuppressWarnings("unused")
    OGNode node =
        graph.addNode(
            OGNode.builder()
                .type(OGBuiltins.TENSOR)
                .attr(OGBuiltins.TENSOR_SHAPE, new ZPoint(2, 3).toJsonString())
                .attr(OGBuiltins.DTYPE, "int32"));

    @SuppressWarnings("unused")
    var p = graph.toPrettyJsonString();

    @SuppressWarnings("unused")
    var xml = JsonUtil.toXml(graph);

    assertJsonEquals(graph, p);
  }
}
