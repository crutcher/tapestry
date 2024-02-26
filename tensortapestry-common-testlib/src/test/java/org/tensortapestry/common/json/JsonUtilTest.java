package org.tensortapestry.common.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.TypeRef;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.CommonAssertions;

public class JsonUtilTest implements CommonAssertions, JsonUtil.WithNodeBuilders {

  @Test
  public void test_WithNodeBuilders() {
    assertJsonEquals(nullNode(), "null");
    assertThat(missingNode()).isEqualTo(JsonNodeFactory.instance.missingNode());

    assertJsonEquals(booleanNode(true), "true");

    assertJsonEquals(numberNode(1), "1");
    assertJsonEquals(numberNode(1.0), "1.0");
    assertJsonEquals(numberNode(1.0f), "1.0");

    assertJsonEquals(textNode("abc"), "\"abc\"");

    assertJsonEquals(objectNode().put("a", 1), "{\"a\": 1}");

    assertJsonEquals(arrayNode().add(1).add(2), "[1, 2]");
  }

  @Test
  public void test_jsonPath() {
    var node = JsonUtil.parseToJsonNodeTree(
      """
        {
          "a" : {
            "b" : [1, 2]
          }
        }"""
    );

    assertThat(JsonUtil.jsonPathOnValue(node, "$.a.b[1]", Integer.class)).isEqualTo(2);
    assertThat(JsonUtil.jsonPathOnValue(node, "$.a.b", new TypeRef<List<Integer>>() {}))
      .containsOnly(1, 2);
  }

  @Test
  public void test_stream() {
    assertThat(
      JsonUtil.Tree.stream((ObjectNode) JsonUtil.parseToJsonNodeTree("{\"a\" : 1, \"b\" : 2}"))
    )
      .containsOnly(Map.entry("a", numberNode(1)), Map.entry("b", numberNode(2)));

    assertThat(JsonUtil.Tree.stream((ArrayNode) JsonUtil.parseToJsonNodeTree("[1, 2, 3]")))
      .containsOnly(numberNode(1), numberNode(2), numberNode(3));
  }

  @Test
  public void test_anyOf() {
    var empty = arrayNode();

    assertThat(JsonUtil.Tree.allOf(empty, JsonNode::isNumber)).isTrue();
    assertThat(JsonUtil.Tree.anyOf(empty, JsonNode::isNumber)).isFalse();
    assertThat(JsonUtil.Tree.isAllNumeric(empty)).isTrue();

    var arrayNode = arrayNode().add(1).add(2).add(3);
    assertThat(JsonUtil.Tree.isAllNumeric(arrayNode)).isTrue();
    assertThat(JsonUtil.Tree.allOf(arrayNode, JsonNode::isNumber)).isTrue();
    assertThat(JsonUtil.Tree.allOf(arrayNode, n -> n.asInt() >= 2)).isFalse();
    assertThat(JsonUtil.Tree.anyOf(arrayNode, n -> n.asInt() >= 2)).isTrue();

    assertThat(JsonUtil.Tree.isAllNumeric(arrayNode().add(1).add("abc"))).isFalse();
  }

  public record ExampleClass(String a, int b) {}

  @Test
  public void test_toJson_Bad() {
    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> JsonUtil.toJson(new Object()));
    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> JsonUtil.toPrettyJson(new Object()));
  }

  @Test
  public void test_reformatToPrettyJson() {
    assertThat(JsonUtil.reformatToPrettyJson("{\"a\":\"a\",\"b\":1}"))
      .isEqualTo("""
        {
          "a" : "a",
          "b" : 1
        }""");
  }

  @Test
  @SuppressWarnings("unused")
  public void test_toJsonString() {
    var obj = new Object() {
      public final String a = "a";
      public final int b = 1;
    };
    assertThat(JsonUtil.toJson(obj)).isEqualTo("{\"a\":\"a\",\"b\":1}");
    assertThat(JsonUtil.toPrettyJson(obj))
      .isEqualTo("""
        {
          "a" : "a",
          "b" : 1
        }""");
  }

  @Test
  public void test_ToSimpleJson() {
    var example = new ExampleClass("hello", 3);

    assertThat(JsonUtil.toSimpleJson(List.of(example)))
      .isEqualTo(List.of(Map.of("a", "hello", "b", 3)));
  }

  @Test
  public void test_treeToSimpleJson() {
    assertThat(JsonUtil.treeToSimpleJson(nullNode())).isNull();
    assertThat(
      JsonUtil.treeToSimpleJson(
        JsonUtil.parseToJsonNodeTree(
          """
            {
              "a" : "hello",
              "b" : 3,
              "bool" : true
            }"""
        )
      )
    )
      .isEqualTo(Map.of("a", "hello", "b", 3, "bool", true));
    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> JsonUtil.treeToSimpleJson(missingNode()));
  }

  @Test
  public void test_parseToJsonNodeTree() {
    assertThat(JsonUtil.parseToJsonNodeTree("null")).isEqualTo(nullNode());
    assertThat(
      JsonUtil.parseToJsonNodeTree(
        """
          {
            "a" : "hello",
            "b" : 3
          }"""
      )
    )
      .isEqualTo(
        JsonUtil.parseToJsonNodeTree(
          """
            {
              "b" : 3,
              "a" : "hello"
            }"""
        )
      );

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> JsonUtil.parseToJsonNodeTree("abc"));
  }

  @Test
  public void testValidateSimpleJson() {
    JsonUtil.validateSimpleJson(2);

    JsonUtil.validateSimpleJson(2.0);
    JsonUtil.validateSimpleJson(Float.POSITIVE_INFINITY);
    JsonUtil.validateSimpleJson(Float.NEGATIVE_INFINITY);
    JsonUtil.validateSimpleJson(Float.NaN);

    JsonUtil.validateSimpleJson(true);
    JsonUtil.validateSimpleJson(false);
    JsonUtil.validateSimpleJson(null);

    JsonUtil.validateSimpleJson("hello");

    JsonUtil.validateSimpleJson(List.of(1, 2.0, "hello", true, false, Float.POSITIVE_INFINITY));

    JsonUtil.validateSimpleJson(
      Map.of(
        "a",
        1,
        "b",
        2.0,
        "c",
        "hello",
        "d",
        true,
        "e",
        false,
        "f",
        Float.POSITIVE_INFINITY,
        "g",
        List.of(1, 2.0, "hello", true, false, Float.POSITIVE_INFINITY),
        "h",
        Map.of("a", 1, "b", 2.0)
      )
    );

    JsonUtil.validateSimpleJson(List.of(Map.of("a", List.of("x", "y", Map.of("z", 1)))));

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() ->
        JsonUtil.validateSimpleJson(Map.of("abc", Map.of("xyz", List.of("a", "b", new Object()))))
      )
      .withMessageContaining("Unexpected value type (Object) at abc.xyz[2]");

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> JsonUtil.validateSimpleJson(new Object()));

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> JsonUtil.validateSimpleJson(List.of(new Object())));

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> JsonUtil.validateSimpleJson(Map.of(2, "abc")));

    var cycle = new ArrayList<>();
    //noinspection CollectionAddedToSelf
    cycle.add(cycle);
    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> JsonUtil.validateSimpleJson(Map.of("foo", cycle)));
    cycle.clear();
  }
}
