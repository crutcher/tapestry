package loom.zspace;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

public class ZTensorBenchmark {
    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void bmClone(Blackhole bh) {
        bh.consume(ZTensor.ones(100, 100).clone());
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void addition(Blackhole bh) {
        bh.consume(ZTensor.Ops.add(ZTensor.Ops.mul(12, ZTensor.ones(100, 100)), ZTensor.ones(100)));
    }
}
