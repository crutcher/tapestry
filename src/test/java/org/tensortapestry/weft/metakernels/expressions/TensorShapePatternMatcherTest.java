package org.tensortapestry.weft.metakernels.expressions;

import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.CommonAssertions;
import org.tensortapestry.zspace.ZPoint;

class TensorShapePatternMatcherTest implements CommonAssertions {

  @Test
  public void test_parse() {
    var expr = "[$batch..., $hwc[$i]=($shape=($height, $width), $channels[$i]), $features[$i]]";

    var pattern = TensorShapePatternMatcher.parse(expr);
    assertThat(pattern.getIndexNames()).containsExactly("$i");
    assertThat(pattern.getDepthFirstGroupOrder())
      .extracting(TensorShapePatternMatcher.PatternItem::getSymbol)
      .extracting(TensorShapePatternMatcher.Symbol::getName)
      .containsExactly("$shape", "$hwc");

    assertThat(pattern.toExpression()).isEqualTo(expr);
  }

  @Test
  public void test_match() {
    var expr = "[$batch..., $hwc[$i]=($shape=($height, $width), $channels[$i]), $features[$i]]";
    var shape = ZPoint.of(100, 128, 256, 512, 3, 8);

    var pattern = TensorShapePatternMatcher.parse(expr);

    var match = pattern.match(shape);

    assertThat(match.getPattern()).isEqualTo(pattern);
    assertThat(match.getShape()).isEqualTo(shape);
    assertThat(match.getGroups())
      .containsEntry(
        "$batch",
        TensorShapePatternMatch.GroupMatch
          .builder()
          .name("$batch")
          .start(0)
          .end(2)
          .value(ZPoint.of(100, 128))
          .build()
      )
      .containsEntry(
        "$shape",
        TensorShapePatternMatch.GroupMatch
          .builder()
          .name("$shape")
          .start(2)
          .end(4)
          .value(ZPoint.of(256, 512))
          .build()
      )
      .containsEntry(
        "$hwc",
        TensorShapePatternMatch.GroupMatch
          .builder()
          .name("$hwc")
          .start(2)
          .end(5)
          .value(ZPoint.of(256, 512, 3))
          .build()
      );

    assertThat(match.getDims())
      .containsEntry(
        "$height",
        TensorShapePatternMatch.DimMatch.builder().name("$height").index(2).value(256).build()
      )
      .containsEntry(
        "$width",
        TensorShapePatternMatch.DimMatch.builder().name("$width").index(3).value(512).build()
      )
      .containsEntry(
        "$channels",
        TensorShapePatternMatch.DimMatch.builder().name("$channels").index(4).value(3).build()
      )
      .containsEntry(
        "$features",
        TensorShapePatternMatch.DimMatch.builder().name("$features").index(5).value(8).build()
      );
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
