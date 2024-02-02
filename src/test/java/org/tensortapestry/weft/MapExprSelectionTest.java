package org.tensortapestry.weft;

import java.util.List;
import java.util.Map;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.CommonAssertions;

public class MapExprSelectionTest implements CommonAssertions {

  @Test
  public void test_exp4j() {
    Expression e = new ExpressionBuilder("3 y - 2")
      .implicitMultiplication(true)
      .variables("x", "y", "z")
      .build();

    assertThat(new Expression(e).setVariable("y", 2).evaluate()).isEqualTo(4);
  }

  @Test
  public void test() {
    var s = MapExprSelection
      .builder()
      .item("x", "2z")
      .item("y", "x - 1")
      .item("z", MapExprSelection.ANY)
      .build();

    assertThat(s).hasToString("{x=\"2z\", y=\"x-1\", z=\"*\"}");

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() ->
        s.check(Map.of("x", List.of(1, 2, 3, 4), "y", List.of(1, 2, 3, 5), "z", List.of(1, 2)))
      )
      .withMessageContaining("Unexpected size for \"y\", expected 3, found 4:");
  }
}
