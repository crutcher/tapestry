package org.tensortapestry.common.json;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.BaseTestClass;

public class JsonPathUtilsTest extends BaseTestClass {

  @Test
  public void testJsonPointerToJsonPath() {
    var examples = List.of(
      Map.entry("", "$"),
      Map.entry("/foo", "$.foo"),
      Map.entry("/foo/bar", "$.foo.bar"),
      Map.entry("/foo/0", "$.foo[0]")
    );

    for (var example : examples) {
      assertThat(JsonPathUtils.jsonPointerToJsonPath(example.getKey()))
        .isEqualTo(example.getValue());
    }

    assertThat(JsonPathUtils.jsonPointerToJsonPath(null)).isEqualTo("$");
  }

  @Test
  public void testConcatJsonPath() {
    var examples = List.of(
      Map.entry(new Object[] { "$", "$.foo[2]", "", ".", "$.bar" }, "$.foo[2].bar"),
      Map.entry(new Object[] { "$", "$.['foo'][2]", "", ".", "$.bar" }, "$.foo[2].bar"),
      Map.entry(new Object[] { "$.", "", null, "foo", "[2]", "$.bar" }, "$.foo[2].bar"),
      Map.entry(new Object[] { ".foo[2]", "$.bar" }, "$.foo[2].bar")
    );

    for (var example : examples) {
      assertThat(JsonPathUtils.concatJsonPath(example.getKey())).isEqualTo(example.getValue());
    }
  }

  @Test
  public void test_normalizePath() {
    var examples = List.of(
      Map.entry("$.['foo'][3]['bar baz']", "$.foo[3]['bar baz']"),
      Map.entry("$.foo['30']['091']", "$.foo[30]['091']")
    );

    for (var example : examples) {
      assertThat(JsonPathUtils.normalizePath(example.getKey())).isEqualTo(example.getValue());
    }
  }
}
