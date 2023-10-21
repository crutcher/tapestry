package loom.graph;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.Data;
import loom.common.xml.w3c.NodeListList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

@Data
public final class ExpressionGraph {

  public static String uuidToNodeId(UUID uuid) {
    return "node-" + uuid.toString();
  }

  public static UUID nodeIdToUuid(String nodeId) {
    if (!nodeId.startsWith("node-")) {
      throw new IllegalArgumentException("Invalid node id: " + nodeId);
    }
    return UUID.fromString(nodeId.substring(5));
  }

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
      return nodeIdToUuid(getId());
    }

    public ExpressionGraph getGraph() {
      return ExpressionGraph.this;
    }
  }

  public static ExpressionGraph from(Document doc) {
    return new ExpressionGraph(doc);
  }

  public static ExpressionGraph from(InputStream is) {
    return from(LoomXml.parse(is));
  }

  public static ExpressionGraph from(String content) {
    return from(content.getBytes(StandardCharsets.UTF_8));
  }

  public static ExpressionGraph from(byte[] content) {
    return from(new ByteArrayInputStream(content));
  }

  public static ExpressionGraph from(File file) {
    try {
      return from(new FileInputStream(file));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static ExpressionGraph create() {
    var doc = LoomXml.DOCUMENT_BUILDER.newDocument();
    doc.appendChild(doc.createElementNS(LoomXml.EG_CORE_SCHEMA_URI, "loom:graph"));

    return new ExpressionGraph(doc);
  }

  private Document doc;

  public ExpressionGraph(Document doc) {
    this(doc, true);
  }

  ExpressionGraph(Document doc, boolean validate) {
    this.doc = doc;
    if (validate) validate();
  }

  @Override
  public ExpressionGraph clone() {
    return new ExpressionGraph((Document) doc.cloneNode(true), false);
  }

  public LoomXml.XQuery xquery() {
    return LoomXml.xquery(doc);
  }

  public void validate() {
    ExpressionGraphValidator.instance.validate(doc);
  }

  private List<Node> docNodes() {
    var result = new ArrayList<Node>();
    for (var n :
        NodeListList.of(
            doc.getElementsByTagNameNS(LoomXml.EG_CORE_SCHEMA_URI, "nodes")
                .item(0)
                .getChildNodes())) {
      if (n.getNodeType() == Node.ELEMENT_NODE && n.getAttributes().getNamedItem("id") != null) {
        result.add(n);
      }
    }
    return result;
  }

  public List<NodeHandle> listNodes() {
    return docNodes().stream().map(NodeHandle::new).toList();
  }

  public NodeHandle getNode(UUID uuid) {
    return getNode(uuidToNodeId(uuid));
  }

  public NodeHandle getNode(String id) {
    for (var n : docNodes()) {
      if (n.getAttributes().getNamedItem("id").getNodeValue().equals(id)) {
        return new NodeHandle(n);
      }
    }
    throw new NoSuchElementException(id);
  }
}
