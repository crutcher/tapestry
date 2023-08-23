package loom.graph;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Splitter;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import lombok.Builder;
import lombok.Data;
import loom.common.runtime.ReflectionUtils;
import loom.common.serialization.JsonUtil;
import loom.common.xml.w3c.ErrorCollectingValidationHandler;
import loom.common.xml.w3c.NodeListList;
import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

@Builder
@Data
public final class ExpressionGraphValidator {
  static final class ValidationContext {
    final Document doc;
    final String content;
    final List<String> lines;
    final ValidationReport report;

    ValidationContext(Document doc) {
      this.doc = doc;
      this.content = LoomXmlResources.documentToPrettyString(doc);
      this.lines = Arrays.asList(content.split("\n"));
      this.report = new ValidationReport();
    }
  }

  @Builder.Default private final Integer contextDepth = 1;

  @Builder.Default private final Integer contextLines = 3;

  public static final ExpressionGraphValidator instance =
      ExpressionGraphValidator.builder().build();

  static {
    LoomXmlResources.SCHEMA_RESOURCES.forEach(
        r -> {
          instance.addSchema(
              URI.create(r.namespace()), ReflectionUtils.resourceAsStream(r.resourcePath()));
        });

    instance.addValidatorTransform(
        ReflectionUtils.resourceAsStream(LoomXmlResources.EG_CORE_XSLT_RESOURCE_PATH));
  }

  @Builder.Default Map<URI, Document> schemaDocuments = new HashMap<>();

  @Builder.Default List<Transformer> transformers = new ArrayList<>();

  public void addSchema(URI uri, InputStream stream) {
    addSchema(uri, LoomXmlResources.parse(stream));
  }

  public void addSchema(URI uri, Document document) {
    schemaDocuments.put(uri, document);
  }

  public Schema getSchemaForDocument(Document doc) {
    return getSchema();
  }

  public Schema getSchema() {
    var sources = schemaDocuments.values().stream().map(DOMSource::new).toArray(Source[]::new);
    try {
      return LoomXmlResources.SCHEMA_FACTORY.newSchema(sources);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void addValidatorTransform(InputStream stream) {
    try {
      addValidatorTransform(
          LoomXmlResources.TRANSFORMER_FACTORY.newTransformer(
              new DOMSource(LoomXmlResources.parse(stream))));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void addValidatorTransform(Transformer transformer) {
    transformers.add(transformer);
  }

  /**
   * Return a pretty-printed representation of the node, with context.
   *
   * <p>Context is taken from the configured contextDepth.
   *
   * @param node the node to pretty-print.
   * @return a pretty-printed representation of the node, with context.
   */
  String prettyContext(Node node) {
    var ancestor = node;
    int idx = 0;
    for (; idx < contextDepth; idx++) {
      var parent = ancestor.getParentNode();
      if (parent == null) {
        break;
      }
      ancestor = parent;
    }

    var sb = new StringBuilder();

    for (var line :
        Splitter.on("\n").split(LoomXmlResources.documentToPrettyString(ancestor).trim())) {
      sb.append("> %s".formatted(line));
      sb.append("\n");
    }
    if (idx > 0) {
      sb.append("[depth=%d]".formatted(idx));
    }

    return sb.toString();
  }

  void xsdValidation(ValidationContext context) {
    var validator = getSchemaForDocument(context.doc).newValidator();

    var errorCollector = new ErrorCollectingValidationHandler();
    validator.setErrorHandler(errorCollector);
    try {
      // We validate against the content, so we can get the line numbers.
      validator.validate(
          new StreamSource(
              new ByteArrayInputStream(context.content.getBytes(StandardCharsets.UTF_8))));
    } catch (SAXException e) {
      // This catch is if a fatalError occurs. Other errors and warnings are captured by the
      // ErrorHandler.
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    for (var e : errorCollector.getExceptions()) {
      var details = new LinkedHashMap<String, String>();

      ValidationReport.Issue.IssueBuilder issue =
          ValidationReport.Issue.builder()
              .type("XsdSchema")
              .lineNumber(e.getLineNumber())
              .details(details)
              .summary("Schema validation error")
              .message(e.getMessage());

      if (e.getLineNumber() >= 0) {
        var lineNo = e.getLineNumber();
        var columnNo = e.getColumnNumber();

        var start = Math.max(0, lineNo - contextLines - 1);
        var end = Math.min(context.lines.size(), lineNo + contextLines);

        var cb = new StringBuilder();
        for (var i = start; i < end; i++) {
          var marker = i == lineNo - 1 ? ">" : " ";
          cb.append("%d:%s%s".formatted(i, marker, context.lines.get(i)));
          cb.append("\n");

          if (i == lineNo - 1) {
            cb.append(" ".repeat(columnNo - 1));
            cb.append("^");
            cb.append("\n");
          }
        }

        issue.context(cb.toString());
      }

      context.report.addIssue(issue);
    }
  }

  void jsonValidation(ValidationContext context) {

    // Parse all the JSON
    // It would be nice to apply JSON schema to things; if we had a way to match them.
    for (var jsonNode :
        NodeListList.of(
            context.doc.getElementsByTagNameNS(LoomXmlResources.EG_CORE_SCHEMA_URI, "json"))) {
      var json = jsonNode.getTextContent().trim();

      try {
        JsonUtil.fromJson(json, JsonNode.class);
      } catch (Exception e) {
        var issue =
            ValidationReport.Issue.builder()
                .type("JsonParse")
                .xpath(
                    LoomXmlResources.generateXPathSelector(
                        jsonNode, List.of(Pair.of(null, "id"), Pair.of("eg:item", "key"))))
                .summary("JSON parse error")
                .message(e.getMessage().trim())
                .details(Map.of("json", "<%s>".formatted(json)))
                .context(prettyContext(jsonNode));

        context.report.addIssue(issue);
      }
    }
  }

  private void xsltValidation(ValidationContext context) {
    for (var transformer : transformers) {
      Document resultDoc = LoomXmlResources.DOCUMENT_BUILDER.newDocument();
      try {
        transformer.transform(new DOMSource(context.doc), new DOMResult(resultDoc));
      } catch (TransformerException e) {
        throw new RuntimeException(e);
      }

      for (var error : NodeListList.of(resultDoc.getElementsByTagName("error"))) {
        NamedNodeMap attributes = error.getAttributes();
        var type = attributes.getNamedItem("type").getTextContent();
        var path = attributes.getNamedItem("path").getTextContent();

        var issue = ValidationReport.Issue.builder().type(type).xpath(path);

        if (path == null) {
          try {
            var contextNode =
                (Node) LoomXmlResources.XPATH.evaluate(path, context.doc, XPathConstants.NODE);
            if (contextNode != null) {
              issue.context(prettyContext(contextNode));
            }
          } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
          }
        }

        try {
          var detailsNode =
              (Node) LoomXmlResources.XPATH.evaluate("details", error, XPathConstants.NODE);
          if (detailsNode != null) {
            var details = new LinkedHashMap<String, String>();
            for (var detail : NodeListList.of(detailsNode.getChildNodes())) {
              details.put(detail.getNodeName(), detail.getTextContent());
            }
            issue.details(details);
          }
        } catch (XPathExpressionException e) {
          throw new RuntimeException(e);
        }

        try {
          String summary =
              (String) LoomXmlResources.XPATH.evaluate("summary", error, XPathConstants.STRING);
          issue.summary(summary);
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
        var render = LoomXmlResources.documentToPrettyString(message);
        // Drop the wrapper.
        sb.append(render.replace("<message>", "").replace("</message>", ""));

        issue.message(sb.toString());

        context.report.addIssue(issue);
      }
    }
  }

  public ValidationReport validationReport(Document doc) {
    // TODO: collect as many errors as possible, present them at once.
    // This may not be possible, malformation at one layer may prevent other checks;
    // but the goal is to generate structured lint output here as a lib, and
    // then in the "just validate" context, format it to a string and throw an error.

    var context = new ValidationContext(doc);

    xsdValidation(context);
    xsltValidation(context);
    jsonValidation(context);

    return context.report;
  }

  public void validate(Document doc) {
    var report = validationReport(doc);
    if (!report.isValid()) {
      throw new LoomValidationException(report);
    }
  }
}
