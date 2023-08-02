package loom.alt.xgraph;

import lombok.AllArgsConstructor;
import lombok.Data;
import loom.common.w3c.NodeListList;
import org.w3c.dom.Document;

import javax.xml.transform.dom.DOMSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Data
@AllArgsConstructor
public class GraphDoc {
  public static GraphDoc from(Document doc) {
    doc.normalizeDocument();

    var graphDoc = new GraphDoc(doc);
    graphDoc.validate();
    return graphDoc;
  }

  public static GraphDoc from(InputStream is) {
    return from(XGraphUtils.parse(is));
  }

  public static GraphDoc from(String content) {
    return from(content.getBytes(StandardCharsets.UTF_8));
  }

  public static GraphDoc from(byte[] content) {
    return from(new ByteArrayInputStream(content));
  }

  public static GraphDoc from(File file) {
    try {
      return from(new FileInputStream(file));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static GraphDoc create() {
    var doc = XGraphUtils.documentBuilder.newDocument();
    var g = doc.createElementNS(XGraphUtils.NS, "graph");
    g.setAttributeNS(XGraphUtils.NS, "id", UUID.randomUUID().toString());

    return new GraphDoc(doc);
  }

  private Document doc;

  @Override
  public GraphDoc clone() {
    return new GraphDoc((Document) doc.cloneNode(true));
  }

  public void validate() {
    try {
      var val = XGraphUtils.getSchema().newValidator();
      val.validate(new DOMSource(doc));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public NodeListList nodes() {
    return new NodeListList(XGraphUtils.graphNodes(doc));
  }
}
