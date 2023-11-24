package loom.graph;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.experimental.Delegate;
import loom.common.HasToJsonString;
import loom.common.LookupError;

import javax.annotation.CheckReturnValue;
import java.util.UUID;
import java.util.stream.Stream;

/** XGraph DOM API. */
public class LoomGraph implements HasToJsonString {
  /** XGraph Node DOM API. */
  public class NodeDom {
    @Delegate @Getter public LoomDoc.NodeDoc doc;

    public NodeDom(LoomDoc.NodeDoc nodeDoc) {
      this.doc = nodeDoc;
    }

    public LoomGraph getGraph() {
      return LoomGraph.this;
    }

    public String jpath() {
      return "$.nodes[?(@.id=='%s')]".formatted(doc.getId());
    }
  }

  @JsonIgnore @Getter private final LoomGraphEnv env;
  @JsonValue @Getter private final LoomDoc doc;

  public LoomGraph(LoomDoc doc, LoomGraphEnv env) {
    this.doc = doc;
    this.env = env;
  }

  @CheckReturnValue
  public LoomGraph deepCopy() {
    return new LoomGraph(doc.deepCopy(), env);
  }

  public void validate() {
    env.validateGraph(this);
  }

  /**
   * Does this graph contain a node with the given ID?
   *
   * @param id the ID to check.
   * @return true if the graph contains a node with the given ID.
   */
  public boolean hasNode(UUID id) {
    return doc.hasNode(id);
  }

  /**
   * Does this graph contain a node with the given ID?
   *
   * @param id the ID to check.
   * @return true if the graph contains a node with the given ID.
   */
  public boolean hasNode(String id) {
    return hasNode(UUID.fromString(id));
  }

  /**
   * Get the node with the given ID.
   *
   * @param id the ID of the node to get.
   * @return the node.
   * @throws LookupError if the node does not exist.
   */
  public NodeDom assertNode(UUID id) {
    return new NodeDom(doc.assertNode(id));
  }

  /**
   * Get the node with the given ID.
   *
   * @param id the ID of the node to get.
   * @return the node.
   * @throws LookupError if the node does not exist.
   */
  public NodeDom assertNode(String id) {
    return assertNode(UUID.fromString(id));
  }

  /** Generate a {@code Stream<Node>} of the nodes in the graph. */
  public Stream<NodeDom> nodeStream() {
    return doc.getNodes().values().stream().map(NodeDom::new);
  }

  public Iterable<NodeDom> nodes() {
    return () -> nodeStream().iterator();
  }
}
