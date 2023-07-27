package loom.alt.attrgraph;

import java.util.List;
import java.util.Map;
import loom.testing.CommonAssertions;
import loom.zspace.ZPoint;
import org.junit.Test;

public class LoomGraphExporterTest implements CommonAssertions {
  @Test
  public void testExport() {

    var graph = new LoomGraph();

    var a =
        graph.addNode(
            LoomGraph.Node.builder()
                .type(LoomBuiltinNS.TENSOR)
                .attr(LoomBuiltinNS.SHAPE, new ZPoint(10, 3))
                .attr(LoomBuiltinNS.DTYPE, "float32"));

    var b =
        graph.addNode(
            LoomGraph.Node.builder()
                .type(LoomBuiltinNS.TENSOR)
                .attr(LoomBuiltinNS.SHAPE, new ZPoint(10, 2))
                .attr(LoomBuiltinNS.DTYPE, "float32"));

    var c =
        graph.addNode(
            LoomGraph.Node.builder()
                .type(LoomBuiltinNS.TENSOR)
                .attr(LoomBuiltinNS.SHAPE, new ZPoint(10, 5))
                .attr(LoomBuiltinNS.DTYPE, "float32"));

    graph.addNode(
        LoomGraph.Node.builder()
            .type(LoomBuiltinNS.OPERATION)
            .attr(LoomBuiltinNS.OP, LoomBuiltinNS.CONCAT)
            .attr(LoomBuiltinNS.INPUTS, Map.of("inputs", List.of(a.getId(), b.getId())))
            .attr(LoomBuiltinNS.RESULTS, Map.of("out", c.getId())));

    var exporter = LoomGraphExporter.builder().graph(graph).build();

    @SuppressWarnings("unused")
    var dot = exporter.toGraph();

    @SuppressWarnings("unused")
    var img = exporter.toImage();

    assertThat(img).isNotNull();
  }
}
