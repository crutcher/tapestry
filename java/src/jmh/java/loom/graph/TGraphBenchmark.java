package loom.graph;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import loom.zspace.ZPoint;
import loom.zspace.ZRange;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.infra.Blackhole;

public class TGraphBenchmark {
  @Benchmark
  @BenchmarkMode(Mode.SampleTime)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public void buildAndCopy(Blackhole bh) {
    var graph = new TGraph();

    var l0 = graph.addNode(new TBlockOperator("load"));
    var sp0 = graph.addNode(new TSequencePoint());
    graph.addNode(new TCanBeSequencedProperty.TWaitsOnEdge(sp0.id, l0.id));
    graph.addNode(
        new TParameters.TWithParametersEdge(
            l0.id, graph.addNode(new TParameters(Map.of("source", "#ref0"))).id));
    var a0 = graph.addNode(new TTensor(new ZPoint(50, 20), "float32"));
    graph.addNode(new TTensor.TResultEdge(a0.id, l0.id, "result"));

    var l1 = graph.addNode(new TBlockOperator("load"));
    var sp1 = graph.addNode(new TSequencePoint());
    graph.addNode(new TCanBeSequencedProperty.TWaitsOnEdge(sp1.id, l1.id));
    graph.addNode(
        new TParameters.TWithParametersEdge(
            l1.id, graph.addNode(new TParameters(Map.of("source", "#ref1"))).id));
    var a1 = graph.addNode(new TTensor(new ZPoint(50, 20), "float32"));
    graph.addNode(new TTensor.TResultEdge(a1.id, l1.id, "result"));

    var concat = graph.addNode(new TSelectorOperator("concat"));
    graph.addNode(
        new TParameters.TWithParametersEdge(
            concat.id, graph.addNode(new TParameters(Map.of("dim", "0"))).id));
    graph.addNode(new TTensor.TConsumesEdge(concat.id, a0.id, "0"));
    graph.addNode(new TTensor.TConsumesEdge(concat.id, a1.id, "1"));

    var a = graph.addNode(new TTensor(new ZPoint(100, 20), "float32"));
    graph.addNode(new TTensor.TResultEdge(a.id, concat.id, "result"));
    graph.addNode(new TLabelTag(a.id, "A"));

    var split = graph.addNode(new TSelectorOperator("split"));
    graph.addNode(
        new TParameters.TWithParametersEdge(
            split.id, graph.addNode(new TParameters(Map.of("dim", "1", "size", "10"))).id));
    graph.addNode(new TTensor.TConsumesEdge(split.id, a.id, "input"));

    var b0 = graph.addNode(new TTensor(new ZPoint(100, 10), "float32"));
    graph.addNode(new TTensor.TResultEdge(b0.id, split.id, "0"));
    graph.addNode(new TLabelTag(b0.id, "B"));

    var b1 = graph.addNode(new TTensor(new ZPoint(100, 10), "float32"));
    graph.addNode(new TTensor.TResultEdge(b1.id, split.id, "1"));

    var store = graph.addNode(new TBlockOperator("store"));
    var spF = graph.addNode(new TSequencePoint());
    graph.addNode(new TCanBeSequencedProperty.TWaitsOnEdge(spF.id, store.id));
    graph.addNode(
        new TParameters.TWithParametersEdge(
            store.id, graph.addNode(new TParameters(Map.of("target", "#refOut"))).id));
    graph.addNode(new TIndexTag(store.id, ZRange.fromShape(100)));
    graph.addNode(new TTensor.TConsumesEdge(store.id, b0.id, "input"));

    var obv = graph.addNode(new TObserver());
    graph.addNode(new TCanBeSequencedProperty.TWaitsOnEdge(obv.id, spF.id));

    var cp = graph.copy();
    bh.consume(cp);
  }
}
