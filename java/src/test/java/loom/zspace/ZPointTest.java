package loom.zspace;

import loom.testing.CommonAssertions;
import org.junit.Test;

public class ZPointTest implements CommonAssertions {
  @Test
  public void test_constructor() {
    {
      ZPoint p = new ZPoint(1, 2, 3);
      assertThat(p.ndim()).isEqualTo(3);
      assertThat(p.coords).isEqualTo(ZTensor.vector(1, 2, 3));
    }

    {
      ZPoint p = new ZPoint(ZTensor.vector(1, 2, 3));
      assertThat(p.ndim()).isEqualTo(3);
      assertThat(p.coords).isEqualTo(ZTensor.vector(1, 2, 3));
    }

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> new ZPoint(ZTensor.scalar(3)));

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> new ZPoint(ZTensor.zeros(2, 3)));
  }

  @Test
  public void test_hashCode() {
    ZPoint p = new ZPoint(1, 2, 3);
    assertThat(p).hasSameHashCodeAs(new ZPoint(1, 2, 3));
  }

  @Test
  public void test_string_parse_json() {
    ZPoint p = new ZPoint(2, 3);
    String str = "[2, 3]";

    assertThat(p).hasToString(str);
    assertThat(ZPoint.parseZPoint(str)).isEqualTo(p);
    assertJsonEquals(p, str);
  }

  @Test
  public void test_ordering() {
    var zeros = new ZPoint(0, 0);

    var p10 = new ZPoint(1, 0);
    var p01 = new ZPoint(0, 1);

    assertThat(ZPoint.Ops.partialCompare(zeros, zeros)).isEqualTo(PartialOrdering.EQUAL);
    assertThat(ZPoint.Ops.lt(zeros, zeros)).isFalse();
    assertThat(ZPoint.Ops.le(zeros, zeros)).isTrue();
    assertThat(ZPoint.Ops.eq(zeros, zeros)).isTrue();
    assertThat(ZPoint.Ops.ne(zeros, zeros)).isFalse();
    assertThat(ZPoint.Ops.ge(zeros, zeros)).isTrue();
    assertThat(ZPoint.Ops.gt(zeros, zeros)).isFalse();

    assertThat(ZPoint.Ops.lt(zeros, zeros.coords)).isFalse();
    assertThat(ZPoint.Ops.le(zeros, zeros.coords)).isTrue();
    assertThat(ZPoint.Ops.eq(zeros, zeros.coords)).isTrue();
    assertThat(ZPoint.Ops.ne(zeros, zeros.coords)).isFalse();
    assertThat(ZPoint.Ops.ge(zeros, zeros.coords)).isTrue();
    assertThat(ZPoint.Ops.gt(zeros, zeros.coords)).isFalse();

    assertThat(ZPoint.Ops.lt(zeros.coords, zeros)).isFalse();
    assertThat(ZPoint.Ops.le(zeros.coords, zeros)).isTrue();
    assertThat(ZPoint.Ops.eq(zeros.coords, zeros)).isTrue();
    assertThat(ZPoint.Ops.ne(zeros.coords, zeros)).isFalse();
    assertThat(ZPoint.Ops.ge(zeros.coords, zeros)).isTrue();
    assertThat(ZPoint.Ops.gt(zeros.coords, zeros)).isFalse();

    assertThat(ZPoint.Ops.lt(zeros.coords, zeros.coords)).isFalse();
    assertThat(ZPoint.Ops.le(zeros.coords, zeros.coords)).isTrue();
    assertThat(ZPoint.Ops.eq(zeros.coords, zeros.coords)).isTrue();
    assertThat(ZPoint.Ops.ne(zeros.coords, zeros.coords)).isFalse();
    assertThat(ZPoint.Ops.ge(zeros.coords, zeros.coords)).isTrue();
    assertThat(ZPoint.Ops.gt(zeros.coords, zeros.coords)).isFalse();

    assertThat(zeros.lt(zeros)).isFalse();
    assertThat(zeros.le(zeros)).isTrue();
    assertThat(zeros.eq(zeros)).isTrue();
    assertThat(zeros.ne(zeros)).isFalse();
    assertThat(zeros.ge(zeros)).isTrue();
    assertThat(zeros.gt(zeros)).isFalse();

    assertThat(zeros.lt(zeros.coords)).isFalse();
    assertThat(zeros.le(zeros.coords)).isTrue();
    assertThat(zeros.eq(zeros.coords)).isTrue();
    assertThat(zeros.ne(zeros.coords)).isFalse();
    assertThat(zeros.ge(zeros.coords)).isTrue();
    assertThat(zeros.gt(zeros.coords)).isFalse();

    assertThat(ZPoint.Ops.partialCompare(zeros, p01)).isEqualTo(PartialOrdering.LESS_THAN);
    assertThat(ZPoint.Ops.lt(zeros, p01)).isTrue();
    assertThat(ZPoint.Ops.le(zeros, p01)).isTrue();
    assertThat(ZPoint.Ops.eq(zeros, p01)).isFalse();
    assertThat(ZPoint.Ops.ne(zeros, p01)).isTrue();
    assertThat(ZPoint.Ops.ge(zeros, p01)).isFalse();
    assertThat(ZPoint.Ops.gt(zeros, p01)).isFalse();
    assertThat(zeros.lt(p01)).isTrue();
    assertThat(zeros.le(p01)).isTrue();
    assertThat(zeros.eq(p01)).isFalse();
    assertThat(zeros.ne(p01)).isTrue();
    assertThat(zeros.ge(p01)).isFalse();
    assertThat(zeros.gt(p01)).isFalse();

    assertThat(ZPoint.Ops.partialCompare(p01, zeros)).isEqualTo(PartialOrdering.GREATER_THAN);
    assertThat(ZPoint.Ops.lt(p01, zeros)).isFalse();
    assertThat(ZPoint.Ops.le(p01, zeros)).isFalse();
    assertThat(ZPoint.Ops.eq(p01, zeros)).isFalse();
    assertThat(ZPoint.Ops.ne(p01, zeros)).isTrue();
    assertThat(ZPoint.Ops.ge(p01, zeros)).isTrue();
    assertThat(ZPoint.Ops.gt(p01, zeros)).isTrue();
    assertThat(p01.lt(zeros)).isFalse();
    assertThat(p01.le(zeros)).isFalse();
    assertThat(p01.eq(zeros)).isFalse();
    assertThat(p01.ne(zeros)).isTrue();
    assertThat(p01.ge(zeros)).isTrue();
    assertThat(p01.gt(zeros)).isTrue();

    assertThat(ZPoint.Ops.partialCompare(zeros, p10)).isEqualTo(PartialOrdering.LESS_THAN);
    assertThat(ZPoint.Ops.lt(zeros, p10)).isTrue();
    assertThat(ZPoint.Ops.le(zeros, p10)).isTrue();
    assertThat(ZPoint.Ops.eq(zeros, p10)).isFalse();
    assertThat(ZPoint.Ops.ne(zeros, p10)).isTrue();
    assertThat(ZPoint.Ops.ge(zeros, p10)).isFalse();
    assertThat(ZPoint.Ops.gt(zeros, p10)).isFalse();
    assertThat(zeros.lt(p10)).isTrue();
    assertThat(zeros.le(p10)).isTrue();
    assertThat(zeros.eq(p10)).isFalse();
    assertThat(zeros.ne(p10)).isTrue();
    assertThat(zeros.ge(p10)).isFalse();
    assertThat(zeros.gt(p10)).isFalse();

    assertThat(ZPoint.Ops.partialCompare(p01, p10)).isEqualTo(PartialOrdering.INCOMPARABLE);
    assertThat(ZPoint.Ops.lt(p01, p10)).isFalse();
    assertThat(ZPoint.Ops.le(p01, p10)).isFalse();
    assertThat(ZPoint.Ops.eq(p01, p10)).isFalse();
    assertThat(ZPoint.Ops.ne(p01, p10)).isTrue();
    assertThat(ZPoint.Ops.ge(p01, p10)).isFalse();
    assertThat(ZPoint.Ops.gt(p01, p10)).isFalse();
    assertThat(p01.lt(p10)).isFalse();
    assertThat(p01.le(p10)).isFalse();
    assertThat(p01.eq(p10)).isFalse();
    assertThat(p01.ne(p10)).isTrue();
    assertThat(p01.ge(p10)).isFalse();
    assertThat(p01.gt(p10)).isFalse();
  }
}
