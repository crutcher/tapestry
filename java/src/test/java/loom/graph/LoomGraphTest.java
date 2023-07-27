package loom.graph;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.ArrayList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;
import loom.testing.CommonAssertions;
import loom.zspace.ZPoint;
import org.junit.Test;

public class LoomGraphTest implements CommonAssertions {
  @Test
  public void testNodeBuilder() {
    LoomGraph.Node node =
        LoomGraph.Node.builder()
            .type(LoomBuiltinNS.TENSOR)
            .attr(LoomBuiltinNS.SHAPE, new ZPoint(1, 2, 3))
            .attr(LoomBuiltinNS.DTYPE, "float32")
            .build();

    assertThat(node.getId()).isInstanceOf(UUID.class);
    assertThat(node.getType()).isEqualTo(LoomBuiltinNS.TENSOR);
    assertThat(node.getAttrs())
        .containsExactlyInAnyOrderEntriesOf(
            Map.of(
                LoomBuiltinNS.SHAPE,
                JsonNodeFactory.instance.arrayNode().add(1).add(2).add(3),
                LoomBuiltinNS.DTYPE,
                JsonNodeFactory.instance.textNode("float32")));
  }

  @Test
  public void testNodeEquality() {
    UUID id = UUID.randomUUID();

    var a =
        LoomGraph.Node.builder()
            .id(id)
            .type(LoomBuiltinNS.TENSOR)
            .attr(LoomBuiltinNS.SHAPE, new ZPoint(1, 2, 3))
            .attr(LoomBuiltinNS.DTYPE, "float32")
            .build();

    var aDup =
        LoomGraph.Node.builder()
            .id(id)
            .type(LoomBuiltinNS.TENSOR)
            .attr(LoomBuiltinNS.SHAPE, new ZPoint(1, 2, 3))
            .attr(LoomBuiltinNS.DTYPE, "float32")
            .build();

    var b = LoomGraph.Node.builder().id(id).type(LoomBuiltinNS.TENSOR).build();

    assertThat(a).isEqualTo(aDup).hasSameHashCodeAs(aDup);
    assertThat(a).isNotEqualTo(b).doesNotHaveSameHashCodeAs(b);
  }

  @Test
  public void testNodeIteratorAndStream() {
    LoomGraph.Node node =
        LoomGraph.Node.builder()
            .type(LoomBuiltinNS.TENSOR)
            .attr(LoomBuiltinNS.SHAPE, new ZPoint(1, 2, 3))
            .attr(LoomBuiltinNS.DTYPE, "float32")
            .build();

    var items = new ArrayList<Map.Entry<NSName, JsonNode>>();
    for (var entry : node.attrs()) {
      items.add(entry);
    }
    assertThat(items)
        .contains(
            Map.entry(
                LoomBuiltinNS.SHAPE, JsonNodeFactory.instance.arrayNode().add(1).add(2).add(3)),
            Map.entry(LoomBuiltinNS.DTYPE, JsonNodeFactory.instance.textNode("float32")));

    assertThat(node.attrStream().collect(Collectors.toList()))
        .contains(
            Map.entry(
                LoomBuiltinNS.SHAPE, JsonNodeFactory.instance.arrayNode().add(1).add(2).add(3)),
            Map.entry(LoomBuiltinNS.DTYPE, JsonNodeFactory.instance.textNode("float32")));
  }

  @Test
  public void testNodeGetAttr() {
    LoomGraph.Node node =
        LoomGraph.Node.builder()
            .type(LoomBuiltinNS.TENSOR)
            .attr(LoomBuiltinNS.SHAPE, new ZPoint(1, 2, 3))
            .attr(LoomBuiltinNS.DTYPE, "float32")
            .build();

    assertThat(node.hasAttr(LoomBuiltinNS.SHAPE)).isTrue();
    assertThat(node.hasAttr(LoomBuiltinNS.INPUTS)).isFalse();

    assertThat(node.getAttr(LoomBuiltinNS.SHAPE, ZPoint.class)).isEqualTo(new ZPoint(1, 2, 3));
    assertThat(node.getAttr(LoomBuiltinNS.DTYPE, String.class)).isEqualTo("float32");

    assertThat(node.getAttrTree(LoomBuiltinNS.DTYPE).asText()).isEqualTo("float32");
    assertThat(node.getAttrString(LoomBuiltinNS.DTYPE)).isEqualTo("\"float32\"");
  }

  @Test
  public void testNodeAttrsImmutableMap() {
    LoomGraph.Node node =
        LoomGraph.Node.builder()
            .type(LoomBuiltinNS.TENSOR)
            .attr(LoomBuiltinNS.SHAPE, new ZPoint(1, 2, 3))
            .attr(LoomBuiltinNS.DTYPE, "float32")
            .build();

    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(
            () ->
                node.getAttrs()
                    .put(LoomBuiltinNS.EXTERNAL, JsonNodeFactory.instance.booleanNode(true)));
  }

  @Test
  public void testNodeToBuilder() {
    LoomGraph.Node node =
        LoomGraph.Node.builder()
            .type(LoomBuiltinNS.TENSOR)
            .attr(LoomBuiltinNS.SHAPE, new ZPoint(1, 2, 3))
            .attr(LoomBuiltinNS.DTYPE, "float32")
            .build();

    var newNode =
        node.toBuilder().removeAttr(LoomBuiltinNS.SHAPE).attr(LoomBuiltinNS.DTYPE, "int32").build();

    assertThat(newNode.getAttr(LoomBuiltinNS.DTYPE, String.class)).isEqualTo("int32");

    assertThat(newNode.getAttrs())
        .containsExactlyInAnyOrderEntriesOf(
            Map.of(LoomBuiltinNS.DTYPE, JsonNodeFactory.instance.textNode("int32")));
  }

  @Test
  public void testNodeJson() {
    LoomGraph.Node node =
        LoomGraph.Node.builder()
            .type(LoomBuiltinNS.TENSOR)
            .attr(LoomBuiltinNS.SHAPE, new ZPoint(1, 2, 3))
            .attr(LoomBuiltinNS.DTYPE, "float32")
            .build();

    assertJsonEquals(node, node.toString());
    assertJsonEquals(node, node.toJsonString());
    assertJsonEquals(node, node.toPrettyJsonString());

    assertJsonEquals(
        node,
        """
            {
              "id": "%s",
              "type": "%s",
              "attrs": {
                "%s": [1, 2, 3],
                "%s": "float32"
              }
            }
            """
            .formatted(
                node.getId().toString(),
                LoomBuiltinNS.TENSOR.toString(),
                LoomBuiltinNS.SHAPE.toString(),
                LoomBuiltinNS.DTYPE.toString()));
  }

  @Test
  public void testGraphAddNode() {
    var graph = new LoomGraph();

    var nodeA = LoomGraph.Node.builder().build();
    assertThat(nodeA.getId()).isInstanceOf(UUID.class);
    assertThat(graph.addNode(nodeA)).isSameAs(nodeA);

    var nodeB = graph.addNode(LoomGraph.Node.builder());
    assertThat(nodeB.getId()).isInstanceOf(UUID.class);

    var nodeCId = UUID.randomUUID();
    var nodeC = graph.addNode(LoomGraph.Node.builder().id(nodeCId).build());
    assertThat(nodeC.getId()).isEqualTo(nodeCId);
  }

  @Test
  public void testCopyChild() {
    var graph = new LoomGraph();
    graph.addNode(LoomGraph.Node.builder());
    graph.addNode(LoomGraph.Node.builder());

    assertThat(graph.copy()).isEqualTo(graph);
    var child = graph.newChild();
    assertThat(child.getId()).isNotEqualTo(graph.getId());
    assertThat(child.getParentId()).isEqualTo(graph.getId());
    assertThat(child.getNodeMap()).isEqualTo(graph.getNodeMap());
  }

  @Test
  public void testGraphIteratorStream() {
    var graph = new LoomGraph();
    var a = graph.addNode(LoomGraph.Node.builder());
    var b = graph.addNode(LoomGraph.Node.builder());

    var items = new ArrayList<LoomGraph.Node>();
    for (var node : graph.nodes()) {
      items.add(node);
    }
    assertThat(items).contains(a, b);

    assertThat(graph.nodeStream().collect(Collectors.toList())).contains(a, b);
  }

  @Test
  public void testGraphHasLookupRemoveNode() {
    var graph = new LoomGraph();
    var nodeA = graph.addNode(LoomGraph.Node.builder());

    assertThat(graph.hasNode(nodeA.getId())).isTrue();
    assertThat(graph.lookupNode(nodeA.getId())).isSameAs(nodeA);

    graph.removeNode(nodeA.getId());

    assertThat(graph.hasNode(nodeA.getId())).isFalse();
    assertThatExceptionOfType(NoSuchElementException.class)
        .isThrownBy(() -> graph.lookupNode(nodeA.getId()));
  }

  @Test
  public void testGraphJson() {
    var graph = new LoomGraph();

    var a =
        graph.addNode(
            LoomGraph.Node.builder()
                .type(LoomBuiltinNS.TENSOR)
                .attr(LoomBuiltinNS.SHAPE, new ZPoint(1, 2, 3))
                .attr(LoomBuiltinNS.DTYPE, "float32"));

    var b =
        graph.addNode(
            LoomGraph.Node.builder()
                .type(LoomBuiltinNS.TENSOR)
                .attr(LoomBuiltinNS.SHAPE, new ZPoint(10, 3))
                .attr(LoomBuiltinNS.DTYPE, "int"));

    assertJsonEquals(graph, graph.toString());
    assertJsonEquals(graph, graph.toJsonString());
    assertJsonEquals(graph, graph.toPrettyJsonString());

    assertJsonEquals(
        graph,
        """
          {
            "id": "%s",
            "nodes": [
              %s,
              %s
            ]
          }
          """
            .formatted(graph.getId().toString(), a.toJsonString(), b.toJsonString()));
  }
}
