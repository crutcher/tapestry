package loom.doozer;

import loom.doozer.nodes.GenericNode;
import loom.doozer.nodes.TensorNode;
import loom.testing.BaseTestClass;
import loom.zspace.ZPoint;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("SameNameButDifferent")
public class DoozerGraphTest extends BaseTestClass {

  // Goals:
  // Given a json fragment:
  //    > {
  //    >   "id": <UUID>,
  //    >   "type": <type>,
  //    >   "data": {
  //    >     <type specific fields>
  //    >   }
  //    > }
  //
  // 1. Match, based upon type, to a Node subclass.
  // 2. Validate the structure of the data field against a type-specific JSD schema.
  // 3. Parse the fragment into a type-specific Node subclass instance.
  // 4. Provide a way to serialize the Node subclass instance back into a json fragment.
  // 5. Provide 2 type-specific semantic validators:
  //    *. A validator which checks that the data field is valid against the JSD schema
  //       and the type specific semantic rules.
  //    *. A validator which checks that the node is valid in context of the full graph.
  // 6. Provide a way to read the data field as a generic JSON or Object tree.
  // 7. Provide a way to read the data field as a generic JSON or Object tree.
  // 8. Provide a way to read the data field as type-specific data.
  // 9. Provide a way to write the data field as type-specific data.
  //
  // In a validated node containing node references, it should be possible to
  // read and manipulate the references, and it should also be possible to
  // transparently traverse the references to read and manipulate the referenced
  // nodes.

  @Test
  public void testGraph() {
    var source =
        """
        {
          "id": "00000000-0000-4000-8000-00000000000A",
          "nodes": [
            {
              "id": "00000000-0000-0000-0000-000000000000",
              "type": "TreeNode",
              "label": "foo",
              "body": {
                "dtype": "int32",
                "shape": [2, 3]
              }
            }
          ]
         }
        """;

    var env =
        DoozerGraph.Environment.builder()
            .nodeMetaFactory(
                TypeMapNodeMetaFactory.builder()
                    .typeMapping(TensorNode.Meta.TYPE, new TensorNode.Meta())
                    .build())
            .build();

    var graph = env.graphFromJson(source);

    var node = (TensorNode) graph.assertNode("00000000-0000-0000-0000-000000000000");
    assertThat(node.getDtype()).isEqualTo("int32");
  }

  @Test
  public void testNothing() {
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
    {
      var factory =
          TypeMapNodeMetaFactory.builder()
              .typeMapping(TensorNode.Meta.TYPE, new TensorNode.Meta())
              .build();

      var node = (TensorNode) factory.nodeFromJson(source);

      assertThat(node.getId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000000"));
      assertThat(node.getLabel()).isEqualTo("foo");

      assertThat(node.getBody().getShape()).isEqualTo(ZPoint.of(2, 3));
      assertThat(node.getBody().getDtype()).isEqualTo("int32");

      assertThat(node.getShape()).isEqualTo(ZPoint.of(2, 3));
      assertThat(node.getDtype()).isEqualTo("int32");

      assertJsonEquals(node.getBody(), node.bodyAsJson());

      assertEquivalentJson(source, node.toJsonString());

      assertEquivalentJson(
          node.bodyAsJson(),
          """
              {
                "dtype": "int32",
                "shape": [2, 3]
              }
              """);

      assertThat(node.bodyAsMap()).isEqualTo(Map.of("dtype", "int32", "shape", List.of(2, 3)));

      node.setShape(ZPoint.of(3, 4));
      node.setDtype("float32");
      assertEquivalentJson(
          node.bodyAsJson(),
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
          node.bodyAsJson(),
          """
              {
                "dtype": "float32",
                "shape": [5, 6]
              }
              """);
    }

    {
      var factory = new GenericNodeMetaFactory();

      var node = (GenericNode) factory.nodeFromJson(source);

      assertThat(node.getId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000000"));
      assertThat(node.getLabel()).isEqualTo("foo");

      assertThat(node.getFields())
          .containsExactly(entry("dtype", "int32"), entry("shape", List.of(2, 3)));

      assertJsonEquals(node.getBody(), node.bodyAsJson());

      assertEquivalentJson(source, node.toJsonString());

      assertEquivalentJson(
          node.bodyAsJson(),
          """
              {
                "dtype": "int32",
                "shape": [2, 3]
              }
              """);
    }
  }
}
