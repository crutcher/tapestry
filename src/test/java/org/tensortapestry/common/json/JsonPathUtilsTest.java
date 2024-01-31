package org.tensortapestry.common.json;

import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.tensortapestry.common.testing.BaseTestClass;

public class JsonPathUtilsTest extends BaseTestClass {

  @Test
  public void testJsonPointerToJsonPath() {
    List<Pair<String, String>> examples = List.of(
      Pair.of("", "$"),
      Pair.of(null, "$"),
      Pair.of("/foo", "$.foo"),
      Pair.of("/foo/bar", "$.foo.bar"),
      Pair.of("/foo/0", "$.foo[0]")
    );

    for (var example : examples) {
      assertThat(JsonPathUtils.jsonPointerToJsonPath(example.getLeft()))
        .isEqualTo(example.getRight());
    }
  }

  @Test
  public void testConcatJsonPath() {
    List<Pair<Object[], String>> examples = List.of(
      Pair.of(new Object[] { "$", "$.foo[2]", "", ".", "$.bar" }, "$.foo[2].bar"),
      Pair.of(new Object[] { "$", "$.['foo'][2]", "", ".", "$.bar" }, "$.foo[2].bar"),
      Pair.of(new Object[] { "$.", "", null, "foo", "[2]", "$.bar" }, "$.foo[2].bar"),
      Pair.of(new Object[] { ".foo[2]", "$.bar" }, "$.foo[2].bar")
    );

    for (var example : examples) {
      assertThat(JsonPathUtils.concatJsonPath(example.getLeft())).isEqualTo(example.getRight());
    }
  }

  @Test
  public void test_normalizePath() {
    List<Pair<String, String>> examples = List.of(
      Pair.of("$.['foo'][3]['bar baz']", "$.foo[3]['bar baz']"),
      Pair.of("$.foo['30']['091']", "$.foo[30]['091']")
    );

    for (var example : examples) {
      assertThat(JsonPathUtils.normalizePath(example.getLeft())).isEqualTo(example.getRight());
    }
  }
}
