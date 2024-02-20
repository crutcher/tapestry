package org.tensortapestry.graphviz;

import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.tensortapestry.common.text.IndentUtils;

@Data
public class DotGraph {

  public interface EdgeTarget {
    Node getNode();

    String getEdgeAddress();
  }

  @Getter
  @RequiredArgsConstructor
  public abstract class GraphItem {

    @Nonnull
    private final String id;

    @Setter
    private String note;

    @Nonnull
    public DotGraph getGraph() {
      return DotGraph.this;
    }
  }

  @Getter
  public class Node extends GraphItem implements EdgeTarget {

    public class Port implements EdgeTarget {

      private final String port;

      @Nullable private final CompassDir compassDir;

      public Port(@Nonnull String port) {
        this(port, null);
      }

      public Port(@Nonnull String port, @Nullable CompassDir compassDir) {
        this.port = port;
        this.compassDir = compassDir;
      }

      public Port withCompassDir(@Nonnull CompassDir compassPoint) {
        return new Port(port, compassPoint);
      }

      @Override
      public Node getNode() {
        return Node.this;
      }

      @Override
      public String getEdgeAddress() {
        var parts = new ArrayList<String>(3);
        parts.add(Node.this.getEdgeAddress());
        parts.add("\"%s\"".formatted(FormatUtils.escape(port)));
        if (compassDir != null) {
          parts.add(compassDir.toString().toLowerCase(Locale.ROOT));
        }
        return String.join(":", parts);
      }
    }

    @Delegate
    private final GraphvizAttributeMap attributes = new GraphvizAttributeMap();

    public Node(@Nonnull String id) {
      super(id);
    }

    @Override
    public Node getNode() {
      return this;
    }

    @Override
    public String getEdgeAddress() {
      return "\"%s\"".formatted(FormatUtils.escape(getId()));
    }

    @Nonnull
    public Port port(@Nonnull String port) {
      return new Port(port);
    }
  }

  @Getter
  public class Edge extends GraphItem {

    private final EdgeTarget from;
    private final EdgeTarget to;

    @Delegate
    private final GraphvizAttributeMap attributes = new GraphvizAttributeMap();

    public Edge(@Nonnull String id, EdgeTarget from, EdgeTarget to) {
      super(id);
      this.from = from;
      this.to = to;
    }

    @SuppressWarnings("InconsistentOverloads")
    public Edge(EdgeTarget from, EdgeTarget to) {
      this(UUID.randomUUID().toString(), from, to);
    }
  }

  @Getter
  public class SubGraph extends GraphItem {

    private final GraphvizAttributeMap attributes = new GraphvizAttributeMap();
    private final GraphvizAttributeMap edgeDefaults = new GraphvizAttributeMap();
    private final GraphvizAttributeMap nodeDefaults = new GraphvizAttributeMap();
    private final List<Node> nodes = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private final List<SubGraph> subGraphs = new ArrayList<>();

    public SubGraph(@Nonnull String id) {
      super(id);
    }

    @Nullable public GraphItem lookup(@Nonnull String id) {
      if (id.equals(getId())) {
        return this;
      }
      for (var node : nodes) {
        if (node.getId().equals(id)) {
          return node;
        }
      }
      for (var edge : edges) {
        if (edge.getId().equals(id)) {
          return edge;
        }
      }
      for (var subGraph : subGraphs) {
        var item = subGraph.lookup(id);
        if (item != null) {
          return item;
        }
      }
      return null;
    }

    @Nonnull
    public <T extends GraphItem> T assertLookup(@Nonnull String id, @Nonnull Class<T> type) {
      var item = lookup(id);
      if (item == null) {
        throw new IllegalArgumentException("Id <%s> not found in graph".formatted(id));
      }
      if (!type.isInstance(item)) {
        throw new IllegalArgumentException(
          "Id <%s> found in graph, but is not a %s: %s".formatted(id, type.getSimpleName(), item)
        );
      }
      return type.cast(item);
    }

    @Nonnull
    public Node createNode(@Nonnull String id) {
      checkIdConflict(id);
      var node = new Node(id);
      nodes.add(node);
      return node;
    }

    @Nonnull
    public Node createNodeStub(@Nonnull String id) {
      var node = new Node(id);
      nodes.add(node);
      return node;
    }

    @Nonnull
    public Edge createEdge(@Nonnull EdgeTarget from, @Nonnull EdgeTarget to) {
      var edge = new Edge(from, to);
      edges.add(edge);
      return edge;
    }

    @Nonnull
    public SubGraph createSubGraph(@Nonnull String id) {
      checkIdConflict(id);
      var subGraph = new SubGraph(id);
      subGraphs.add(subGraph);
      return subGraph;
    }

    @Nonnull
    public Cluster createCluster(@Nonnull String id) {
      checkIdConflict(id);
      var cluster = new Cluster(id);
      subGraphs.add(cluster);
      return cluster;
    }

    protected String format(int indent) {
      var prefix = BASE_INDENT.repeat(indent);
      return (
        prefix +
        "subgraph \"%s\" {\n".formatted(subgraphId()) +
        formatBody(indent + 1) +
        prefix +
        "}\n"
      );
    }

    protected String subgraphId() {
      return getId();
    }

    public String formatBody(int indent) {
      var prefix = BASE_INDENT.repeat(indent);
      var sb = new StringBuilder();

      if (!attributes.isEmpty()) {
        sb.append(FormatUtils.formatContextAttributes(attributes, indent));
      }

      if (!nodeDefaults.isEmpty()) {
        sb
          .append(prefix)
          .append("node")
          .append(FormatUtils.formatItemAttributes(nodeDefaults, indent))
          .append(";\n");
      }
      if (!edgeDefaults.isEmpty()) {
        sb
          .append(prefix)
          .append("edge")
          .append(FormatUtils.formatItemAttributes(edgeDefaults, indent))
          .append(";\n");
      }

      if (!nodes.isEmpty()) {
        sb.append("\n");
        for (var item : nodes) {
          if (item.getNote() != null) {
            sb.append(IndentUtils.reindent(prefix + "// ", item.getNote())).append("\n");
          }

          sb
            .append(prefix)
            .append("\"%s\"".formatted(item.getId()))
            .append(FormatUtils.formatItemAttributes(item.attributes, indent))
            .append(";\n");
        }
      }

      if (!subGraphs.isEmpty()) {
        sb.append("\n");
        for (var item : subGraphs) {
          if (item.getNote() != null) {
            sb.append(IndentUtils.reindent(prefix + "// ", item.getNote())).append("\n");
          }

          sb.append(item.format(indent));
        }
      }

      if (!edges.isEmpty()) {
        sb.append("\n");
        for (var item : edges) {
          if (item.getNote() != null) {
            sb.append(IndentUtils.reindent(prefix + "// ", item.getNote())).append("\n");
          }

          String from = item.getFrom().getEdgeAddress();
          String to = item.getTo().getEdgeAddress();
          sb
            .append(prefix)
            .append("%s -> %s".formatted(from, to))
            .append(FormatUtils.formatItemAttributes(item.attributes, indent))
            .append(";\n");
        }
      }

      return sb.toString();
    }
  }

  @Getter
  public class Cluster extends SubGraph {

    public Cluster(@Nonnull String id) {
      super(id);
    }

    @Override
    protected String subgraphId() {
      return "cluster_" + getId();
    }
  }

  public static final String BASE_INDENT = "  ";

  @Delegate
  private final SubGraph root;

  private static final List<List<String>> rankSets = new ArrayList<>();

  public DotGraph(@Nonnull String id) {
    root = new SubGraph(id);
  }

  public DotGraph() {
    this("G");
  }

  public void checkIdConflict(@Nonnull String id) {
    var obj = lookup(id);
    if (obj != null) {
      throw new IllegalArgumentException("Id <%s> already exists in graph: %s".formatted(id, obj));
    }
  }

  public void sameRank(@Nonnull GraphItem... items) {
    sameRank(Arrays.asList(items));
  }

  public void sameRank(@Nonnull List<? extends GraphItem> items) {
    rankSets.add(items.stream().map(GraphItem::getId).toList());
  }

  @Override
  public String toString() {
    var sb = new StringBuilder();
    sb.append("digraph \"%s\" {\n".formatted(getId()));
    sb.append(root.formatBody(1));
    if (!rankSets.isEmpty()) {
      for (var rankSet : rankSets) {
        sb.append("  { rank=same; ");
        sb.append(
          rankSet
            .stream()
            .map(id -> "\"%s\"".formatted(FormatUtils.escape(id)))
            .collect(Collectors.joining("; "))
        );
        sb.append(" }\n");
      }
    }
    sb.append("}\n");
    return sb.toString();
  }
}
