package loom.zspace;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.infra.Blackhole;

public class ZRangeBenchmark {
  @Benchmark
  @BenchmarkMode(Mode.SampleTime)
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  public void intersection(Blackhole bh) {
    var a = ZRange.fromShape(100, 100, 100);
    var b = ZRange.fromShape(50, 50, 50).translate(new ZPoint(75, 75, 75));
    bh.consume(a.intersection(b));
  }
}
