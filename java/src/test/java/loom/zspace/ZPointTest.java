package loom.zspace;

import java.util.List;
import loom.testing.CommonAssertions;
import org.junit.Test;

public class ZPointTest implements CommonAssertions {
  @Test
  public void test_clone() {
    var p = new ZPoint(1, 2, 3);
    assertThat(p.clone()).isEqualTo(p).isSameAs(p);
  }

  @Test
  public void test_of() {
    assertThat(ZPoint.of(1, 2, 3)).isEqualTo(new ZPoint(1, 2, 3));
  }

  @Test
  public void test_constructor() {
    {
      var p = new ZPoint(1, 2, 3);
      assertThat(p.getNDim()).isEqualTo(3);
      assertThat(p.coords).isEqualTo(ZTensor.newVector(1, 2, 3));
    }

    {
      var p = new ZPoint(ZTensor.newVector(1, 2, 3));
      assertThat(p.getNDim()).isEqualTo(3);
      assertThat(p.coords).isEqualTo(ZTensor.newVector(1, 2, 3));
    }

    {
      var p = new ZPoint(List.of(1, 2, 3));
      assertThat(p.getNDim()).isEqualTo(3);
      assertThat(p.coords).isEqualTo(ZTensor.newVector(1, 2, 3));
    }

    assertThatExceptionOfType(ZDimMissMatchError.class)
        .isThrownBy(() -> new ZPoint(ZTensor.newScalar(3)));

    assertThatExceptionOfType(ZDimMissMatchError.class)
        .isThrownBy(() -> new ZPoint(ZTensor.newZeros(2, 3)));
  }

  @Test
  public void test_newOnes() {
    assertThat(ZPoint.newOnes(3)).isEqualTo(new ZPoint(1, 1, 1));
    assertThat(ZPoint.newOnes(0)).isEqualTo(new ZPoint());

    assertThat(ZPoint.newOnesLike(new ZPoint(1, 2, 3))).isEqualTo(new ZPoint(1, 1, 1));
  }

  @Test
  public void test_newZeros() {
    assertThat(ZPoint.newZeros(3)).isEqualTo(new ZPoint(0, 0, 0));
    assertThat(ZPoint.newZeros(0)).isEqualTo(new ZPoint());

    assertThat(ZPoint.newZerosLike(new ZPoint(1, 2, 3))).isEqualTo(new ZPoint(0, 0, 0));
  }

  @Test
  public void test_get() {
    ZPoint p = new ZPoint(1, 2, 3);
    assertThat(p.get(0)).isEqualTo(1);
    assertThat(p.get(1)).isEqualTo(2);
    assertThat(p.get(2)).isEqualTo(3);
  }

  @Test
  public void test_toArray() {
    ZPoint p = new ZPoint(1, 2, 3);
    assertThat(p.toArray()).isEqualTo(new int[] {1, 2, 3});
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
    assertThat(ZPoint.parse(str)).isEqualTo(p);
    assertJsonEquals(p, str);

    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> ZPoint.parse("abc"));
  }

  @Test
  public void test_permute() {
    var p = new ZPoint(8, 9, 10);
    assertThat(p.permute(1, 2, 0)).isEqualTo(new ZPoint(9, 10, 8));
  }

  @Test
  public void test_ordering() {
    var zeros = new ZPoint(0, 0);

    var p10 = new ZPoint(1, 0);
    var p01 = new ZPoint(0, 1);

    assertThat(ZPoint.Ops.partialOrderByGrid(zeros, zeros)).isEqualTo(PartialOrdering.EQUAL);
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

    assertThat(ZPoint.Ops.partialOrderByGrid(zeros, p01)).isEqualTo(PartialOrdering.LESS_THAN);
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

    assertThat(ZPoint.Ops.partialOrderByGrid(p01, zeros)).isEqualTo(PartialOrdering.GREATER_THAN);
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

    assertThat(ZPoint.Ops.partialOrderByGrid(zeros, p10)).isEqualTo(PartialOrdering.LESS_THAN);
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

    assertThat(ZPoint.Ops.partialOrderByGrid(p01, p10)).isEqualTo(PartialOrdering.INCOMPARABLE);
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
