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
import loom.common.exceptions.LookupError;
import loom.common.json.JsonUtil;
import loom.common.json.WithSchema;
import loom.graph.nodes.GenericNode;
import loom.graph.nodes.GenericNodeMetaFactory;
import loom.graph.nodes.TensorNode;
import loom.graph.nodes.TypeMapNodeMetaFactory;
import loom.testing.BaseTestClass;
import loom.zspace.ZPoint;
import org.junit.Test;

public class LoomGraphTest extends BaseTestClass {
  @Jacksonized
  @SuperBuilder
  @Getter
  @Setter
  public static class DemoNode extends LoomGraph.Node<DemoNode, DemoNode.Body> {
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

  public static class DemoNodePrototype extends LoomGraph.NodePrototype<DemoNode, DemoNode.Body> {
    public static final String TYPE = "DemoNode";

    public DemoNodePrototype() {
      super(DemoNode.class, DemoNode.Body.class);
    }
  }

  @Test
  public void testDefaultGraph() {
    var id = UUID.randomUUID();
    var graph = LoomGraph.builder().id(id).build();

    assertThat(graph.getId()).isEqualTo(id);
    assertThat(graph.getEnv()).isSameAs(LoomGraph.GENERIC_ENV);
  }

  @Test
  public void testNewUnusedNodeId() {
    var graph = LoomGraph.GENERIC_ENV.graphBuilder().build();

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
  public void testHasAssertAddNode() {
    var graph = LoomGraph.builder().build();

    var nodeIdA = UUID.randomUUID();

    assertThat(graph.hasNode(nodeIdA)).isFalse();
    assertThat(graph.hasNode(nodeIdA.toString())).isFalse();
    assertThatExceptionOfType(LookupError.class).isThrownBy(() -> graph.assertNode(nodeIdA));
    assertThatExceptionOfType(LookupError.class)
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
    assertThatExceptionOfType(LookupError.class)
        .isThrownBy(() -> graph.assertNode(nodeIdA, "foo", GenericNode.class));
    assertThatExceptionOfType(LookupError.class)
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

    var env = LoomGraph.GENERIC_ENV;
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

    var env =
        LoomEnvironment.builder()
            .nodeMetaFactory(
                TypeMapNodeMetaFactory.builder()
                    .typeMapping(TensorNode.TYPE, TensorNode.Prototype.builder().build())
                    .build())
            .build();

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
    var factory = new GenericNodeMetaFactory();

    var node = (GenericNode) factory.nodeFromJson(source);

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

    var factory =
        TypeMapNodeMetaFactory.builder()
            .typeMapping(TensorNode.TYPE, TensorNode.Prototype.builder().build())
            .build();

    var node = (TensorNode) factory.nodeFromJson(source);

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
    var env =
        LoomEnvironment.builder()
            .nodeMetaFactory(
                TypeMapNodeMetaFactory.builder()
                    .typeMapping(DemoNodePrototype.TYPE, new DemoNodePrototype())
                    .build())
            .build();

    var graph = env.graphBuilder().build();

    var a =
        (DemoNode)
            graph.buildNode(DemoNodePrototype.TYPE, DemoNode.Body.builder().foo("a").build());

    var b = (DemoNode) graph.buildNode(DemoNodePrototype.TYPE, Map.of("foo", "b"));

    var c = (DemoNode) graph.buildNode(DemoNodePrototype.TYPE, "{\"foo\": \"c\"}");

    var d =
        (DemoNode)
            graph.buildNode(
                DemoNodePrototype.TYPE, JsonUtil.parseToJsonNodeTree("{\"foo\": \"d\"}"));

    assertThat(a.getFoo()).isEqualTo("a");
    assertThat(b.getFoo()).isEqualTo("b");
    assertThat(c.getFoo()).isEqualTo("c");
    assertThat(d.getFoo()).isEqualTo("d");
  }

  @Test
  public void testNode() {
    var env =
        LoomEnvironment.builder()
            .nodeMetaFactory(
                TypeMapNodeMetaFactory.builder()
                    .typeMapping(DemoNodePrototype.TYPE, new DemoNodePrototype())
                    .build())
            .build();

    var graph = env.graphBuilder().build();
    graph.setId(UUID.randomUUID());

    {
      DemoNode orphan =
          DemoNode.builder()
              .id(UUID.randomUUID())
              .type(DemoNodePrototype.TYPE)
              .body(DemoNode.Body.builder().foo("bar").build())
              .build();
      assertThat(orphan.getGraph()).isNull();
      assertThatExceptionOfType(IllegalStateException.class).isThrownBy(orphan::assertGraph);
    }

    DemoNode node =
        graph.addNode(
            DemoNode.builder()
                .type(DemoNodePrototype.TYPE)
                .body(DemoNode.Body.builder().foo("bar").build()));

    assertThat(graph.hasNode(node.getId())).isTrue();
    assertThat(graph.hasNode(node.getId().toString())).isTrue();
    assertThat(graph.getNode(node.getId())).isSameAs(node);
    assertThat(graph.getNode(node.getId().toString())).isSameAs(node);

    assertThat(graph.toString()).contains("id=" + graph.getId());

    {
      var nodeList = new ArrayList<>();
      graph.iterator().forEachRemaining(nodeList::add);
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

  @Test
  public void testNodeMeta() {
    DemoNodePrototype prototype = new DemoNodePrototype();
    TypeMapNodeMetaFactory metaFactory =
        TypeMapNodeMetaFactory.builder().typeMapping(DemoNodePrototype.TYPE, prototype).build();

    var env = LoomEnvironment.builder().nodeMetaFactory(metaFactory).build();

    var graph = env.graphBuilder().build();

    var node =
        graph.addNode(
            DemoNode.builder()
                .type(DemoNodePrototype.TYPE)
                .body(DemoNode.Body.builder().foo("bar").build()));

    assertThat(node).isInstanceOf(DemoNode.class);

    assertThat(metaFactory.getPrototypeForType(DemoNodePrototype.TYPE))
        .isInstanceOf(DemoNodePrototype.class);
    assertThat(
            metaFactory
                .getPrototypeForType(DemoNodePrototype.TYPE)
                .nodeFromJson(node.toJsonString()))
        .isInstanceOf(DemoNode.class);
    assertThat(metaFactory.getPrototypeForType(DemoNodePrototype.TYPE).nodeFromTree(node))
        .isInstanceOf(DemoNode.class);

    {
      var parsed = prototype.nodeFromJson(node.toJsonString());
      assertThat(parsed)
          .isNotSameAs(node)
          .hasFieldOrPropertyWithValue("id", node.getId())
          .isInstanceOf(DemoNode.class);
    }
    {
      var parsed = prototype.nodeFromTree(JsonUtil.valueToJsonNodeTree(node));
      assertThat(parsed)
          .isNotSameAs(node)
          .hasFieldOrPropertyWithValue("id", node.getId())
          .isInstanceOf(DemoNode.class);
    }
  }
}
