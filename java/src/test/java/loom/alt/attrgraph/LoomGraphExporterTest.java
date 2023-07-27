package loom.alt.attrgraph;

import loom.testing.CommonAssertions;
import loom.zspace.ZPoint;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class LoomGraphExporterTest implements CommonAssertions {
  @Test
  public void testExport() {

    var env = new LoomEnvironment();
    env.addSchema(
        LoomSchema.builder()
            .urn(LoomBuiltinNS.BUILTINS_URN)
            .attribute(
                LoomBuiltinNS.RESULTS.name(),
                LoomSchema.Attribute.builder()
                    .name(LoomBuiltinNS.RESULTS.name())
                    .invertEdge(true)
                    .build())
            .build());

    env.aliasMap.put("tap", LoomBuiltinNS.BUILTINS_URN);

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

    @SuppressWarnings("unused")
    var json = graph.toString();

    var exporter = LoomGraphExporter.builder().graph(graph).environment(env).build();

    @SuppressWarnings("unused")
    var dot = exporter.toGraph();

    @SuppressWarnings("unused")
    var img = exporter.toImage();

    assertThat(img).isNotNull();
  }
}
