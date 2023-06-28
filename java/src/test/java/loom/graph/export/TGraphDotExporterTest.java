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

public class TGraphDotExporterTest implements CommonAssertions {
    public static TTensor load(TGraph graph, String ref, ZPoint shape, String dtype) {
        var lop = graph.addNode(new TBlockOperator("load"));
        lop.bindParameters(Map.of("source", ref));
        return lop.bindResult("result", shape, dtype);
    }

    public static TTensor loadShards(
            TGraph graph, int dim, String dtype, List<Pair<String, ZPoint>> shards) {
        int[] fusedShape = null;
        for (var shard : shards) {
            ZPoint shape = shard.getSecond();
            if (fusedShape == null) {
                fusedShape = shape.toArray();
            } else {
                for (int i = 0; i < fusedShape.length; i++) {
                    if (i == dim) {
                        fusedShape[i] += shape.get(i);
                    } else if (fusedShape[i] != shape.get(i)) {
                        throw new IllegalArgumentException(
                                "Incompatible shapes: " + new ZPoint(fusedShape) + " vs " + shape);
                    }
                }
            }
        }
        if (fusedShape == null) {
            throw new IllegalArgumentException("No shards provided");
        }
        var concatOp = graph.addNode(new TFusionOperator("concat"));

        for (int i = 0; i < shards.size(); ++i) {
            var shard = shards.get(i);
            String ref = shard.getFirst();
            ZPoint shape = shard.getSecond();

            var lop = graph.addNode(new TBlockOperator("load"));
            lop.bindParameters(Map.of("source", ref));
            var tensor = lop.bindResult("result", shape, dtype);

            concatOp.bindInput(String.valueOf(i), tensor);
        }

        return concatOp.bindResult("result", new ZPoint(fusedShape), dtype);
    }

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
                loadShards(
                        graph,
                        0,
                        "float32",
                        List.of(
                                Pair.create("#ref0", new ZPoint(50, 20)),
                                Pair.create("#ref1", new ZPoint(50, 20))));

        var split = graph.addNode(new TViewOperator("split"));
        split.bindParameters(Map.of("dim", "1", "size", "10"));
        split.bindInput("input", a);
        var b0 = split.bindResult("0", new ZPoint(100, 10), float32);
        split.bindResult("1", new ZPoint(100, 10), float32);

        var retype = graph.addNode(new TCellOperator("float8"));
        retype.bindInput("input", b0);
        var c = retype.bindResult("result", new ZPoint(100, 10), float8);

        var w = load(graph, "#refW", new ZPoint(5, 10), float8);

        var dense = graph.addNode(new TMacroOperator("dense"));
        dense.bindInputs(Map.of("input", c, "weight", w));
        var y = dense.bindResult("result", new ZPoint(100, 5), float8);

        var store = graph.addNode(new TBlockOperator("store"));
        store.bindIndex(ZRange.fromShape(100));
        store.bindParameters(Map.of("target", "#refOut"));
        store.bindInput("input", y);

        var obv = graph.addNode(new TObserver());
        obv.waitOnBarrier(store);

        // Force reserialization to validate the graph.
        graph = JsonUtil.roundtrip(graph);
        graph.validate();

        var img = TGraphDotExporter.builder().build().toImage(graph);

        assertThat(img).isNotNull();
    }
}
