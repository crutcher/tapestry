package loom.graph;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import loom.common.xml.w3c.NodeListList;
import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class LoomXmlResources {
  public static final String EG_CORE_SCHEMA_URI =
      "http://loom-project.org/schemas/v0.1/ExpressionGraph.core.xsd";

  public static final String EG_CORE_XSD_VALIDATOR_RESOURCE_PATH =
      "loom/alt/xgraph/ExpressionGraph.core.xsd";
  public static final String EG_EXT_SCHEMA_URI =
      "http://loom-project.org/schemas/v0.1/ExpressionGraph.ext.xsd";

  public static final String EG_EXT_XSD_VALIDATOR_RESOURCE_PATH =
      "loom/alt/xgraph/ExpressionGraph.ext.xsd";

  public record SchemaResource(String prefix, String namespace, String resourcePath) {}

  public static final List<SchemaResource> SCHEMA_RESOURCES =
      List.of(
          new SchemaResource("loom", EG_CORE_SCHEMA_URI, EG_CORE_XSD_VALIDATOR_RESOURCE_PATH),
          new SchemaResource("ext", EG_EXT_SCHEMA_URI, EG_EXT_XSD_VALIDATOR_RESOURCE_PATH));

  public static final String EG_CORE_XSLT_RESOURCE_PATH = "loom/alt/xgraph/Validate.core.xsl";
  public static final String LOOM_EG_TO_DOT_XSL_RESOURCE_PATH = "loom/alt/xgraph/LGraphToDot.xsl";
  public static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY =
      DocumentBuilderFactory.newInstance();
  public static final DocumentBuilder DOCUMENT_BUILDER;
  public static final SchemaFactory SCHEMA_FACTORY =
      SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
  public static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();
  public static final XPathFactory XPATH_FACTORY = XPathFactory.newInstance();
  public static final XPath XPATH = XPATH_FACTORY.newXPath();

  static {
    XPATH.setNamespaceContext(
        new NamespaceContext() {
          @Override
          public String getNamespaceURI(String prefix) {
            if (prefix == null) {
              throw new NullPointerException("Null prefix");
            } else if ("xml".equals(prefix)) {
              return XMLConstants.XML_NS_URI;
            } else if ("xs".equals(prefix)) {
              return XMLConstants.W3C_XML_SCHEMA_NS_URI;
            } else if ("xsi".equals(prefix)) {
              return XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;
            } else {
              for (var sr : SCHEMA_RESOURCES) {
                if (sr.prefix().equals(prefix)) {
                  return sr.namespace();
                }
              }
            }
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

  static {
    DOCUMENT_BUILDER_FACTORY.setNamespaceAware(true);
    DOCUMENT_BUILDER_FACTORY.setIgnoringComments(true);
    DOCUMENT_BUILDER_FACTORY.setIgnoringElementContentWhitespace(true);
  }

  static {
    try {
      DOCUMENT_BUILDER = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static Document parse(InputStream is) {
    try {
      var doc = DOCUMENT_BUILDER.parse(is);
      collapseWhitespaceTextNodes(doc);
      return doc;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static Document parse(String source) {
    return parse(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)));
  }

  /**
   * Removes all descendent text nodes that contain only whitespace.
   *
   * @param node The node to collapse whitespace text nodes for.
   */
  public static void collapseWhitespaceTextNodes(Node node) {
    Set<Node> toRemove = new HashSet<>();

    List<Node> toVisit = new ArrayList<>();
    toVisit.add(node);

    while (!toVisit.isEmpty()) {
      Node n = toVisit.remove(0);

      if (n.getNodeType() == Node.TEXT_NODE) {
        if (n.getNodeValue().trim().isEmpty()) {
          toRemove.add(n);
        }
      } else {
        toVisit.addAll(NodeListList.of(n.getChildNodes()));
      }
    }

    toRemove.forEach(n -> n.getParentNode().removeChild(n));
  }

  private static Transformer outputTransformer() {
    Transformer transformer;
    try {
      transformer = TRANSFORMER_FACTORY.newTransformer();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

    return transformer;
  }

  public static String documentToPrettyString(Node node) {
    try {
      DOMSource domSource = new DOMSource(node);
      StringWriter writer = new StringWriter();
      StreamResult result = new StreamResult(writer);

      var transformer = outputTransformer();

      transformer.transform(domSource, result);

      return writer.toString();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void documentToFile(Node node, File path) {
    try {
      DOMSource domSource = new DOMSource(node);
      FileWriter writer = new FileWriter(path, StandardCharsets.UTF_8);
      StreamResult result = new StreamResult(writer);

      var transformer = outputTransformer();
      transformer.transform(domSource, result);

    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public static File documentToTempFile(Node node) {
    try {
      File tempFile = File.createTempFile("tmp", ".tmp");
      documentToFile(node, tempFile);
      tempFile.deleteOnExit();
      return tempFile;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Generates an XPath selector for the given node, from the document root.
   *
   * @param node The node to generate the XPath for.
   * @param elementIdAttributes The set of (element, attributes) that are considered to be unique
   *     identifiers at the sibling level for nodes in the tree.
   * @return an XPath selector for the given node.
   */
  public static String generateXPathSelector(
      Node node, List<Pair<String, String>> elementIdAttributes) {
    if (node == null || node.getNodeType() != Node.ELEMENT_NODE) {
      return null;
    }

    // Step 1: Check if it's the root node
    Node parent = node.getParentNode();
    if (parent == null) {
      return "/";
    }

    StringBuilder xpath = new StringBuilder();

    // Step 2: Traverse from the node up to the root
    while (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
      String nodeName = node.getNodeName();
      String step = "/" + nodeName;

      boolean withId = false;
      for (var elemIdAttrib : elementIdAttributes) {
        String elemName = elemIdAttrib.getLeft();
        if (elemName != null && !elemName.equals(nodeName)) {
          continue;
        }
        String idAttr = elemIdAttrib.getRight();

        if (node.getAttributes().getNamedItem(idAttr) != null) {
          step +=
              "[@"
                  + idAttr
                  + "='"
                  + node.getAttributes().getNamedItem(idAttr).getNodeValue()
                  + "']";
          withId = true;
          break;
        }
      }
      if (!withId) {
        int position = getPositionAmongSiblings(node);
        if (position != -1) {
          step += "[" + (position + 1) + "]";
        }
      }

      xpath.insert(0, step);
      node = node.getParentNode();
    }

    return xpath.toString();
  }

  private static int getPositionAmongSiblings(Node node) {
    if (node == null || node.getParentNode() == null) {
      return -1; // Default position
    }

    var name = node.getNodeName();

    Node parent = node.getParentNode();
    var siblings =
        NodeListList.of(parent.getChildNodes()).stream()
            .filter(n -> n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals(name))
            .toList();

    int siblingCount = siblings.size();
    int position = siblings.indexOf(node);

    if (siblingCount == 1) {
      return -1; // No position
    }
    return position;
  }
}
