package loom.alt.xgraph;

import lombok.Builder;
import org.w3c.dom.Document;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

@Builder
public class LGraphVisualizer {
  @Builder.Default boolean debugTransform = true;

  @Builder.Default private Integer minPrefixLength = 3;

  public Transformer getTransformer() {
    try {
      return XGraphUtils.TRANSFORMER_FACTORY.newTransformer(
          new StreamSource(
              LGraphVisualizer.class
                  .getClassLoader()
                  .getResourceAsStream("loom/alt/xgraph/LGraphToDot.xsl")));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  Map<String, String> buildIdToAliasMap(LGraph graph) {
    Map<String, String> longSyms = new HashMap<>();
    for (var tnode : graph.listNodes()) {
      var id = tnode.getId();
      var uuid = tnode.getUUID();
      try {
        var digest =
            MessageDigest.getInstance("MD5")
                .digest(uuid.toString().getBytes(StandardCharsets.UTF_8));
        longSyms.put(id, HexFormat.of().formatHex(digest).toUpperCase(Locale.ROOT));
      } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
      }
    }

    for (int i = minPrefixLength; i <= 32; i++) {
      Map<String, String> shortSyms = new HashMap<>();
      for (var entry : longSyms.entrySet()) {
        shortSyms.put(entry.getKey(), entry.getValue().substring(0, i));
      }
      if (shortSyms.size() == longSyms.size()) {
        return shortSyms;
      }
    }

    return longSyms;
  }

  Document buildNodeAliases(LGraph graph) {
    var doc = XGraphUtils.DOCUMENT_BUILDER.newDocument();
    var root = doc.createElement("NodeAliases");
    doc.appendChild(root);

    buildIdToAliasMap(graph)
        .forEach(
            (id, alias) -> {
              var node = doc.createElement("node");
              node.setAttribute("id", id);
              node.setAttribute("alias", alias);
              root.appendChild(node);
            });

    return doc;
  }

  public String toDot(LGraph graph) {
    var transformer = getTransformer();

    if (debugTransform) {
      transformer.setErrorListener(
          new ErrorListener() {
            @Override
            public void warning(TransformerException exception) throws TransformerException {
              System.err.println(exception.getMessageAndLocation());
              throw exception;
            }

            @Override
            public void error(TransformerException exception) throws TransformerException {
              System.err.println(exception.getMessageAndLocation());
              throw exception;
            }

            @Override
            public void fatalError(TransformerException exception) throws TransformerException {
              System.err.println(exception.getMessageAndLocation());
              throw exception;
            }
          });
    }

      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

    transformer.setParameter(
        "NodeAliasesURI", XGraphUtils.documentToTempFile(buildNodeAliases(graph)).toURI());

    try {
      var result = new javax.xml.transform.stream.StreamResult(new java.io.StringWriter());
      transformer.transform(new javax.xml.transform.dom.DOMSource(graph.getDoc()), result);
      return result.getWriter().toString();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
