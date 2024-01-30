package org.tensortapestry.loom.graph;

import java.util.UUID;
import org.junit.Test;
import org.tensortapestry.common.json.JsonUtil;
import org.tensortapestry.loom.graph.dialects.common.NoteNode;
import org.tensortapestry.loom.graph.dialects.tensorops.TensorNode;
import org.tensortapestry.loom.testing.BaseTestClass;
import org.tensortapestry.zspace.ZRange;

public class LoomGraphTest extends BaseTestClass {

  @Test
  public void testNewUnusedNodeId() {
    var env = CommonEnvironments.expressionEnvironment();
    var graph = env.newGraph();

    for (int i = 0; i < 10; i++) {
      NoteNode.builder(graph).configure(b -> b.message("test")).build();
    }

    var nodeId = graph.genNodeId();
    assertThat(graph.hasNode(nodeId)).isFalse();
  }

  @Test
  public void testAddRemoveNode() {
    var env = CommonEnvironments.expressionEnvironment();
    var graph = env.newGraph();

    var node1 = NoteNode.builder(graph).configure(b -> b.message("node 1")).label("foo").build();

    assertThat(graph.getNode(node1.getId())).isSameAs(node1.unwrap());
    assertThat(graph.getNode(node1.getId().toString())).isSameAs(node1.unwrap());

    // Idempotent
    graph.addNode(node1);

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> graph.addNode(LoomNode.builder().id(node1.getId()).type("foo").body("Abc")))
      .withMessage("Graph already has node with id: %s".formatted(node1.getId()));

    {
      var graph2 = env.newGraph();
      var node2 = NoteNode.builder(graph2).configure(b -> b.message("node 1")).build();

      assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> graph.addNode(node2))
        .withMessage("Node already belongs to a graph: %s".formatted(graph2.getId()));
    }

    var node2 = NoteNode.builder(graph).configure(b -> b.message("node 2")).label("bar").build();
    var node3 = NoteNode.builder(graph).configure(b -> b.message("node 3")).label("qux").build();

    {
      // By Node

      graph.removeNode(node1);
      assertThat(node1.getGraph()).isNull();
      assertThat(graph.hasNode(node1.getId())).isFalse();
      graph.removeNode(node2.getId());
      // Should be a no-op.
      graph.removeNode(node1);
    }

    {
      // By Id
      assertThat(node2.getGraph()).isNull();
      assertThat(graph.hasNode(node2.getId())).isFalse();
    }

    {
      // By Id String
      graph.removeNode(node3.getId().toString());
      assertThat(node3.getGraph()).isNull();
      assertThat(graph.hasNode(node3.getId())).isFalse();
    }
  }

  @Test
  public void testHasAssertAddNode() {
    var env = CommonEnvironments.expressionEnvironment();
    var graph = env.newGraph();

    var nodeIdA = UUID.randomUUID();

    assertThat(graph.hasNode(nodeIdA)).isFalse();
    assertThat(graph.hasNode(nodeIdA.toString())).isFalse();
    assertThatExceptionOfType(IllegalStateException.class)
      .isThrownBy(() -> graph.assertNode(nodeIdA));
    assertThatExceptionOfType(IllegalStateException.class)
      .isThrownBy(() -> graph.assertNode(nodeIdA.toString()));

    var nodeA = NoteNode.builder(graph).configure(b -> b.message("test")).id(nodeIdA).build();

    assertThat(nodeA)
      .hasFieldOrPropertyWithValue("id", nodeIdA)
      .hasFieldOrPropertyWithValue("type", NoteNode.TYPE);

    assertThat(graph.hasNode(nodeIdA)).isTrue();
    assertThat(graph.hasNode(nodeIdA.toString())).isTrue();
    assertThat(graph.assertNode(nodeIdA)).isSameAs(nodeA.unwrap());
    assertThat(graph.assertNode(nodeIdA.toString())).isSameAs(nodeA.unwrap());

    // Casting assertions.
    assertThat(graph.assertNode(nodeIdA, NoteNode.TYPE)).isSameAs(nodeA.unwrap());
    assertThatExceptionOfType(IllegalStateException.class)
      .isThrownBy(() -> graph.assertNode(nodeIdA, "foo"));
  }

  @Test
  public void test_json() {
    var source =
      """
        {
           "id": "00000000-0000-0000-0000-000000000000",
           "nodes": [
              {
                "id": "00000000-0000-0000-0000-000000000001",
                "type": "%1$s",
                "label": "foo",
                "body": {
                  "dtype": "int32",
                  "range": {"start": [0, 0], "end": [2, 3] }
                }
              },
              {
                "id": "00000000-0000-0000-0000-000000000002",
                "type": "%1$s",
                "label": "bar",
                "body": {
                  "dtype": "float32",
                  "range": {"start": [0, 0], "end": [4, 5] }
                }
              }
           ]
        }
        """.formatted(
          TensorNode.TYPE
        );

    var graph = JsonUtil.fromJson(source, LoomGraph.class);
    assertThat(graph.getEnv()).isNull();

    assertThat(graph.getId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000000"));

    assertThat(graph.assertNode("00000000-0000-0000-0000-000000000001", TensorNode.TYPE))
      .hasFieldOrPropertyWithValue("label", "foo");
    assertThat(graph.assertNode("00000000-0000-0000-0000-000000000002"))
      .hasFieldOrPropertyWithValue("label", "bar");

    assertThatJson(graph.toJsonString()).isEqualTo(source);
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
                    "type": "%1$s",
                    "label": "foo",
                    "body": {
                      "dtype": "int32",
                      "range": { "start": [0, 0], "end": [2, 3] }
                    }
                  },
                  {
                    "id": "00000000-0000-0000-0000-000000000002",
                    "type": "%1$s",
                    "label": "bar",
                    "body": {
                      "dtype": "float32",
                      "range": { "start": [0, 0], "end": [4, 5] }
                    }
                  }
               ]
            }
            """.formatted(
          TensorNode.TYPE
        );

    var env = CommonEnvironments.expressionEnvironment();

    var graph = env.graphFromJson(source);
    graph.validate();

    assertThatJson(
      graph.assertNode("00000000-0000-0000-0000-000000000001").viewBodyAs(TensorNode.Body.class)
    )
      .isEqualTo(TensorNode.Body.builder().dtype("int32").range(ZRange.newFromShape(2, 3)).build());
    assertThatJson(
      graph.assertNode("00000000-0000-0000-0000-000000000002").viewBodyAs(TensorNode.Body.class)
    )
      .isEqualTo(
        TensorNode.Body.builder().dtype("float32").range(ZRange.newFromShape(4, 5)).build()
      );

    assertThatJson(graph.toJsonString()).isEqualTo(source);
  }
}
