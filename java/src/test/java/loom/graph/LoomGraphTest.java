package loom.graph;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Delegate;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.common.LookupError;
import loom.common.serialization.JsonUtil;
import loom.graph.nodes.GenericNode;
import loom.graph.nodes.GenericNodeMetaFactory;
import loom.graph.nodes.TensorNode;
import loom.graph.nodes.TypeMapNodeMetaFactory;
import loom.testing.BaseTestClass;
import loom.zspace.ZPoint;
import org.junit.Test;

public class LoomGraphTest extends BaseTestClass {
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
        .hasFieldOrPropertyWithValue("meta", GenericNode.META)
        .isInstanceOf(GenericNode.class);

    assertThat(graph.hasNode(nodeIdA)).isTrue();
    assertThat(graph.hasNode(nodeIdA.toString())).isTrue();
    assertThat(graph.assertNode(nodeIdA)).isSameAs(nodeA);
    assertThat(graph.assertNode(nodeIdA.toString())).isSameAs(nodeA);
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
                "type": "TreeNode",
                "label": "foo",
                "body": {
                  "dtype": "int32",
                  "shape": [2, 3]
                }
              },
              {
                "id": "00000000-0000-0000-0000-000000000002",
                "type": "TreeNode",
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

    var graphCopy = graph.deepCopy();
    assertThat(graphCopy).isNotSameAs(graph).hasFieldOrPropertyWithValue("env", graph.getEnv());
    assertEquivalentJson(graphCopy.toJsonString(), graph.toJsonString());
    assertThat(graphCopy.assertNode("00000000-0000-0000-0000-000000000001").getBody())
        .hasFieldOrPropertyWithValue("fields", Map.of("dtype", "int32", "shape", List.of(2, 3)))
        .isNotSameAs(graph.assertNode("00000000-0000-0000-0000-000000000001").getBody());
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
                    "type": "TreeNode",
                    "label": "foo",
                    "body": {
                      "dtype": "int32",
                      "shape": [2, 3]
                    }
                  },
                  {
                    "id": "00000000-0000-0000-0000-000000000002",
                    "type": "TreeNode",
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
                    .typeMapping(
                        TensorNode.Meta.TYPE,
                        TensorNode.Meta.builder().validDType("int32").validDType("float32").build())
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

    var graphCopy = graph.deepCopy();
    assertThat(graphCopy).isNotSameAs(graph).hasFieldOrPropertyWithValue("env", graph.getEnv());
    assertEquivalentJson(graphCopy.toJsonString(), graph.toJsonString());
    assertThat(graphCopy.assertNode("00000000-0000-0000-0000-000000000001").getBody())
        .hasFieldOrPropertyWithValue("dtype", "int32")
        .isNotSameAs(graph.assertNode("00000000-0000-0000-0000-000000000001").getBody());
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

    assertThat(node.getFields())
        .containsExactly(entry("dtype", "int32"), entry("shape", List.of(2, 3)));

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
                "type": "TreeNode",
                "label": "foo",
                "body": {
                  "dtype": "int32",
                  "shape": [2, 3]
                }
            }
            """;

    var factory =
        TypeMapNodeMetaFactory.builder()
            .typeMapping(
                TensorNode.Meta.TYPE, TensorNode.Meta.builder().validDType("int32").build())
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

    node.setShape(ZPoint.of(3, 4));
    node.setDtype("float32");
    assertEquivalentJson(
        node.getBodyAsJson(),
        """
              {
                "dtype": "float32",
                "shape": [3, 4]
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

  @Jacksonized
  @SuperBuilder
  public static class DemoNode extends LoomGraph.Node<DemoNode, DemoNode.Body> {
    @Data
    @Jacksonized
    @Builder
    public static class Body {
      @Nonnull private String foo;
    }

    @Override
    public Class<Body> getBodyClass() {
      return Body.class;
    }

    @SuppressWarnings("unused")
    @Delegate
    private Body _delegateProvider() {
      return getBody();
    }
  }

  public static class DemoNodeMeta extends LoomGraph.NodeMeta<DemoNode, DemoNode.Body> {
    public static final String TYPE = "DemoNode";

    public static final String BODY_SCHEMA =
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
            """;

    public DemoNodeMeta() {
      super(DemoNode.class, DemoNode.Body.class, BODY_SCHEMA);
    }

    public static final Set<String> VALID_FOO_VALUES = Set.of("bar", "baz");

    @Override
    public void validateNode(DemoNode node) {
      if (!VALID_FOO_VALUES.contains(node.getFoo())) {
        throw new IllegalArgumentException(
            "Invalid foo value: " + node.getFoo() + "; expected one of " + VALID_FOO_VALUES);
      }
    }
  }

  @Test
  public void testNode() {
    var env =
        LoomEnvironment.builder()
            .nodeMetaFactory(
                TypeMapNodeMetaFactory.builder()
                    .typeMapping(DemoNodeMeta.TYPE, new DemoNodeMeta())
                    .build())
            .build();

    var graph = env.graphBuilder().build();

    DemoNode node =
        graph.addNode(
            DemoNode.builder()
                .type(DemoNodeMeta.TYPE)
                .body(DemoNode.Body.builder().foo("bar").build()));

    assertThat(node)
        .isInstanceOf(DemoNode.class)
        .hasFieldOrPropertyWithValue("jsonPath", "$.nodes[@.id=='" + node.getId() + "']");

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

    var nodeCopy = node.deepCopy();
    assertThat(nodeCopy)
        .isNotSameAs(node)
        .hasFieldOrPropertyWithValue("id", node.getId())
        .hasFieldOrPropertyWithValue("type", node.getType())
        .hasFieldOrPropertyWithValue("body", node.getBody())
        .isInstanceOf(DemoNode.class);

    assertThat(nodeCopy.getBody()).isNotSameAs(node.getBody());
  }

  @Test
  public void testNodeMeta() {
    TypeMapNodeMetaFactory metaFactory =
        TypeMapNodeMetaFactory.builder().typeMapping(DemoNodeMeta.TYPE, new DemoNodeMeta()).build();

    var env = LoomEnvironment.builder().nodeMetaFactory(metaFactory).build();

    var graph = env.graphBuilder().build();

    var node =
        graph.addNode(
            DemoNode.builder()
                .type(DemoNodeMeta.TYPE)
                .body(DemoNode.Body.builder().foo("bar").build()));

    assertThat(node).isInstanceOf(DemoNode.class);

    node.validate();

    var meta = node.getMeta();
    assertThat(meta).isInstanceOf(DemoNodeMeta.class);

    assertThat(metaFactory.getMetaForType(DemoNodeMeta.TYPE)).isInstanceOf(DemoNodeMeta.class);
    assertThat(metaFactory.getMetaForType(DemoNodeMeta.TYPE).nodeFromJson(node.toJsonString()))
        .isInstanceOf(DemoNode.class);
    assertThat(metaFactory.getMetaForType(DemoNodeMeta.TYPE).nodeFromTree(node))
        .isInstanceOf(DemoNode.class);

    {
      var parsed = meta.nodeFromJson(node.toJsonString());
      assertThat(parsed)
          .isNotSameAs(node)
          .hasFieldOrPropertyWithValue("id", node.getId())
          .isInstanceOf(DemoNode.class);
    }
    {
      var parsed = meta.nodeFromTree(JsonUtil.valueToJsonNodeTree(node));
      assertThat(parsed)
          .isNotSameAs(node)
          .hasFieldOrPropertyWithValue("id", node.getId())
          .isInstanceOf(DemoNode.class);
    }
  }
}
