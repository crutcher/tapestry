package loom.graph.export;

import java.util.Map;
import loom.graph.*;
import loom.testing.CommonAssertions;
import loom.zspace.ZPoint;
import loom.zspace.ZRange;
import org.junit.Test;

public class TGraphDotExporterTest implements CommonAssertions {
  @Test
  public void testExampleGraph() {
    final String float32 = "float32";

    var graph = new TGraph();

    var l0 = graph.addNode(new TBlockOperator("load"));
    var sp0 = graph.addNode(new TSequencePoint());
    sp0.waitOnBarrier(l0);
    graph.addNode(
        new TWithEdge(l0.id, graph.addNode(new TParameters(Map.of("source", "#ref0"))).id));
    var a0 = l0.bindResult(new ZPoint(50, 20), float32, "result");

    var l1 = graph.addNode(new TBlockOperator("load"));
    var sp1 = graph.addNode(new TSequencePoint());
    sp1.waitOnBarrier(l1);
    graph.addNode(
        new TWithEdge(l1.id, graph.addNode(new TParameters(Map.of("source", "#ref1"))).id));
    var a1 = l1.bindResult(new ZPoint(50, 20), float32, "result");

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

    var concat = graph.addNode(new TSelectorOperator("concat"));
    graph.addNode(new TWithEdge(concat.id, graph.addNode(new TParameters(Map.of("dim", "0"))).id));
    concat.bindInput(a0, "0");
    concat.bindInput(a1, "0");
    var a = concat.bindResult(new ZPoint(100, 20), float32, "result");
    graph.addNode(new TLabelTag(a.id, "A"));

    var split = graph.addNode(new TSelectorOperator("split"));
    graph.addNode(
        new TWithEdge(
            split.id, graph.addNode(new TParameters(Map.of("dim", "1", "size", "10"))).id));
    split.bindInput(a, "input");

    var b0 = graph.addNode(new TTensor(new ZPoint(100, 10), float32));
    graph.addNode(new TResultEdge(b0.id, split.id, "0"));
    graph.addNode(new TLabelTag(b0.id, "B"));

    var b1 = graph.addNode(new TTensor(new ZPoint(100, 10), float32));
    graph.addNode(new TResultEdge(b1.id, split.id, "1"));

    var store = graph.addNode(new TBlockOperator("store"));
    var spF = graph.addNode(new TSequencePoint());
    spF.waitOnBarrier(store);
    graph.addNode(
        new TWithEdge(store.id, graph.addNode(new TParameters(Map.of("target", "#refOut"))).id));
    graph.addNode(new TIndexTag(store.id, ZRange.fromShape(100)));
    store.bindInput(b0, "input");

    var obv = graph.addNode(new TObserver());
    obv.waitOnBarrier(spF);

    var img = TGraphDotExporter.builder().build().toImage(graph);

    assertThat(img).isNotNull();
  }
}
