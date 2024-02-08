package org.tensortapestry.zspace.ops;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;
import org.tensortapestry.zspace.ZPoint;
import org.tensortapestry.zspace.ZRange;

public class RangeOpsTest implements WithAssertions {

  @Test
  public void test_cartesianProduct() {
    var lhs = ZRange.builder().start(2).end(5).build();
    var rhs = ZRange.builder().start(10, 4).end(20, 8).build();

    assertThat(RangeOps.cartesianProduct(lhs, rhs))
      .isEqualTo(ZRange.builder().start(2, 10, 4).end(5, 20, 8).build());

    assertThat(RangeOps.cartesianProduct(lhs, ZPoint.of(2, 3)))
      .isEqualTo(ZRange.builder().start(2, 0, 0).end(5, 2, 3).build());

    assertThat(RangeOps.cartesianProduct(ZPoint.of(10), ZPoint.of(2, 3)))
      .isEqualTo(ZRange.builder().start(0, 0, 0).end(10, 2, 3).build());
  }
}
