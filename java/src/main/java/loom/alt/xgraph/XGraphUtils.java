package loom.alt.xgraph;

import java.io.InputStream;
import java.util.Iterator;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import loom.common.w3c.NodeListList;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public final class XGraphUtils {
  private XGraphUtils() {}

  public static final String NS = "http://loom.org/v1";

  public static final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

  static {
    dbFactory.setNamespaceAware(true);
  }

  public static final DocumentBuilder documentBuilder;

  static {
    try {
      documentBuilder = dbFactory.newDocumentBuilder();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static Document parse(InputStream is) {
    try {
      return documentBuilder.parse(is);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static final SchemaFactory schemaFactory =
      SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
  public static final XPathFactory xpathFactory = XPathFactory.newInstance();
  public static final XPath xpath = xpathFactory.newXPath();

  static {
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
  }

  public static final String SCHEMA_RESOURCE_PATH = "loom/alt/xgraph/graph.xsd";

  public static Schema getSchema() {
    try {
      InputStream is = XGraphUtils.class.getClassLoader().getResourceAsStream(SCHEMA_RESOURCE_PATH);
      return schemaFactory.newSchema(new StreamSource(is));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static NodeListList graphNodes(Document doc) {
    try {
      return new NodeListList(
          (NodeList)
              xpath.evaluate("/t:graph/t:node", doc, javax.xml.xpath.XPathConstants.NODESET));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
