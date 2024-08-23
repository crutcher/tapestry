package org.tensortapestry.loom.graph.export.graphviz;

import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import lombok.*;
import lombok.experimental.Delegate;

@Data
public class DotBuilder {

  public static final List<String> GRAPH_ATTRIBUTES = List.of(
    "_background",
    "bb",
    "beautify",
    "bgcolor",
    "center",
    "charset",
    "class",
    "clusterrank",
    "colorscheme",
    "comment",
    "compound",
    "concentrate",
    "Damping",
    "defaultdist",
    "dim",
    "dimen",
    "diredgeconstraints",
    "dpi",
    "epsilon",
    "esep",
    "fontcolor",
    "fontname",
    "fontnames",
    "fontpath",
    "fontsize",
    "forcelabels",
    "gradientangle",
    "href",
    "id",
    "imagepath",
    "inputscale",
    "K",
    "label",
    "label_scheme",
    "labeljust",
    "labelloc",
    "landscape",
    "layerlistsep",
    "layers",
    "layerselect",
    "layersep",
    "layout",
    "levels",
    "levelsgap",
    "lheight",
    "linelength",
    "lp",
    "lwidth",
    "margin",
    "maxiter",
    "mclimit",
    "mindist",
    "mode",
    "model",
    "newrank",
    "nodesep",
    "nojustify",
    "normalize",
    "notranslate",
    "nslimit",
    "nslimit1",
    "oneblock",
    "ordering",
    "orientation",
    "outputorder",
    "overlap",
    "overlap_scaling",
    "overlap_shrink",
    "pack",
    "packmode",
    "pad",
    "page",
    "pagedir",
    "quadtree",
    "quantum",
    "rankdir",
    "ranksep",
    "ratio",
    "remincross",
    "repulsiveforce",
    "resolution",
    "root",
    "rotate",
    "rotation",
    "scale",
    "searchsize",
    "sep",
    "showboxes",
    "size",
    "smoothing",
    "sortv",
    "splines",
    "start",
    "style",
    "stylesheet",
    "target",
    "TBbalance",
    "tooltip",
    "truecolor",
    "URL",
    "viewport",
    "voro_margin",
    "xdotversion"
  );

  public static final List<String> EDGE_ATTRIBUTES = List.of(
    "arrowhead",
    "arrowsize",
    "arrowtail",
    "class",
    "color",
    "colorscheme",
    "comment",
    "constraint",
    "decorate",
    "dir",
    "edgehref",
    "edgetarget",
    "edgetooltip",
    "edgeURL",
    "fillcolor",
    "fontcolor",
    "fontname",
    "fontsize",
    "head_lp",
    "headclip",
    "headhref",
    "headlabel",
    "headport",
    "headtarget",
    "headtooltip",
    "headURL",
    "href",
    "id",
    "label",
    "labelangle",
    "labeldistance",
    "labelfloat",
    "labelfontcolor",
    "labelfontname",
    "labelfontsize",
    "labelhref",
    "labeltarget",
    "labeltooltip",
    "labelURL",
    "layer",
    "len",
    "lhead",
    "lp",
    "ltail",
    "minlen",
    "nojustify",
    "penwidth",
    "pos",
    "samehead",
    "sametail",
    "showboxes",
    "style",
    "tail_lp",
    "tailclip",
    "tailhref",
    "taillabel",
    "tailport",
    "tailtarget",
    "tailtooltip",
    "tailURL",
    "target",
    "tooltip",
    "URL",
    "weight",
    "xlabel",
    "xlp"
  );

  public static final List<String> CLUSTER_ATTRIBUTES = List.of(
    "area",
    "bb",
    "bgcolor",
    "class",
    "cluster",
    "color",
    "colorscheme",
    "fillcolor",
    "fontcolor",
    "fontname",
    "fontsize",
    "gradientangle",
    "href",
    "id",
    "K",
    "label",
    "labeljust",
    "labelloc",
    "layer",
    "lheight",
    "lp",
    "lwidth",
    "margin",
    "nojustify",
    "pencolor",
    "penwidth",
    "peripheries",
    "sortv",
    "style",
    "target",
    "tooltip",
    "URL"
  );

  public static final List<String> NODE_ATTRIBUTES = List.of(
    "area",
    "class",
    "color",
    "colorscheme",
    "comment",
    "distortion",
    "fillcolor",
    "fixedsize",
    "fontcolor",
    "fontname",
    "fontsize",
    "gradientangle",
    "group",
    "height",
    "href",
    "id",
    "image",
    "imagepos",
    "imagescale",
    "label",
    "labelloc",
    "layer",
    "margin",
    "nojustify",
    "ordering",
    "orientation",
    "penwidth",
    "peripheries",
    "pin",
    "pos",
    "rects",
    "regular",
    "root",
    "samplepoints",
    "shape",
    "shapefile",
    "showboxes",
    "sides",
    "skew",
    "sortv",
    "style",
    "target",
    "tooltip",
    "URL",
    "vertices",
    "width",
    "xlabel",
    "xlp",
    "z"
  );

  public static void checkKey(
    String category,
    Collection<String> legalKeys,
    String key,
    String value
  ) {
    if (!legalKeys.contains(key)) {
      throw new IllegalArgumentException(
        "Illegal %s attribute: %s = \"%s\"".formatted(category, key, value)
      );
    }
  }

  public static void validateGraphAttribute(String key, String value) {
    checkKey("graph", GRAPH_ATTRIBUTES, key, value);
  }

  public static void validateEdgeAttribute(String key, String value) {
    checkKey("edge", EDGE_ATTRIBUTES, key, value);
  }

  public static void validateClusterAttribute(String key, String value) {
    checkKey("cluster", CLUSTER_ATTRIBUTES, key, value);
  }

  public static void validateNodeAttribute(String key, String value) {
    checkKey("node", NODE_ATTRIBUTES, key, value);
  }

  @Data
  public static final class Attributes {

    private final Map<String, String> attrs = new HashMap<>();

    @Nullable private final BiConsumer<String, String> validator;

    public Attributes() {
      this(null);
    }

    public Attributes(@Nullable BiConsumer<String, String> validator) {
      this.validator = validator;
    }

    public void validate(String key, String value) {
      if (validator != null) validator.accept(key, value);
    }

    public void set(String key, Object value) {
      var stringValue = value.toString();
      attrs.put(key, stringValue);
    }

    public void setAll(Map<String, Object> attributes) {
      attributes.forEach(this::set);
    }

    public void setAll(Attributes attributes) {
      attrs.putAll(attributes.attrs);
    }

    public void setAll(Collection<Map.Entry<String, Object>> attributes) {
      attributes.forEach(e -> set(e.getKey(), e.getValue()));
    }

    public String get(String key) {
      return attrs.get(key);
    }
  }

  @Data
  @RequiredArgsConstructor
  public abstract static class Item {

    private final String id;
  }

  @Getter
  @EqualsAndHashCode(callSuper = true)
  public static final class Node extends Item {

    @Delegate
    private final Attributes attributes = new Attributes(DotBuilder::validateNodeAttribute);

    public Node(String id) {
      super(id);
    }

    public String render() {
      return "\"%s\" [%s];".formatted(getId(), renderAttributes(attributes, ", "));
    }

    public String stub() {
      return "\"%s\";".formatted(getId());
    }
  }

  @Getter
  @EqualsAndHashCode(callSuper = true)
  @SuppressWarnings("InconsistentOverloads")
  public static final class Edge extends Item {

    private final String source;
    private final String target;

    @Delegate
    private final Attributes attributes = new Attributes(DotBuilder::validateEdgeAttribute);

    public Edge(String source, String target) {
      super(UUID.randomUUID().toString());
      this.source = source;
      this.target = target;
    }

    public Edge(String id, String source, String target) {
      super(id);
      this.source = source;
      this.target = target;
    }

    public String render() {
      return "\"%s\" -> \"%s\" [%s];".formatted(
          getSource(),
          getTarget(),
          renderAttributes(attributes, ", ")
        );
    }
  }

  @Getter
  @EqualsAndHashCode(callSuper = true)
  public static final class Cluster extends Item {

    @Delegate
    private final Attributes attributes = new Attributes(DotBuilder::validateClusterAttribute);

    private final List<String> nodeIds = new ArrayList<>();
    private final List<Cluster> clusters = new ArrayList<>();

    public Cluster(String id) {
      super(id);
    }

    public void addNode(Node node) {
      nodeIds.add(node.getId());
    }

    public Cluster newCluster(String id) {
      var cluster = new Cluster(id);
      clusters.add(cluster);
      return cluster;
    }

    public String render() {
      var sb = new StringBuilder("subgraph cluster_%s {\n".formatted(getId()));
      sb.append(renderAttributes(attributes, ";\n"));
      for (var node : nodeIds) {
        sb.append("\"%s\"".formatted(node)).append("\n");
      }
      for (var cluster : clusters) {
        sb.append(cluster.render()).append("\n");
      }
      sb.append("}");
      return sb.toString();
    }

    public Cluster lookup(String id) {
      if (getId().equals(id)) return this;
      for (var cluster : clusters) {
        if (cluster.getId().equals(id)) return cluster;
        var result = cluster.lookup(id);
        if (result != null) return result;
      }
      return null;
    }
  }

  private final Attributes graphAttributes = new Attributes(DotBuilder::validateGraphAttribute);
  private final Attributes edgeAttributes = new Attributes(DotBuilder::validateEdgeAttribute);
  private final Attributes nodeAttributes = new Attributes(DotBuilder::validateNodeAttribute);

  private final Map<String, Node> nodes = new HashMap<>();
  private final Map<String, Edge> edges = new HashMap<>();
  private final Map<String, Cluster> clusters = new HashMap<>();

  public Item lookup(String id) {
    if (nodes.containsKey(id)) return nodes.get(id);
    if (edges.containsKey(id)) return edges.get(id);
    for (var cluster : clusters.values()) {
      var result = cluster.lookup(id);
      if (result != null) return result;
    }
    return null;
  }

  public void checkId(String id) {
    var existing = lookup(id);
    if (existing != null) {
      throw new IllegalArgumentException("Duplicate id: %s => %s".formatted(id, existing));
    }
  }

  public Cluster newCluster(String id) {
    checkId(id);
    var cluster = new Cluster(id);
    clusters.put(id, cluster);
    return cluster;
  }

  public Node newNode(String id) {
    checkId(id);
    var node = new Node(id);
    nodes.put(id, node);
    return node;
  }

  public Edge newEdge(String source, String target) {
    var edge = new Edge(source, target);
    edges.put(edge.getId(), edge);
    return edge;
  }

  @SuppressWarnings("InconsistentOverloads")
  public Edge newEdge(String id, String source, String target) {
    checkId(id);
    var edge = new Edge(id, source, target);
    edges.put(id, edge);
    return edge;
  }

  private static String renderAttribute(String key, String value) {
    if (key.equals("label") || key.equals("xlabel")) {
      if (value.startsWith("<") && value.endsWith(">")) {
        // HTML label
        return "%s=%s".formatted(key, value);
      }
    }
    return "%s=\"%s\"".formatted(key, value);
  }

  private static String renderAttributes(Attributes attributes, String delim) {
    var sb = new StringBuilder();
    for (var entry : attributes.getAttrs().entrySet()) {
      sb.append(renderAttribute(entry.getKey(), entry.getValue())).append(delim);
    }
    return sb.toString();
  }

  public String toDigraph() {
    var sb = new StringBuilder("digraph {\n");
    renderAttributes(graphAttributes, ";\n");

    for (var node : nodes.values()) {
      sb.append(node.render()).append("\n");
    }
    for (var cluster : clusters.values()) {
      sb.append(cluster.render()).append("\n");
    }
    for (var edge : edges.values()) {
      sb.append(edge.render()).append("\n");
    }

    sb.append("}");
    return sb.toString();
  }
}
