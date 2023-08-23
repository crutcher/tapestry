package loom.graph;

import loom.testing.CommonAssertions;
import org.junit.Test;

public class ExpressionGraphValidatorTest implements CommonAssertions {
  @Test
  public void testDisplayNodeInContext() {
    var doc = LoomXmlResources.parse("<foo><bar><baz/></bar></foo>");

    assertThat(ExpressionGraphValidator.instance.prettyContext(doc))
        .isEqualTo(
            """
                        > <foo>
                        >   <bar>
                        >     <baz/>
                        >   </bar>
                        > </foo>
                        """);
  }

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
                          <eg:ref target="node-00000000-0000-0000-0000-0000000000A1" />
                          <eg:ref target="node-00000000-0000-0000-0000-0000000000E1" />
                          <eg:ref target="node-00000000-0000-0000-0000-0000000000E2" />
                          </eg:item>
                        </eg:inputs>
                      <eg:outputs>
                        <foo>bar</foo>
                        <eg:item key="out">
                          <eg:ref target="node-00000000-0000-0000-0000-0000000000E3" />
                          </eg:item>
                        <eg:item key="out">
                          <eg:ref target="node-00000000-0000-0000-0000-0000000000E3" />
                          </eg:item>
                        </eg:outputs>
                      <eg:options>
                          <eg:item key="dim"><eg:json>0, "[</eg:json></eg:item>
                          <eg:item key="foo">
                            <eg:json>2</eg:json>
                            <eg:json>"ab</eg:json>
                          </eg:item>
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

    ValidationReport report;
    try {
      ExpressionGraph.from(content);
      fail("Expected exception");
      return;
    } catch (LoomValidationException ex) {
      // Expected
      report = ex.report;
      assertThat(report).isNotNull();
    }

    System.err.println(report);

    assertThat(report.getIssues()).hasSize(5);

    assertThat(report.getIssues().stream().map(i -> i.type))
        .containsExactlyInAnyOrder(
            "XsdSchema", "XsdSchema", "RefTargetType", "JsonParse", "JsonParse");
  }
}
