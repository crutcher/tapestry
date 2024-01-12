package loom.common.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Value;
import loom.testing.CommonAssertions;
import org.junit.Test;

public class JsonUtilTest implements CommonAssertions {

  @Test
  public void test_anyOf() {
    var empty = JsonNodeFactory.instance.arrayNode();

    assertThat(JsonUtil.Tree.allOf(empty, JsonNode::isNumber)).isTrue();
    assertThat(JsonUtil.Tree.anyOf(empty, JsonNode::isNumber)).isFalse();
    assertThat(JsonUtil.Tree.isAllNumeric(empty)).isTrue();

    var arrayNode = JsonNodeFactory.instance.arrayNode().add(1).add(2).add(3);
    assertThat(JsonUtil.Tree.isAllNumeric(arrayNode)).isTrue();
    assertThat(JsonUtil.Tree.allOf(arrayNode, JsonNode::isNumber)).isTrue();
    assertThat(JsonUtil.Tree.allOf(arrayNode, n -> n.asInt() >= 2)).isFalse();
    assertThat(JsonUtil.Tree.anyOf(arrayNode, n -> n.asInt() >= 2)).isTrue();

    assertThat(JsonUtil.Tree.isAllNumeric(JsonNodeFactory.instance.arrayNode().add(1).add("abc")))
        .isFalse();
  }

  @Value
  public static class ExampleClass {
    public String a;
    public int b;
  }

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
        .isEqualTo(
            """
            {
              "a" : "a",
              "b" : 1
            }""");
  }

  @Test
  @SuppressWarnings("unused")
  public void test_toJsonString() {
    var obj =
        new Object() {
          public final String a = "a";
          public final int b = 1;
        };
    assertThat(JsonUtil.toJson(obj)).isEqualTo("{\"a\":\"a\",\"b\":1}");
    assertThat(JsonUtil.toPrettyJson(obj))
        .isEqualTo(
            """
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
    assertThat(JsonUtil.treeToSimpleJson(JsonNodeFactory.instance.nullNode())).isNull();
    assertThat(
            JsonUtil.treeToSimpleJson(
                JsonUtil.parseToJsonNodeTree(
                    """
                    {
                      "a" : "hello",
                      "b" : 3,
                      "bool" : true
                    }""")))
        .isEqualTo(Map.of("a", "hello", "b", 3, "bool", true));
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> JsonUtil.treeToSimpleJson(JsonNodeFactory.instance.missingNode()));
  }

  @Test
  public void test_parseToJsonNodeTree() {
    assertThat(JsonUtil.parseToJsonNodeTree("null")).isEqualTo(JsonNodeFactory.instance.nullNode());
    assertThat(
            JsonUtil.parseToJsonNodeTree(
                """
                {
                  "a" : "hello",
                  "b" : 3
                }"""))
        .isEqualTo(
            JsonUtil.parseToJsonNodeTree(
                """
                {
                  "b" : 3,
                  "a" : "hello"
                }"""));

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
            Map.of("a", 1, "b", 2.0)));

    JsonUtil.validateSimpleJson(List.of(Map.of("a", List.of("x", "y", Map.of("z", 1)))));

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () ->
                JsonUtil.validateSimpleJson(
                    Map.of("abc", Map.of("xyz", List.of("a", "b", new Object())))))
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
