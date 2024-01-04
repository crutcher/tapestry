package loom.graph;

import java.util.Map;
import java.util.UUID;
import loom.common.json.JsonUtil;
import loom.graph.nodes.NoteNode;
import loom.testing.BaseTestClass;
import loom.validation.ValidationIssue;
import org.junit.Test;

public class LoomNodeTest extends BaseTestClass {

  public LoomEnvironment makeEnvironment() {
    return LoomEnvironment.builder()
        .build()
        .autowireNodeTypeClass(NoteNode.TYPE, NoteNode.class)
        .autowireNodeTypeClass(ExampleNode.TYPE, ExampleNode.class);
  }

  public LoomGraph makeGraph() {
    return makeEnvironment().newGraph();
  }

  @Test
  public void test_getBodyClass() {
    {
      // Static
      assertThat(LoomNode.getBodyClass(ExampleNode.class)).isEqualTo(ExampleNode.Body.class);
    }

    {
      // Dynamic
      var graph = makeGraph();
      var node = ExampleNode.withBody(b -> b.foo("bar")).addTo(graph);

      assertThat(node.getBodyClass()).isEqualTo(ExampleNode.Body.class);
    }
  }

  @Test
  public void test_getJsonPath() {
    var graph = makeGraph();
    var node = ExampleNode.withBody(b -> b.foo("bar")).addTo(graph);

    assertThat(node.getJsonPath()).isEqualTo("$.nodes[@.id=='%s']".formatted(node.getId()));
  }

  @Test
  public void test_toString() {
    var graph = makeGraph();
    var node = ExampleNode.withBody(b -> b.foo("bar")).addTo(graph);

    assertThat(node)
        .hasToString(
            "ExampleNode{\"id\":\"%s\",\"type\":\"ExampleNode\",\"body\":{\"foo\":\"bar\"}}"
                .formatted(node.getId()));
  }

  @Test
  public void test_assertGraph() {
    {
      var graph = makeGraph();
      var node = ExampleNode.withBody(b -> b.foo("bar")).addTo(graph);

      assertThat(node.getGraph()).isSameAs(graph);
      assertThat(node.assertGraph()).isSameAs(graph);
    }
    {
      var node = ExampleNode.withBody(b -> b.foo("bar")).id(UUID.randomUUID()).build();

      assertThat(node.getGraph()).isNull();
      assertThatThrownBy(node::assertGraph).isInstanceOf(IllegalStateException.class);
    }
  }

  @Test
  public void test_asValidationContext() {
    var graph = makeGraph();
    var node = ExampleNode.withBody(b -> b.foo("bar")).addTo(graph);

    assertThat(node.asValidationContext("test"))
        .isEqualTo(
            ValidationIssue.Context.builder()
                .name("test")
                .jsonpath("$.nodes[@.id=='%s']".formatted(node.getId()))
                .data(node)
                .build());

    assertThat(node.asValidationContext("test", "hello world"))
        .isEqualTo(
            ValidationIssue.Context.builder()
                .name("test")
                .message("hello world")
                .jsonpath("$.nodes[@.id=='%s']".formatted(node.getId()))
                .data(node)
                .build());
  }

  @Test
  public void test_getBody() {
    var graph = makeGraph();
    var node = ExampleNode.withBody(b -> b.foo("bar")).addTo(graph);

    assertThat(node.getBody()).isEqualTo(ExampleNode.Body.builder().foo("bar").build());
    assertEquivalentJson(node.getBodyAsJson(), "{\"foo\":\"bar\"}");
    assertThat(node.getBodyAsJsonNode())
        .isEqualTo(JsonUtil.parseToJsonNodeTree("{\"foo\":\"bar\"}"));

    node.setBody(ExampleNode.Body.builder().foo("baz").build());
    assertThat(node.getFoo()).isEqualTo("baz");

    node.setBodyFromJson("{\"foo\":\"qux\"}");
    assertThat(node.getFoo()).isEqualTo("qux");

    node.setBodyFromValue(Map.of("foo", "jjj"));
    assertThat(node.getFoo()).isEqualTo("jjj");

    node.setBodyFromValue(ExampleNode.Body.builder().foo("mmm").build());
    assertThat(node.getFoo()).isEqualTo("mmm");
  }

  @Test
  public void test_json() {
    var graph = makeGraph();
    var node = ExampleNode.withBody(b -> b.foo("bar")).addTo(graph);

    assertJsonEquals(
        node,
        """
        {
          "id": "%s",
          "type": "ExampleNode",
          "body": {
            "foo": "bar"
          }
        }
        """
            .formatted(node.getId()));
  }
}