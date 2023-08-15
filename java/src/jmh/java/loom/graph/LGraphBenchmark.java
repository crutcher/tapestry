package loom.graph;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.infra.Blackhole;

public class LGraphBenchmark {
  @Benchmark
  @BenchmarkMode(Mode.SampleTime)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public void buildAndCopy(Blackhole bh) {
    var doc =
        XGraphUtils.parse(
            """
                <?xml version="1.0" encoding="UTF-8"?>
                <eg:graph
                  xmlns:eg="http://loom-project.org/schemas/v0.1/ExpressionGraph.core.xsd"
                  xmlns:ext="http://loom-project.org/schemas/v0.1/ExpressionGraph.ext.xsd"
                  >
                  <eg:nodes>
                  <eg:tensor id="node-00000000-0000-0000-0000-0000000000E1">
                    <eg:dtype>float32</eg:dtype>
                    <eg:shape>
                      <eg:dim size="30"/>
                      <eg:dim size="2"/>
                      </eg:shape>
                    </eg:tensor>
                  <eg:tensor id="node-00000000-0000-0000-0000-0000000000E2">
                    <eg:dtype>float32</eg:dtype>
                    <eg:shape>
                      <eg:dim size="30"/>
                      <eg:dim size="2"/>
                      </eg:shape>
                    </eg:tensor>
                  <eg:trace
                      id="node-00000000-0000-0000-0000-0000000000A1"
                      ref="node-00000000-0000-0000-0000-0000000000E0">
                    <eg:text>foo</eg:text>
                    </eg:trace>
                  <eg:operation id="node-00000000-0000-0000-0000-0000000000E0"
                      op="loom:concat">
                    <eg:inputs>
                      <eg:item name="source">
                        <eg:ref target="node-00000000-0000-0000-0000-0000000000E1" />
                        <eg:ref target="node-00000000-0000-0000-0000-0000000000E2" />
                        </eg:item>
                      </eg:inputs>
                    <eg:outputs>
                      <eg:item name="out">
                        <eg:ref target="node-00000000-0000-0000-0000-0000000000E2" />
                        </eg:item>
                      </eg:outputs>
                    <eg:options>
                        <eg:item name="dim"><eg:json>0</eg:json></eg:item>
                        </eg:options>
                    </eg:operation>
                  <eg:tensor id="node-00000000-0000-0000-0000-0000000000E3">
                    <eg:dtype>float32</eg:dtype>
                    <eg:shape>
                      <eg:dim size="50"/>
                      <eg:dim size="2"/>
                      </eg:shape>
                    </eg:tensor>
                    </eg:nodes>
                  </eg:graph>
                """);

    var lg = LGraph.from(doc);

    var c = lg.clone();

    bh.consume(c);
  }
}
