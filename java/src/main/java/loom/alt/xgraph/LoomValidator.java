package loom.alt.xgraph;

import loom.common.w3c.NodeListList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LoomValidator {
  public static final String EG_SCHEMA_URI =
      "http://loom-project.org/schemas/v0.1/ExpressionGraph.core.xsd";

  public static final String EG_EXT_SCHEMA_URI =
      "http://loom-project.org/schemas/v0.1/ExpressionGraph.ext.xsd";

  public static final String EXPRESSION_GRAPH_CORE_XSD = "loom/alt/xgraph/ExpressionGraph.core.xsd";
  public static final String EXPRESSION_GRAPH_EXT_XSD = "loom/alt/xgraph/ExpressionGraph.ext.xsd";

  public record SchemaResource(String prefix, String namespace, String resourcePath) {}

  public static final List<SchemaResource> SCHEMA_RESOURCES =
      List.of(
          new SchemaResource("loom", EG_SCHEMA_URI, EXPRESSION_GRAPH_CORE_XSD),
          new SchemaResource("ext", EG_EXT_SCHEMA_URI, EXPRESSION_GRAPH_EXT_XSD));

  public static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY =
      DocumentBuilderFactory.newInstance();

  static {
    DOCUMENT_BUILDER_FACTORY.setNamespaceAware(true);
    DOCUMENT_BUILDER_FACTORY.setIgnoringComments(true);
    DOCUMENT_BUILDER_FACTORY.setIgnoringElementContentWhitespace(true);
  }

  public static final String XG_CORE_VALIDATOR_XSLT_RESOURCE_PATH =
      "loom/alt/xgraph/Validate.core.xsl";

  public static final DocumentBuilder DOCUMENT_BUILDER;

  static {
    try {
      DOCUMENT_BUILDER = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static final SchemaFactory SCHEMA_FACTORY =
      SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

  private static InputStream resourceAsStream(String path) {
    return LoomValidator.class.getClassLoader().getResourceAsStream(path);
  }

  private static Document parse(InputStream is) {
    try {
      return DOCUMENT_BUILDER.parse(is);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();

  public static final XPathFactory XPATH_FACTORY = XPathFactory.newInstance();
  public static final XPath XPATH = XPATH_FACTORY.newXPath();

  public static final LoomValidator instance = new LoomValidator();

  static {
    SCHEMA_RESOURCES.forEach(
        r -> {
          instance.addSchema(URI.create(r.namespace), resourceAsStream(r.resourcePath));
        });

    instance.addValidatorTransform(resourceAsStream(XG_CORE_VALIDATOR_XSLT_RESOURCE_PATH));
  }

  Map<URI, Document> schemaDocuments = new HashMap<>();
  List<Transformer> transformers = new ArrayList<>();

  public void addSchema(URI uri, InputStream stream) {
    addSchema(uri, parse(stream));
  }

  public void addSchema(URI uri, Document document) {
    schemaDocuments.put(uri, document);
  }

  public Schema getSchema() {
    var sources = schemaDocuments.values().stream().map(DOMSource::new).toArray(Source[]::new);
    try {
      return SCHEMA_FACTORY.newSchema(sources);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void addValidatorTransform(InputStream stream) {
    try {
      addValidatorTransform(TRANSFORMER_FACTORY.newTransformer(new DOMSource(parse(stream))));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void addValidatorTransform(Transformer transformer) {
    transformers.add(transformer);
  }

  public void validate(Document doc) {
    try {
      getSchema().newValidator().validate(new DOMSource(doc));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    List<Node> errors = new ArrayList<>();
    for (var transformer : transformers) {
      Document resultDoc = DOCUMENT_BUILDER.newDocument();
      try {
        transformer.transform(new DOMSource(doc), new DOMResult(resultDoc));
        errors.addAll(NodeListList.of(resultDoc.getElementsByTagName("error")));
      } catch (TransformerException e) {
        throw new RuntimeException(e);
      }
    }

    if (!errors.isEmpty()) {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < errors.size(); i++) {
        var error = errors.get(i);
        sb.append("\n");

        sb.append(XGraphUtils.documentToString(error));
      }
      sb.append("\n");

      throw new IllegalArgumentException(sb.toString());
    }
  }
}
