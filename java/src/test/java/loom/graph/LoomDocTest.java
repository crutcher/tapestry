package loom.graph;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.List;
import java.util.UUID;
import loom.testing.CommonAssertions;
import org.junit.Test;

public class LoomDocTest implements CommonAssertions {
  @Test
  public void testAddNodeFromBuilder() {
    var doc = new LoomDoc();

    var nodeId = doc.addNode(LoomDoc.NodeDoc.builder().type("test").label("Foo"));

    assertThat(doc.assertNode(nodeId).getId()).isEqualTo(nodeId);
  }

  @Test
  public void testCopy() {
    var doc = new LoomDoc();
    var nodeId = doc.addNode(LoomDoc.NodeDoc.builder().type("test").label("Foo"));

    var dup = doc.deepCopy();

    assertJsonEquals(dup, doc.toJsonString());
    assertThat(doc).isNotSameAs(dup);
    assertThat(dup.assertNode(nodeId)).isNotSameAs(doc.assertNode(nodeId));
  }

  @Test
  public void testBasic() {
    var nodeId = "00000000-0000-4000-8000-000000000001";

    String json =
        """
                    {
                      "nodes": [
                         {
                           "id": "%s",
                           "type": "test",
                           "label": "Foo",
                           "fields": {
                             "a": 1,
                             "b": [2, 3]
                           }
                         }
                      ]
                    }
                    """
            .formatted(nodeId);

    var doc = new LoomDoc();

    doc.addNode(
        LoomDoc.NodeDoc.builder()
            .id(UUID.fromString(nodeId))
            .type("test")
            .label("Foo")
            .fieldFromObject("a", 1)
            .fieldFromString("b", "[2, 3]"));

    assertJsonEquals(doc, json);

    var node = doc.assertNode(nodeId);
    assertThat(node.getFieldAsJsonNode("b"))
        .isEqualTo(JsonNodeFactory.instance.arrayNode().add(2).add(3));
    assertThat(node.getFieldAsObject("b")).isEqualTo(List.of(2, 3));
    assertThat(node.getFieldAsType("b", int[].class)).isEqualTo(new int[] {2, 3});
  }

  @Test
  public void testAddNode() {
    var doc = new LoomDoc();
    var node =
        LoomDoc.NodeDoc.builder()
            .id(UUID.fromString("00000000-0000-0000-0000-000000000000"))
            .type("test")
            .build();

    assertThat(doc.hasNode(node.getId())).isFalse();
    assertThat(doc.hasNode(node.getId().toString())).isFalse();

    doc.addNode(node);

    assertThat(doc.hasNode(node.getId())).isTrue();
    assertThat(doc.hasNode(node.getId().toString())).isTrue();

    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> doc.addNode(node));

    assertThat(doc.assertNode(node.getId())).isEqualTo(node);
  }
}
