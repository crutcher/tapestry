package org.tensortapestry.loom.graph;

import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import org.tensortapestry.loom.common.json.JsonUtil;
import org.tensortapestry.loom.graph.dialects.common.NoteNode;
import org.tensortapestry.loom.testing.BaseTestClass;
import org.tensortapestry.loom.validation.ValidationIssue;

public class LoomNodeTest extends BaseTestClass {

  public LoomEnvironment makeEnvironment() {
    return LoomEnvironment
      .builder()
      .build()
      .autowireNodeTypeClass(NoteNode.NOTE_NODE_TYPE, NoteNode.class)
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

  public record Example(String foo) {}

  @Test
  public void test_annotations() {
    var graph = makeGraph();
    var node = ExampleNode.withBody(b -> b.foo("bar")).addTo(graph);
    graph.getEnv().getAnnotationTypeClasses().put("foo", String.class);
    graph.getEnv().getAnnotationTypeClasses().put("Example", Example.class);

    {
      node.setAnnotation("foo", "abc");

      assertThat(node.hasAnnotation("foo")).isTrue();
      assertThat(node.hasAnnotation("foo", String.class)).isTrue();
      assertThat(node.hasAnnotation("foo", Example.class)).isFalse();

      assertThat(node.getAnnotation("foo")).isEqualTo("abc");
      assertThat(node.getAnnotation("foo", String.class)).isEqualTo("abc");

      assertThatExceptionOfType(ClassCastException.class)
        .isThrownBy(() -> node.getAnnotation("foo", Integer.class));

      node.removeAnnotation("foo");
      assertThat(node.hasAnnotation("foo")).isFalse();
      assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> node.assertAnnotation("foo", String.class));
    }

    {
      node.setAnnotation("Example", new Example("xyz"));

      assertThat(node.getAnnotation("Example", Example.class)).isEqualTo(new Example("xyz"));

      assertThat(node.assertAnnotation("Example", Example.class)).isEqualTo(new Example("xyz"));

      node.setAnnotationFromJson("Example", "{\"foo\":\"zzz\"}");
      assertThat(node.getAnnotation("Example", Example.class)).isEqualTo(new Example("zzz"));

      node.setAnnotationFromValue("Example", Map.of("foo", "mmm"));
      assertThat(node.getAnnotation("Example", Example.class)).isEqualTo(new Example("mmm"));
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
        "ExampleNode{\"id\":\"%s\",\"type\":\"ExampleNode\",\"body\":{\"foo\":\"bar\"}}".formatted(
            node.getId()
          )
      );
  }

  @Test
  public void test_assertGraph() {
    {
      var graph = makeGraph();
      var node = ExampleNode.withBody(b -> b.foo("bar")).addTo(graph);

      assertThat(node.hasGraph()).isTrue();
      assertThat(node.getGraph()).isSameAs(graph);
      assertThat(node.assertGraph()).isSameAs(graph);
    }
    {
      var node = ExampleNode.withBody(b -> b.foo("bar")).id(UUID.randomUUID()).build();

      assertThat(node.hasGraph()).isFalse();
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
        ValidationIssue.Context
          .builder()
          .name("test")
          .jsonpath("$.nodes[@.id=='%s']".formatted(node.getId()))
          .data(node)
          .build()
      );

    assertThat(node.asValidationContext("test", "hello world"))
      .isEqualTo(
        ValidationIssue.Context
          .builder()
          .name("test")
          .message("hello world")
          .jsonpath("$.nodes[@.id=='%s']".formatted(node.getId()))
          .data(node)
          .build()
      );
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
        """.formatted(
          node.getId()
        )
    );

    graph.getEnv().getAnnotationTypeClasses().put("foo", String.class);
    graph.getEnv().getAnnotationTypeClasses().put("Example", Example.class);

    node.setAnnotation("foo", "abc");
    node.setAnnotation("Example", new Example("xyz"));

    assertJsonEquals(
      node,
      """
        {
          "id": "%s",
          "type": "ExampleNode",
          "body": {
            "foo": "bar"
          },
          "annotations": {
            "foo": "abc",
            "Example": {
              "foo": "xyz"
            }
          }
        }
        """.formatted(
          node.getId()
        )
    );
  }
}
