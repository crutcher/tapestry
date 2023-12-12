package loom.zspace;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.infra.Blackhole;

public class ZTensorBenchmark {
  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public void bmClone(Blackhole bh) {
    bh.consume(ZTensor.ones(100, 100).clone());
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public void mul10x10(Blackhole bh) {
    bh.consume(ZTensor.Ops.mul(ZTensor.vector(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), ZTensor.ones(10, 10)));
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public void add10x10(Blackhole bh) {
    bh.consume(ZTensor.Ops.add(ZTensor.vector(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), ZTensor.ones(10, 10)));
  }
}
