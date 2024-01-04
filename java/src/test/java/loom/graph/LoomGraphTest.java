package loom.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.common.json.JsonUtil;
import loom.common.json.WithSchema;
import loom.graph.nodes.GenericNode;
import loom.graph.nodes.TensorNode;
import loom.testing.BaseTestClass;
import loom.validation.ValidationIssue;
import loom.zspace.ZPoint;
import org.junit.Test;

public class LoomGraphTest extends BaseTestClass {
  @Jacksonized
  @SuperBuilder
  @Getter
  @Setter
  public static class DemoNode extends LoomNode<DemoNode, DemoNode.Body> {
    public static final String TYPE = "DemoNode";
    @Delegate @Nonnull private Body body;

    @Data
    @Jacksonized
    @Builder
    @WithSchema(
        """
    {
      "type": "object",
      "properties": {
        "foo": {
          "type": "string",
          "enum": ["bar", "baz"]
        }
      },
      "required": ["foo"]
    }
    """)
    public static class Body {
      @Nonnull private String foo;
    }
  }

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
  public void testRemoveNode() {
    var env = CommonEnvironments.genericEnvironment();
    var graph = env.newGraph();

    var node1 =
        graph.addNode(
            GenericNode.builder()
                .label("foo")
                .type("test")
                .body(GenericNode.Body.builder().field("a", 12).build()));

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
        .isThrownBy(() -> graph.assertNode(nodeIdA, "test", DemoNode.class));
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
  public void test_buildNode() {
    var env = LoomEnvironment.builder().build().addNodeTypeClass(DemoNode.TYPE, DemoNode.class);

    var graph = env.graphBuilder().build();

    Object body1 = DemoNode.Body.builder().foo("a").build();
    var a = (DemoNode) graph.nodeBuilder().type(DemoNode.TYPE).body(body1).build();

    var b = (DemoNode) graph.nodeBuilder().type(DemoNode.TYPE).body(Map.of("foo", "b")).build();

    var c = (DemoNode) graph.nodeBuilder().type(DemoNode.TYPE).body("{\"foo\": \"c\"}").build();

    Object body = JsonUtil.parseToJsonNodeTree("{\"foo\": \"d\"}");
    var d = (DemoNode) graph.nodeBuilder().type(DemoNode.TYPE).body(body).build();

    assertThat(a.getFoo()).isEqualTo("a");
    assertThat(b.getFoo()).isEqualTo("b");
    assertThat(c.getFoo()).isEqualTo("c");
    assertThat(d.getFoo()).isEqualTo("d");
  }

  @Test
  public void test_nodeBuilder() {
    var env = LoomEnvironment.builder().build().addNodeTypeClass(DemoNode.TYPE, DemoNode.class);

    var graph = env.graphBuilder().build();

    var node =
        graph.nodeBuilder().type(DemoNode.TYPE).label("abc").body(Map.of("foo", "bar")).build();

    assertThat(node)
        .isInstanceOf(DemoNode.class)
        .hasFieldOrPropertyWithValue("label", "abc")
        .hasFieldOrPropertyWithValue("foo", "bar");
  }

  @Test
  public void testNode() {
    var env = LoomEnvironment.builder().build().addNodeTypeClass(DemoNode.TYPE, DemoNode.class);

    var graph = env.graphBuilder().build();
    graph.setId(UUID.randomUUID());

    {
      DemoNode orphan =
          DemoNode.builder()
              .id(UUID.randomUUID())
              .type(DemoNode.TYPE)
              .body(DemoNode.Body.builder().foo("bar").build())
              .build();
      assertThat(orphan.getGraph()).isNull();
      assertThatExceptionOfType(IllegalStateException.class).isThrownBy(orphan::assertGraph);
    }

    DemoNode node =
        graph.addNode(
            DemoNode.builder()
                .type(DemoNode.TYPE)
                .body(DemoNode.Body.builder().foo("bar").build()));

    assertThat(node.asValidationContext("foo", "bar"))
        .isEqualTo(
            ValidationIssue.Context.builder()
                .name("foo")
                .message("bar")
                .jsonpath(node.getJsonPath())
                .data(node)
                .build());

    assertThat(node.assertGraph()).isSameAs(graph);

    assertThat(graph.hasNode(node.getId())).isTrue();
    assertThat(graph.hasNode(node.getId().toString())).isTrue();
    assertThat(graph.getNode(node.getId())).isSameAs(node);
    assertThat(graph.getNode(node.getId().toString())).isSameAs(node);

    assertThat(graph.toString()).contains("id=" + graph.getId());

    {
      var nodeList = new ArrayList<>();
      graph.nodeScan().asStream().forEach(nodeList::add);
      assertThat(nodeList).containsExactly(node);
    }

    {
      var otherGraph = env.graphBuilder().build();
      assertThatExceptionOfType(IllegalArgumentException.class)
          .isThrownBy(() -> otherGraph.addNode(node))
          .withMessageContaining("Node already belongs to a graph");
    }

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> graph.addNode(node))
        .withMessageContaining("Graph already has node with id: " + node.getId());

    assertThat(node)
        .isInstanceOf(DemoNode.class)
        .hasFieldOrPropertyWithValue("jsonPath", "$.nodes[@.id=='" + node.getId() + "']");

    assertThat(node).hasToString("DemoNode" + node.toJsonString());

    DemoNode selfRef = node.self();
    assertThat(selfRef).isSameAs(node);

    assertEquivalentJson(
        node.getBodyAsJson(),
        """
                {
                  "foo": "bar"
                }
                """);

    assertThat(node.getBodyAsJsonNode())
        .isEqualTo(
            JsonUtil.parseToJsonNodeTree(
                """
                    {
                      "foo": "bar"
                    }
                    """));

    node.setBody(DemoNode.Body.builder().foo("baz").build());
    assertEquivalentJson(
        node.getBodyAsJson(),
        """
                    {
                      "foo": "baz"
                    }
                    """);

    node.setBodyFromJson(
        """
                    {
                      "foo": "bar"
                    }
                    """);
    assertEquivalentJson(
        node.getBodyAsJson(),
        """
                        {
                          "foo": "bar"
                        }
                        """);

    node.setBodyFromValue(Map.of("foo", "baz"));
    assertEquivalentJson(
        node.getBodyAsJson(),
        """
                        {
                          "foo": "baz"
                        }
                        """);
  }
}
