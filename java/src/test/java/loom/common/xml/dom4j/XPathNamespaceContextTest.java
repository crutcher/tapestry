package loom.common.xml.dom4j;

import loom.graph.LoomXml;
import loom.testing.BaseTestClass;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;
import org.junit.Test;

public class XPathNamespaceContextTest extends BaseTestClass {

  @Test
  public void testSelectors() throws Exception {
    String content =
        """
              <?xml version="1.0" encoding="UTF-8"?>
              <f:graph
                xmlns:f="http://loom-project.org/schemas/v0.1/ExpressionGraph.core.xsd"
                >
                <f:nodes>
                  <f:tensor id="2">
                    <f:dtype>float32</f:dtype>
                    <f:shape>
                      <f:dim size="30"/>
                      <f:dim size="2"/>
                      </f:shape>
                    </f:tensor>
                  <f:tensor id="1">
                    <f:annotations>
                      <f:pre>foo</f:pre>
                      </f:annotations>
                    <f:dtype>float32</f:dtype>
                    <f:shape>
                      <f:dim size="30"/>
                      <f:dim size="2"/>
                      </f:shape>
                    </f:tensor>
                  </f:nodes>
                </f:graph>
              """;

    var doc = DocumentHelper.parseText(content);

    final var xpCtx =
        XPathNamespaceContext.builder().namespace("eg", LoomXml.EG_CORE_SCHEMA_URI).build();

    assertThat(xpCtx.selectNodes(doc, "//eg:tensor"))
        .extracting(n -> n.valueOf("@id"))
        .containsExactly("2", "1");

    assertThat(xpCtx.evaluate(doc, "count(//eg:tensor)")).isEqualTo(2.0);

    assertThat(xpCtx.selectSingleNode(doc, "//eg:tensor[@id='1']"))
        .isInstanceOf(Node.class)
        .isNotNull();

    assertThat(xpCtx.valueOf(doc, "//eg:tensor[@id='1']/eg:dtype")).isEqualTo("float32");

    assertThat(xpCtx.selectNodes(doc, "//eg:tensor", "@id"))
        .extracting(n -> n.valueOf("@id"))
        .containsExactly("1", "2");

    assertThat(xpCtx.numberValueOf(doc, "count(//eg:tensor)")).isEqualTo(2.0);

    assertThat(xpCtx.booleanValueOf(doc, "count(//eg:tensor) = 1")).isEqualTo(false);

    {
      var node = xpCtx.selectSingleNode(doc, "//eg:tensor[@id='1']");
      assertThat(node).isNotNull();

      assertThat(xpCtx.matches(node, ".[@id='1']")).isTrue();

      assertThat(xpCtx.matches(node, ".[@id='2']")).isFalse();
    }
  }
}
