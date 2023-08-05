package loom.alt.xgraph;

import loom.testing.CommonAssertions;
import org.junit.Test;
import org.w3c.dom.Node;

import java.util.stream.Collectors;

import static javax.xml.xpath.XPathConstants.NODE;

public class LGraphTest implements CommonAssertions {
  @Test
  public void testCreate() {
    var doc = LGraph.create();
    doc.validate();
  }

  @Test
  public void testFrom() throws Exception {
    var graph =
        LGraph.from(
            """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <eg:graph
                      xmlns:eg="http://loom-project.org/schemas/v0.1/ExpressionGraph.core.xsd"
                      xmlns:ext="http://loom-project.org/schemas/v0.1/ExpressionGraph.ext.xsd"
                      >
                      <eg:tensor id="node-00000000-0000-0000-0000-0000000000E1" dtype="float32" shape="[30, 2]"/>
                      <eg:tensor id="node-00000000-0000-0000-0000-0000000000E2" dtype="float32" shape="[20, 3]"/>
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
                        <eg:results>
                          <eg:item name="out">
                            <eg:ref target="node-00000000-0000-0000-0000-0000000000E2" />
                            </eg:item>
                          </eg:results>
                        <eg:options>
                            <eg:item name="dim"><eg:json>0</eg:json></eg:item>
                            </eg:options>
                        </eg:operation>
                      <eg:tensor id="node-00000000-0000-0000-0000-0000000000E3" dtype="float32" shape="[50, 2]"/>
                      </eg:graph>
                    """);

    var schemaNodes = XGraphUtils.loadSchemaNodes(graph.getDoc());
    for (var node : schemaNodes) {
      var n = (Node) XGraphUtils.xpath.evaluate("//xs:element[@name='tensor']", node, NODE);
      if (n != null) {
        System.out.println(n);
      }

      System.out.println(
          "type: "
              + XGraphUtils.xpath.evaluate(
                  "//xs:element[@name='tensor']/descendant::xs:attribute[@name='shape']/@type",
                  node,
                  NODE));
    }

    System.out.println(XGraphUtils.documentNamespaceMap(graph.getDoc()));

    assertThat(graph.listNodes().stream().map(LGraph.NodeHandle::getId))
        .contains(
            "node-00000000-0000-0000-0000-0000000000E0",
            "node-00000000-0000-0000-0000-0000000000E1");

    var nodes =
        graph.listNodes().stream().collect(Collectors.toMap(LGraph.NodeHandle::getId, n -> n));
    assertThat(graph.getNode("node-00000000-0000-0000-0000-0000000000E0"))
        .isEqualTo(nodes.get("node-00000000-0000-0000-0000-0000000000E0"));
  }
}
