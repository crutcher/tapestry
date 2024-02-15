package org.tensortapestry.weft.metakernels;

import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.CommonAssertions;
import org.tensortapestry.loom.graph.CommonEnvironments;
import org.tensortapestry.loom.graph.dialects.tensorops.*;
import org.tensortapestry.zspace.ZPoint;

public class MKTest implements CommonAssertions {

  @Test
  @SuppressWarnings("unused")
  public void test() {
    var env = CommonEnvironments.expressionEnvironment();
    var graph = env.newGraph();

    var tensorA = TensorNode
      .on(graph)
      .label("A")
      .body(b -> b.dtype("float32").shape(10, 10, 10))
      .build();

    var tensorB = TensorNode
      .on(graph)
      .label("B")
      // TODO: fix .shape(1, 10) here.
      .body(b -> b.dtype("float32").shape(10, 10, 10))
      .build();

    var tensorC = CommonMetaKernels.ADD
      .on(graph)
      .input("tensors", tensorA, tensorB)
      .apply()
      .getResult();


    assertThat(tensorC.getShape()).isEqualTo(ZPoint.of(10, 10, 10));
    assertThat(tensorC.getDtype()).isEqualTo("float32");

    graph.validate();
    // var exporter = GraphVisualizer.buildDefault();
    // var export = exporter.export(graph);
    // var gv = export.getGraphviz();
    // var img = gv.render(Format.PNG).toImage();
  }
}
