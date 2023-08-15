package loom.experiment;

import loom.graph.XGraphUtils;
import org.junit.Test;
import org.w3c.dom.Document;

import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class XsltTests {
  @SuppressWarnings("unused")
  @Test
  public void testFunc() throws Exception{
    var xml =
        XGraphUtils.parse(
            """
                <?xml version="1.0" encoding="UTF-8"?>
                <foo a="1"/>
                """);

    var xsl =
            """
<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:my="http://loom-project.org/ext/xsllib.xsl"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:func="http://exslt.org/functions"
                extension-element-prefixes="func"
>
<xsl:output method="xml" indent="yes"/>

<func:function name="my:xyz">
  <xsl:param name="x"/>
  <func:result select="$x + 2"/>
</func:function>

<xsl:template match="/foo">
  <bar>
    <b>
    <xsl:value-of select="my:xyz(3)"/>
    </b>
    </bar>
</xsl:template>

</xsl:stylesheet>
                        """;

    var xslSource = new StreamSource(new ByteArrayInputStream(xsl.getBytes(StandardCharsets.UTF_8)));
    var transform = XGraphUtils.TRANSFORMER_FACTORY.newTransformer(xslSource);

    Document resultDoc = XGraphUtils.DOCUMENT_BUILDER.newDocument();
    transform.transform(new DOMSource(xml), new DOMResult(resultDoc));

    System.out.println(XGraphUtils.documentToString(resultDoc));
  }
}
