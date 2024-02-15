package org.tensortapestry.weft.metakernels;

import guru.nidi.graphviz.engine.Format;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.CommonAssertions;
import org.tensortapestry.loom.graph.CommonEnvironments;
import org.tensortapestry.loom.graph.dialects.tensorops.*;
import org.tensortapestry.loom.graph.export.graphviz.GraphVisualizer;

public class MKTest implements CommonAssertions {

  @Test
  @SuppressWarnings("unused")
  public void test() {
    var env = CommonEnvironments.expressionEnvironment();
    var graph = env.newGraph();

    var tensorA = TensorNode
      .builder(graph)
      .label("A")
      .body(b -> b.dtype("int32").shape(10, 10))
      .build();

    var tensorB = TensorNode
      .builder(graph)
      .label("B")
      .body(b -> b.dtype("int32").shape(10, 10))
      .build();

    var tensorC = CommonMetaKernels.ADD
      .on(graph)
      .input("tensors", List.of(TensorSelection.from(tensorA), TensorSelection.from(tensorB)))
      .apply()
      .getResult();

    graph.validate();
    var exporter = GraphVisualizer.buildDefault();
    var export = exporter.export(graph);
    var gv = export.getGraphviz();
    var img = gv.render(Format.PNG).toImage();
  }
}
