package org.tensortapestry.weft.metakernels.expressions;

import org.junit.jupiter.api.Test;
import org.tensortapestry.common.json.JsonUtil;
import org.tensortapestry.common.testing.CommonAssertions;
import org.tensortapestry.zspace.ZPoint;

class TensorShapePatternMatcherTest implements CommonAssertions {

  @Test
  public void test_parse() {
    var expr = "[$batch..., $shape[$i]=($height, $width, $channels[$i]), $features[$i]]";
    var shape = ZPoint.of(100, 128, 256, 512, 3, 8);

    var pattern = TensorShapePatternMatcher.parse(expr);

    var match = pattern.match(shape);
    System.out.println(JsonUtil.toYaml(match));
  }

  @Test
  void test_round_trip() {
    String source = "[$batch..., $shape=($height, $width), $features[$i]]";
    var expr = TensorShapePatternMatcher.parse(source);
    assertThat(expr.toExpression()).isEqualTo(source);
  }

  @Test
  void test_validate() {
    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> TensorShapePatternMatcher.parse("[$batch, $batch]"))
      .withMessageContaining("Duplicate names: [$batch]");

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> TensorShapePatternMatcher.parse("[$batch, $outer=($foo, $batch)]"))
      .withMessageContaining("Duplicate names: [$batch]");

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> TensorShapePatternMatcher.parse("[$batch, $features[$batch]]"))
      .withMessageContaining("Overlap between names and index names: [$batch]");

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> TensorShapePatternMatcher.parse("[$batch..., $shape...]"))
      .withMessageContaining("Multiple ellipsis: [$batch..., $shape...]");
  }
}
