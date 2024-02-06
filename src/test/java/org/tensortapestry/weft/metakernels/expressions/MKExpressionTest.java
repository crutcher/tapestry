package org.tensortapestry.weft.metakernels.expressions;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.CommonAssertions;

public class MKExpressionTest implements CommonAssertions {

  @Test
  public void test_parseSizeExpr() {
    var input = "a.b + (2 + (-b)) * c + 3";
    var expr = MKExpression.parse(input);

    assertThat(expr.eval(Map.of("a.b", 1, "b", 2, "c", 3)::get)).isEqualTo(4);

    // System.out.println(expr.format());
    assertThat(expr.format()).isEqualTo(input);
  }
}
