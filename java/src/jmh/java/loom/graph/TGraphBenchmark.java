package loom.graph;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import loom.alt.linkgraph.graph.*;
import loom.zspace.ZPoint;
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
    final String float32 = "float32";

    var graph = new TGraph();

    var l0 = graph.addNode(new TBlockOperator("load"));
    var sp0 = graph.addNode(new TSequencePoint());
    sp0.waitOnBarrier(l0);
    l0.bindParameters(Map.of("source", "#ref0"));
    var a0 = l0.bindResult("result", new ZPoint(50, 20), float32);

    var l1 = graph.addNode(new TBlockOperator("load"));
    var sp1 = graph.addNode(new TSequencePoint());
    sp1.waitOnBarrier(l1);
    l1.bindParameters(Map.of("source", "#ref1"));
    var a1 = l1.bindResult("result", new ZPoint(50, 20), float32);

    var concat = graph.addNode(new TFusionOperator("concat"));
    concat.bindParameters(Map.of("dim", "0"));
    concat.bindInput("0", a0);
    concat.bindInput("1", a1);
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
    var spF = graph.addNode(new TSequencePoint());
    spF.waitOnBarrier(store);
    store.bindParameters(Map.of("target", "#refOut"));
    store.bindInput("input", c);

    var obv = graph.addNode(new TObserver());
    obv.waitOnBarrier(spF);

    var cp = graph.copy();
    graph.validate();

    bh.consume(cp);
  }
}
