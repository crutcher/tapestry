package loom.zspace;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.infra.Blackhole;

public class ZTensorBenchmark {

  public static final ZTensor ONES_100_100_100 = ZTensor.newOnes(100, 100, 100);
  public static final ZTensor VECTOR_10 = ZTensor.newVector(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
  public static final ZTensor ONES_10_10 = ZTensor.newOnes(10, 10);
  public static final ZTensor ONES_100_100 = ZTensor.newOnes(100, 100);

  @Benchmark
  @BenchmarkMode(Mode.SampleTime)
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  public void clone100x100(Blackhole bh) {
    bh.consume(ONES_100_100.clone());
  }

  @Benchmark
  @BenchmarkMode(Mode.SampleTime)
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  public void mul10x10(Blackhole bh) {
    bh.consume(ZTensor.Ops.mul(VECTOR_10, ONES_10_10));
  }

  @Benchmark
  @BenchmarkMode(Mode.SampleTime)
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  public void add10x10(Blackhole bh) {
    bh.consume(ZTensor.Ops.add(VECTOR_10, ONES_10_10));
  }

  @Benchmark
  @BenchmarkMode(Mode.SampleTime)
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  public void sum100x100x100(Blackhole bh) {
    bh.consume(ONES_100_100_100.sum(1));
  }
}
