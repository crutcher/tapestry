package loom.graph.export;

import loom.common.JsonUtil;
import loom.graph.*;
import loom.testing.CommonAssertions;
import loom.zspace.ZPoint;
import loom.zspace.ZRange;
import org.apache.commons.math3.util.Pair;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class TGraphDotExporterTest implements CommonAssertions, TGraphOperations {

  @Test
  public void testExampleGraph() {
    // Ideas:
    // {
    //     var concatResults = TSelectorOperator.opBuilder()
    //             .op("concat")
    //             .graph(graph)
    //             .input(Map.of("0", a0, "1", a1))
    //             .params(Map.of("dim", "0"))
    //             .build()
    //             .yields();
    //     var a = concatResults.outputs.get("result");
    // }
    // {
    //     var a = graph.concat(0).inputs(a0, a1).yields().item();
    // }
    // {
    //     var a = LoomOps.concat(graph).dim(0).inputs(a0, a1).yields().item();
    //     var a = graph.concat(0).inputs(a0, a1).yields().item();
    // }

    final String float32 = "float32";
    final String float8 = "float8";

    var graph = new TGraph();

    var a =
        loadTensorFromShards(
            graph,
            0,
            "float32",
            List.of(
                Pair.create("#ref0", new ZPoint(50, 20)),
                Pair.create("#ref1", new ZPoint(50, 20))));

    var split = graph.addNode(new TViewOperator("split"));
    split.bindParameters(Map.of("dim", "1", "size", "10"));
    split.bindInput("input", a);
    var b0 = split.bindResult("chunk/0", new ZPoint(100, 10), float32);
    split.bindResult("chunk/1", new ZPoint(100, 10), float32);

    var retype = graph.addNode(new TCellOperator("convert"));
    retype.bindParameters(Map.of("dtype", float8));
    retype.bindInput("input", b0);
    var c = retype.bindResult("result", new ZPoint(100, 10), float8);

    var w = loadTensor(graph, "#refW", new ZPoint(5, 10), float8);

    var dense = graph.addNode(new TMacroOperator("dense"));
    dense.bindInputs(Map.of("input", c, "weight", w));
    var y = dense.bindResult("result", new ZPoint(100, 5), float8);

    var store = graph.addNode(new TBlockOperator("store"));
    store.markAsIO();
    store.bindIndex(ZRange.fromShape(100));
    store.bindParameters(Map.of("target", "#refOut"));
    store.bindInput("input", y);

    var obv = graph.addNode(new TObserver());
    obv.waitOnBarrier(store);

    // Force reserialization to validate the graph.
    graph = JsonUtil.roundtrip(graph);
    graph.validate();

    @SuppressWarnings("unused")
    var json = JsonUtil.toPrettyJson(graph);

    var exporter = TGraphDotExporter.builder().build();

    @SuppressWarnings("unused")
    var dot = exporter.toGraph(graph).toString();

    var img = exporter.toImage(graph);

    assertThat(img).isNotNull();
  }
}
