package loom.alt.xgraph;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.Renderer;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import lombok.Builder;
import org.w3c.dom.Document;

@Builder
public class LGraphVisualizer {
  LGraph graph;

  @Builder.Default boolean debugTransform = true;

  @Builder.Default private Integer minPrefixLength = 3;

  @Builder.Default private Float scale = 2.0f;

  String dot;

  @Builder
  public LGraphVisualizer(
      LGraph graph, boolean debugTransform, Integer minPrefixLength, Float scale, String dot) {
    this.graph = graph;
    this.debugTransform = debugTransform;
    this.minPrefixLength = minPrefixLength;
    this.scale = scale;
    this.dot = dot;

    toDot();
  }

  public void clear() {
    dot = null;
  }

  public BufferedImage toImage() {
    return toRenderer(Format.PNG).toImage();
  }

  public Renderer toRenderer(Format format) {
    return Graphviz.fromString(toDot()).scale(scale).render(format);
  }

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

  private Map<String, String> buildIdToAliasMap() {
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

  private Document buildNodeAliases() {
    var doc = XGraphUtils.DOCUMENT_BUILDER.newDocument();
    var root = doc.createElement("NodeAliases");
    doc.appendChild(root);

    buildIdToAliasMap()
        .forEach(
            (id, alias) -> {
              var node = doc.createElement("node");
              node.setAttribute("id", id);
              node.setAttribute("alias", alias);
              root.appendChild(node);
            });

    return doc;
  }

  public String toDot() {
    if (dot != null) {
      return dot;
    }

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

    Document aliasesDoc = buildNodeAliases();
    File paramFile = XGraphUtils.documentToTempFile(aliasesDoc);
    transformer.setParameter("NodeAliasesURI", paramFile.toURI());

    try {
      var result = new javax.xml.transform.stream.StreamResult(new java.io.StringWriter());
      transformer.transform(new javax.xml.transform.dom.DOMSource(graph.getDoc()), result);
      dot = result.getWriter().toString();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return dot;
  }
}
