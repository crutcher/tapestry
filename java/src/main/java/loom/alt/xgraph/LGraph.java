package loom.alt.xgraph;

import lombok.Data;
import loom.common.w3c.NodeListList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.transform.dom.DOMSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Data
public class LGraph {
  @Data
  public class NodeHandle {
    private final Node node;

    public NodeHandle(Node node) {
      this.node = node;
    }

    public String getId() {
      return node.getAttributes().getNamedItem("id").getNodeValue();
    }

    public UUID getUUID() {
      return UUID.fromString(getId());
    }

    public LGraph getGraph() {
      return LGraph.this;
    }
  }

  public static LGraph from(Document doc) {
    var graph = new LGraph(doc);
    graph.validate();
    return graph;
  }

  public static LGraph from(InputStream is) {
    return from(XGraphUtils.parse(is));
  }

  public static LGraph from(String content) {
    return from(content.getBytes(StandardCharsets.UTF_8));
  }

  public static LGraph from(byte[] content) {
    return from(new ByteArrayInputStream(content));
  }

  public static LGraph from(File file) {
    try {
      return from(new FileInputStream(file));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static LGraph create() {
    var doc = XGraphUtils.documentBuilder.newDocument();
    doc.appendChild(doc.createElementNS(XGraphUtils.EG_SCHEMA_URI, "loom:graph"));

    return new LGraph(doc);
  }

  private Document doc;

  public LGraph(Document doc) {
    doc.normalizeDocument();
    this.doc = doc;
  }

  @Override
  public LGraph clone() {
    return new LGraph((Document) doc.cloneNode(true));
  }

  public void validate() {
    try {
      var val = XGraphUtils.getLoomSchema(doc).newValidator();
      val.validate(new DOMSource(doc));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private List<Node> docNodes() {
    var result = new ArrayList<Node>();
    for (var n : NodeListList.of(doc.getDocumentElement().getChildNodes())) {
      if (n.getNodeType() == Node.ELEMENT_NODE && n.getAttributes().getNamedItem("id") != null) {
        result.add(n);
      }
    }
    return result;
  }

  public List<NodeHandle> listNodes() {
    return docNodes().stream().map(NodeHandle::new).toList();
  }

  public NodeHandle getNode(String id) {
    for (var n : listNodes()) {
      if (n.getId().equals(id)) return n;
    }
    throw new NoSuchElementException(id);
  }
}
