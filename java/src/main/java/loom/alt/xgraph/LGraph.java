package loom.alt.xgraph;

import lombok.Data;
import loom.common.w3c.NodeListList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

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
public final class LGraph {

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

    public LGraph getGraph() {
      return LGraph.this;
    }
  }

  public static LGraph from(Document doc) {
    return new LGraph(doc);
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
    var doc = XGraphUtils.DOCUMENT_BUILDER.newDocument();
    doc.appendChild(doc.createElementNS(XGraphUtils.EG_SCHEMA_URI, "loom:graph"));

    return new LGraph(doc);
  }

  private LoomValidator validator;

  private Document doc;

  public LGraph(Document doc) {
    this(doc, LoomValidator.instance, true);
  }

  LGraph(Document doc, LoomValidator validator, boolean validate) {
    this.doc = doc;
    this.validator = validator;
    if (validate) validate();
  }

  @Override
  public LGraph clone() {
    return new LGraph((Document) doc.cloneNode(true), validator, false);
  }

  public void validate() {
    validator.validate(doc);
  }

  private List<Node> docNodes() {
    var result = new ArrayList<Node>();
    for (var n :
        NodeListList.of(
            doc.getElementsByTagNameNS(XGraphUtils.EG_SCHEMA_URI, "nodes")
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
