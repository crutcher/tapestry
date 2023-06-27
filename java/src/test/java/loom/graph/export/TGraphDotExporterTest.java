package loom.graph.export;

import loom.common.JsonUtil;
import loom.graph.*;
import loom.testing.CommonAssertions;
import loom.zspace.ZPoint;
import loom.zspace.ZRange;
import org.junit.Test;

import java.util.Map;

public class TGraphDotExporterTest implements CommonAssertions {
    @Test
    public void testExampleGraph() {
        final String float32 = "float32";

        var graph = new TGraph();

        var l0 = graph.addNode(new TBlockOperator("load"));
        l0.createBarrier();
        l0.bindParameters(Map.of("source", "#ref0"));
        var a0 = l0.bindResult("result", new ZPoint(50, 20), float32);

        var l1 = graph.addNode(new TBlockOperator("load"));
        l1.createBarrier();
        l1.bindParameters(Map.of("source", "#ref1"));
        var a1 = l1.bindResult("result", new ZPoint(50, 20), float32);

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

        var concat = graph.addNode(new TFusionOperator("concat"));
        concat.bindParameters(Map.of("dim", "0"));
        concat.bindInputs(Map.of("0", a0, "1", a1));
        var a = concat.bindResult("result", new ZPoint(100, 20), float32);

        var split = graph.addNode(new TViewOperator("split"));
        split.bindParameters(Map.of("dim", "1", "size", "10"));
        split.bindInput("input", a);
        var b0 = split.bindResult("0", new ZPoint(100, 10), float32);
        split.bindResult("1", new ZPoint(100, 10), float32);

        var retype = graph.addNode(new TCellOperator("float8"));
        retype.bindInput("input", b0);
        var c = retype.bindResult("result", new ZPoint(100, 10), "float8");

        var store = graph.addNode(new TBlockOperator("store"));
        store.bindIndex(ZRange.fromShape(100));
        var spF = store.createBarrier();
        store.bindParameters(Map.of("target", "#refOut"));
        store.bindInput("input", c);

        var obv = graph.addNode(new TObserver());
        obv.waitOnBarrier(spF);

        // Force reserialization to validate the graph.
        graph = JsonUtil.roundtrip(graph);
        graph.validate();

        var img = TGraphDotExporter.builder()
                .build().toImage(graph);

        assertThat(img).isNotNull();
    }
}
