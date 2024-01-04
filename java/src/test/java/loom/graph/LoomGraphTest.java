package loom.graph;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import loom.common.json.JsonUtil;
import loom.graph.nodes.GenericNode;
import loom.graph.nodes.NoteNode;
import loom.graph.nodes.TensorNode;
import loom.testing.BaseTestClass;
import loom.zspace.ZPoint;
import org.junit.Test;

public class LoomGraphTest extends BaseTestClass {

  @Test
  public void testNewUnusedNodeId() {
    var env = CommonEnvironments.genericEnvironment();
    var graph = env.newGraph();

    for (int i = 0; i < 10; i++) {
      graph.addNode(
          GenericNode.builder()
              .type("test")
              .body(GenericNode.Body.builder().field("a", 12).build()));
    }

    var nodeId = graph.newUnusedNodeId();
    assertThat(graph.hasNode(nodeId)).isFalse();
  }

  @Test
  public void testAddRemoveNode() {
    var env = CommonEnvironments.genericEnvironment();
    var graph = env.newGraph();

    var node1 =
        graph.addNode(
            GenericNode.builder()
                .label("foo")
                .type("test")
                .body(GenericNode.Body.builder().field("a", 12).build()));

    assertThat(graph.getNode(node1.getId())).isSameAs(node1);
    assertThat(graph.getNode(node1.getId().toString())).isSameAs(node1);

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> graph.addNode(node1))
        .withMessage("Graph already has node with id: %s".formatted(node1.getId()));

    {
      var graph2 = env.newGraph();
      var node2 =
          graph2.addNode(
              GenericNode.builder()
                  .label("foo")
                  .type("test")
                  .body(GenericNode.Body.builder().field("a", 12).build()));

      assertThatExceptionOfType(IllegalArgumentException.class)
          .isThrownBy(() -> graph.addNode(node2))
          .withMessage("Node already belongs to a graph: %s".formatted(node2.getId()));
    }

    var node2 =
        graph.addNode(
            GenericNode.builder()
                .label("bar")
                .type("test")
                .body(GenericNode.Body.builder().field("a", 12).build()));

    var node3 =
        graph.addNode(
            GenericNode.builder()
                .label("qux")
                .type("test")
                .body(GenericNode.Body.builder().field("a", 12).build()));

    graph.removeNode(node1);
    // Should be a no-op.
    graph.removeNode(node1);
    graph.removeNode(node2.getId());
    graph.removeNode(node3.getId().toString());

    assertThat(graph.hasNode(node1.getId())).isFalse();
    assertThat(graph.hasNode(node2.getId())).isFalse();
    assertThat(graph.hasNode(node3.getId())).isFalse();
  }

  @Test
  public void testHasAssertAddNode() {
    var env = CommonEnvironments.genericEnvironment();
    var graph = env.newGraph();

    var nodeIdA = UUID.randomUUID();

    assertThat(graph.hasNode(nodeIdA)).isFalse();
    assertThat(graph.hasNode(nodeIdA.toString())).isFalse();
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> graph.assertNode(nodeIdA));
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> graph.assertNode(nodeIdA.toString()));

    var nodeA =
        graph.addNode(
            GenericNode.builder()
                .id(nodeIdA)
                .type("test")
                .body(GenericNode.Body.builder().field("a", 12).build()));
    assertThat(nodeA)
        .hasFieldOrPropertyWithValue("id", nodeIdA)
        .hasFieldOrPropertyWithValue("type", "test")
        .isInstanceOf(GenericNode.class);

    assertThat(graph.hasNode(nodeIdA)).isTrue();
    assertThat(graph.hasNode(nodeIdA.toString())).isTrue();
    assertThat(graph.assertNode(nodeIdA)).isSameAs(nodeA);
    assertThat(graph.assertNode(nodeIdA.toString())).isSameAs(nodeA);

    // Casting assertions.
    assertThat(graph.assertNode(nodeIdA, "test", GenericNode.class)).isSameAs(nodeA);
    assertThat(graph.assertNode(nodeIdA.toString(), "test", GenericNode.class)).isSameAs(nodeA);
    assertThat(graph.assertNode(nodeIdA, null, GenericNode.class)).isSameAs(nodeA);
    assertThat(graph.assertNode(nodeIdA.toString(), null, GenericNode.class)).isSameAs(nodeA);
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> graph.assertNode(nodeIdA, "foo", GenericNode.class));
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> graph.assertNode(nodeIdA, "test", ExampleNode.class));
  }

  @Test
  public void testGenericSerializer() {
    var source =
        """
        {
           "id": "00000000-0000-0000-0000-000000000000",
           "nodes": [
              {
                "id": "00000000-0000-0000-0000-000000000001",
                "type": "TensorNode",
                "label": "foo",
                "body": {
                  "dtype": "int32",
                  "shape": [2, 3]
                }
              },
              {
                "id": "00000000-0000-0000-0000-000000000002",
                "type": "TensorNode",
                "label": "bar",
                "body": {
                  "dtype": "float32",
                  "shape": [4, 5]
                }
              }
           ]
        }
        """;

    var env = CommonEnvironments.genericEnvironment();
    var graph = env.graphFromJson(source);
    graph.validate();

    assertThat(graph.assertNode("00000000-0000-0000-0000-000000000001"))
        .hasFieldOrPropertyWithValue("label", "foo")
        .isInstanceOf(GenericNode.class);
    assertThat(graph.assertNode("00000000-0000-0000-0000-000000000002"))
        .hasFieldOrPropertyWithValue("label", "bar")
        .isInstanceOf(GenericNode.class);

    assertEquivalentJson(graph.toJsonString(), source);
  }

  @Test
  public void testBoundEnvironment() {
    var source =
        """
            {
               "id": "00000000-0000-0000-0000-000000000000",
               "nodes": [
                  {
                    "id": "00000000-0000-0000-0000-000000000001",
                    "type": "TensorNode",
                    "label": "foo",
                    "body": {
                      "dtype": "int32",
                      "shape": [2, 3]
                    }
                  },
                  {
                    "id": "00000000-0000-0000-0000-000000000002",
                    "type": "TensorNode",
                    "label": "bar",
                    "body": {
                      "dtype": "float32",
                      "shape": [4, 5]
                    }
                  }
               ]
            }
            """;

    var env = LoomEnvironment.builder().build().addNodeTypeClass(TensorNode.TYPE, TensorNode.class);

    var graph = env.graphFromJson(source);
    graph.validate();

    assertThat(graph.assertNode("00000000-0000-0000-0000-000000000001"))
        .hasFieldOrPropertyWithValue("label", "foo")
        .isInstanceOf(TensorNode.class)
        .hasFieldOrPropertyWithValue("dtype", "int32")
        .hasFieldOrPropertyWithValue("shape", ZPoint.of(2, 3));
    assertThat(graph.assertNode("00000000-0000-0000-0000-000000000002"))
        .hasFieldOrPropertyWithValue("label", "bar")
        .isInstanceOf(TensorNode.class)
        .hasFieldOrPropertyWithValue("dtype", "float32")
        .hasFieldOrPropertyWithValue("shape", ZPoint.of(4, 5));

    assertEquivalentJson(graph.toJsonString(), source);
  }

  @Test
  public void testGenericNodeDelegation() {
    var source =
        """
              {
                "id": "00000000-0000-0000-0000-000000000000",
                "type": "TreeNode",
                "label": "foo",
                "body": {
                  "dtype": "int32",
                  "shape": [2, 3]
                }
              }
              """;

    var node = (GenericNode) JsonUtil.fromJson(source, GenericNode.class);

    assertThat(node.getId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    assertThat(node.getLabel()).isEqualTo("foo");

    assertThat(node.getFields()).contains(entry("dtype", "int32"), entry("shape", List.of(2, 3)));

    assertJsonEquals(node.getBody(), node.getBodyAsJson());

    assertEquivalentJson(source, node.toJsonString());

    assertEquivalentJson(
        node.getBodyAsJson(),
        """
                  {
                    "dtype": "int32",
                    "shape": [2, 3]
                  }
                  """);
  }

  @Test
  public void testTensorNodeDelegation() {

    var source =
        """
            {
                "id": "00000000-0000-0000-0000-000000000000",
                "type": "TensorNode",
                "label": "foo",
                "body": {
                  "dtype": "int32",
                  "shape": [2, 3]
                }
            }
            """;

    var node = JsonUtil.fromJson(source, TensorNode.class);

    assertThat(node.getId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    assertThat(node.getLabel()).isEqualTo("foo");

    assertThat(node.getBody().getShape()).isEqualTo(ZPoint.of(2, 3));
    assertThat(node.getBody().getDtype()).isEqualTo("int32");

    assertThat(node.getShape()).isEqualTo(ZPoint.of(2, 3));
    assertThat(node.getDtype()).isEqualTo("int32");

    assertJsonEquals(node.getBody(), node.getBodyAsJson());

    assertEquivalentJson(source, node.toJsonString());

    assertEquivalentJson(
        node.getBodyAsJson(),
        """
              {
                "dtype": "int32",
                "shape": [2, 3]
              }
              """);

    node.setBodyFromJson(
        """
              {
                "dtype": "float32",
                "shape": [5, 6]
              }
              """);
    assertEquivalentJson(
        node.getBodyAsJson(),
        """
              {
                "dtype": "float32",
                "shape": [5, 6]
              }
              """);
  }

  @Test
  public void test_nodeBuilder() {
    var env =
        LoomEnvironment.builder()
            .build()
            .autowireNodeTypeClass(NoteNode.TYPE, NoteNode.class)
            .autowireNodeTypeClass(ExampleNode.TYPE, ExampleNode.class);
    var graph = env.newGraph();

    var node1 =
        graph
            .nodeBuilder()
            .label("node1")
            .type(ExampleNode.TYPE)
            .body(ExampleNode.Body.builder().foo("bar").build())
            .build();

    var node2 =
        graph
            .nodeBuilder()
            .label("node2")
            .type(ExampleNode.TYPE)
            .body("{\"foo\": \"baz\"}")
            .build();

    var node3 =
        graph
            .nodeBuilder()
            .label("node3")
            .type(ExampleNode.TYPE)
            .body(Map.of("foo", "bar"))
            .build();

    NoteNode.withBody(b -> b.message("hello world")).label("extraneous").addTo(graph);

    graph.validate();

    assertThat(graph.assertNode(node1.getId())).isInstanceOf(ExampleNode.class);
    assertThat(graph.assertNode(node2.getId())).isInstanceOf(ExampleNode.class);
    assertThat(graph.assertNode(node3.getId())).isInstanceOf(ExampleNode.class);
  }

  @Test
  public void test_scan() {
    var env = CommonEnvironments.expressionEnvironment();
    var graph = env.newGraph();

    var tensorNode = TensorNode.withBody(b -> b.dtype("int32").shape(ZPoint.of(2, 3))).addTo(graph);

    var noteNode = NoteNode.withBody(b -> b.message("foo")).addTo(graph);

    assertThat(graph.nodeScan().type(NoteNode.TYPE).asList()).containsOnly(noteNode);
    assertThat(graph.nodeScan().type(NoteNode.TYPE).nodeClass(NoteNode.class).asList())
        .containsOnly(noteNode);
    assertThat(graph.nodeScan().nodeClass(NoteNode.class).asList()).containsOnly(noteNode);

    assertThat(graph.nodeScan().type(TensorNode.TYPE).asList()).containsOnly(tensorNode);
    assertThat(graph.nodeScan().type(TensorNode.TYPE).nodeClass(TensorNode.class).asList())
        .containsOnly(tensorNode);
    assertThat(graph.nodeScan().nodeClass(TensorNode.class).asList()).containsOnly(tensorNode);

    assertThat(graph.nodeScan().type(TensorNode.TYPE).nodeClass(NoteNode.class).asList()).isEmpty();
  }
}
