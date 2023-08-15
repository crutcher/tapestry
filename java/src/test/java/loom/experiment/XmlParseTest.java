package loom.experiment;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import loom.testing.CommonAssertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class XmlParseTest implements CommonAssertions {
  @Rule public TemporaryFolder temp = new TemporaryFolder();

  private File writeTemp(String localName, String content) {
    try {
      File file = temp.newFile(localName);
      // Write `schemaJson' to `schemaFile'
      try (var writer = new java.io.FileWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
        writer.write(content);
      }
      return file;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testSchema() throws Exception {
    String content =
        """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <xs:schema
                  version="1.0"
                  xmlns:xs="http://www.w3.org/2001/XMLSchema"
                  xmlns:tns="http://loom.org/v1"
                  targetNamespace="http://loom.org/v1"
                  elementFormDefault="qualified"
                  >

                  <xs:element name="example" type="tns:ExampleType"/>

                  <xs:complexType name="ExampleType">
                    <xs:sequence>
                      <xs:element name="a" type="xs:string" minOccurs="1" maxOccurs="1"/>
                      <xs:element name="b" type="xs:int" minOccurs="0" maxOccurs="1"/>
                    </xs:sequence>
                  </xs:complexType>
                </xs:schema>
                """;

    File schemaPath = writeTemp("schema.xsd", content);

    // Setup schema validator
    SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    var schema = sf.newSchema(schemaPath);

    @SuppressWarnings("unused")
    var validator = schema.newValidator();

    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    dbFactory.setNamespaceAware(true);

    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

    var doc =
        dBuilder.parse(
            new ByteArrayInputStream(
                """
                                        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                                        <tns:example
                                          xmlns:tns="http://loom.org/v1">
                                            <tns:a>hello</tns:a>
                                            <tns:b>2</tns:b>
                                        </tns:example>
                                        """
                    .getBytes(StandardCharsets.UTF_8)));

    // System.out.println(documentToString(doc));

    validator.validate(new DOMSource(doc));
  }

  public String documentToString(Document doc) {
    try {
      TransformerFactory tf = TransformerFactory.newInstance();
      Transformer transformer = tf.newTransformer();
      // Optional: Add indentation to the output string
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      transformer.setOutputProperty("indent", "yes");

      StringWriter writer = new StringWriter();
      transformer.transform(new DOMSource(doc), new StreamResult(writer));

      return writer.toString();
    } catch (Exception e) {
      throw new RuntimeException("Error converting to String", e);
    }
  }

  @Test
  public void testXGraph() throws Exception {
    var dbFactory = DocumentBuilderFactory.newInstance();
    dbFactory.setNamespaceAware(true);
    var dBuilder = dbFactory.newDocumentBuilder();

    @SuppressWarnings("unused")
    var doc =
        dBuilder.parse(
            new ByteArrayInputStream(
                """
                            <?xml version="1.0" encoding="UTF-8"?>
                            <t:graph
                              xmlns:t="http://loom.org/v1">
                              <t:node
                                id="00000000-0000-0000-0000-000000000001"
                                type="t:tensor">
                                <t:shape>[2, 3]</t:shape>
                                <t:dtype>float32</t:dtype>
                                </t:node>
                              <t:node
                                id="00000000-0000-0000-0000-000000000002"
                                type="t:tensor">
                                <t:shape>[2, 3]</t:shape>
                                <t:dtype>int8</t:dtype>
                                </t:node>
                              <t:node
                                id="00000000-0000-0000-0000-000000000002"
                                type="t:cast">
                                <t:input>
                                  <t:link target="00000000-0000-0000-0000-000000000001"/>
                                  </t:input>
                                <t:result>
                                  <t:link target="00000000-0000-0000-0000-000000000002"/>
                                  </t:result>
                                </t:node>
                            </t:graph>
                            """
                    .getBytes(StandardCharsets.UTF_8)));

    XPathFactory xpathFactory = XPathFactory.newInstance();
    XPath xpath = xpathFactory.newXPath();

    // Set the NamespaceContext to resolve prefixes in the XPath expression
    xpath.setNamespaceContext(
        new NamespaceContext() {
          @Override
          public String getNamespaceURI(String prefix) {
            if (prefix == null) throw new NullPointerException("Null prefix");
            else if ("t".equals(prefix)) return "http://loom.org/v1";
            else if ("xml".equals(prefix)) return XMLConstants.XML_NS_URI;
            return XMLConstants.NULL_NS_URI;
          }

          @Override
          public String getPrefix(String uri) {
            // Not needed in this context
            return null;
          }

          @Override
          public Iterator<String> getPrefixes(String uri) {
            // Not needed in this context
            return null;
          }
        });
    // Compile and execute the XPath expression
    // This example retrieves all elements with the tag name "element"
    String expression = "//t:node[@id='00000000-0000-0000-0000-000000000002']";
    @SuppressWarnings("unused")
    NodeList nodeList = (NodeList) xpath.evaluate(expression, doc, XPathConstants.NODESET);

    // for (int i = 0; i < nodeList.getLength(); i++) {
    //   System.out.println(nodeList.item(i).getTextContent());
    // }
  }
}
