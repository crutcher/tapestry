package loom.common.xml.dom4j;

import loom.graph.ExpressionGraph;
import loom.graph.LoomXmlResources;
import loom.testing.CommonAssertions;
import org.dom4j.Document;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMDocumentFactory;
import org.dom4j.io.DOMReader;
import org.dom4j.io.SAXReader;
import org.junit.Test;

import java.io.StringReader;

public class Dom4JTest implements CommonAssertions {

  @Test
  public void testparse() throws Exception {
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

    var reader = new SAXReader(DOMDocumentFactory.getInstance());
    var doc = (DOMDocument) reader.read(new StringReader(content));
  }

  @Test
  public void testBasic() throws Exception {
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

    DOMReader reader = new DOMReader();
    Document d4jdoc = reader.read(graph.getDoc());

    var ctx =
        XPathNamespaceContext.builder()
            .namespace("eg", LoomXmlResources.EG_CORE_SCHEMA_URI)
            .build();

    ctx.selectNodes(d4jdoc, "//eg:tensor")
        .forEach(
            node -> {
              System.out.println(node.asXML());
            });
  }
}
