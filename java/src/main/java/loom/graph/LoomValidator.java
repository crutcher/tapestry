package loom.graph;

import com.fasterxml.jackson.databind.JsonNode;
import loom.common.serialization.JsonUtil;
import loom.common.w3c.ErrorCollectingValidationHandler;
import loom.common.w3c.NodeListList;
import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

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
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.net.URI;
import java.util.*;

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

  public ValidationReport validationReport(Document doc) {
    // TODO: collect as many errors as possible, present them at once.
    // This may not be possible, malformation at one layer may prevent other checks;
    // but the goal is to generate structured lint output here as a lib, and
    // then in the "just validate" context, format it to a string and throw an error.
    var report = new ValidationReport();

    var validator = getSchema().newValidator();
    var errorCollector = new ErrorCollectingValidationHandler();
    validator.setErrorHandler(errorCollector);

    try {
      validator.validate(new DOMSource(doc));
    } catch (SAXException e) {
      // This catch is if a fatalError occurs. Other errors and warnings are captured by the
      // ErrorHandler.
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    for (var e : errorCollector.getExceptions()) {
      var details = new LinkedHashMap<String, String>();
      report.issues.add(
          ValidationReport.Issue.builder()
              .type("XsdSchema")
              .lineNumber(e.getLineNumber())
              .details(details)
              .summary("Schema validation error")
              .message(e.getMessage())
              .build());
    }

    for (var transformer : transformers) {
      Document resultDoc = DOCUMENT_BUILDER.newDocument();
      try {
        transformer.transform(new DOMSource(doc), new DOMResult(resultDoc));
      } catch (TransformerException e) {
        throw new LoomValidationException(e.getMessage(), e);
      }

      for (var error : NodeListList.of(resultDoc.getElementsByTagName("error"))) {

        NamedNodeMap attributes = error.getAttributes();
        var type = attributes.getNamedItem("type").getTextContent();
        var path = attributes.getNamedItem("path").getTextContent();

        var details = new LinkedHashMap<String, String>();
        try {
          var detailsNode =
              (Node) XGraphUtils.xpath.evaluate("details", error, XPathConstants.NODE);
          if (detailsNode != null) {
            for (var detail : NodeListList.of(detailsNode.getChildNodes())) {
              details.put(detail.getNodeName(), detail.getTextContent());
            }
          }
        } catch (XPathExpressionException e) {
          throw new RuntimeException(e);
        }

        String summary;
        try {
          summary = (String) XGraphUtils.xpath.evaluate("summary", error, XPathConstants.STRING);
        } catch (XPathExpressionException e) {
          throw new RuntimeException(e);
        }

        StringBuilder sb = new StringBuilder();

        // TODO: maybe a rendering XSLT, or some XPath here?
        // select the 'message' element child of 'node':
        var message =
            NodeListList.of(error.getChildNodes()).stream()
                .filter(n -> n.getNodeName().equals("message"))
                .findFirst()
                .get();
        var render = XGraphUtils.documentToString(message);
        // Drop the wrapper.
        sb.append(render.replace("<message>", "").replace("</message>", ""));

        report.issues.add(
            ValidationReport.Issue.builder()
                .xpath(path)
                .type(type)
                .summary(summary)
                .message(sb.toString())
                .details(details)
                .build());
      }
    }

    // Parse all the JSON
    // It would be nice to apply JSON schema to things; if we had a way to match them.
    for (var jsonNode : NodeListList.of(doc.getElementsByTagNameNS(EG_SCHEMA_URI, "json"))) {
      var json = jsonNode.getTextContent().trim();

      try {
        JsonUtil.fromJson(json, JsonNode.class);
      } catch (Exception e) {
        report.issues.add(
            ValidationReport.Issue.builder()
                .type("JsonParse")
                .xpath(
                    XGraphUtils.generateXPathSelector(
                        jsonNode, List.of(Pair.of(null, "id"), Pair.of("eg:item", "key"))))
                .summary("JSON parse error")
                .message(e.getMessage().trim())
                .details(Map.of("json", "<%s>".formatted(json)))
                .build());
      }
    }

    return report;
  }

  public void validate(Document doc) {
    var report = validationReport(doc);
    if (!report.isValid()) {
      throw new LoomValidationException(report.toCollatedString(doc));
    }
  }
}
