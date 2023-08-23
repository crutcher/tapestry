package loom.graph;

import loom.testing.CommonAssertions;
import org.junit.Test;

public class ExpressionGraphTest implements CommonAssertions {

  @SuppressWarnings("unused")
  @Test
  public void testValidate() {
    var content =
        """
            <?xml version="1.0" encoding="UTF-8"?>
            <eg:graph
              xmlns:eg="http://loom-project.org/schemas/v0.1/ExpressionGraph.core.xsd"
              >
              <eg:nodes>
                <eg:tensor id="node-00000000-0000-0000-0000-0000000000E1">
                  <eg:annotations>
                    <eg:pre>foo</eg:pre>
                    </eg:annotations>
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
                    target="node-00000000-0000-0000-0000-0000000000E0">
                  <eg:pre>foo</eg:pre>
                  </eg:trace>
                <eg:operation id="node-00000000-0000-0000-0000-0000000000E0"
                    op="loom:concat">
                  <eg:inputs>
                    <eg:item key="source">
                      <eg:ref target="node-00000000-0000-0000-0000-0000000000E1" />
                      <eg:ref target="node-00000000-0000-0000-0000-0000000000E2" />
                      </eg:item>
                    </eg:inputs>
                  <eg:outputs>
                    <eg:item key="out">
                      <eg:ref target="node-00000000-0000-0000-0000-0000000000E3" />
                      </eg:item>
                    </eg:outputs>
                  <eg:options>
                      <eg:item key="dim"><eg:json>0</eg:json></eg:item>
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
            """;

    var graph = ExpressionGraph.from(content);

    var vis = ExpressionGraphVisualizer.builder().graph(graph).build();

    var dot = vis.toDot();
    var img = vis.toImage();
  }
}
