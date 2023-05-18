package loom.zspace;

import loom.testing.CommonAssertions;
import org.junit.Test;

public class ZPointTest implements CommonAssertions {
  @Test
  public void testValidateRange() {
    assertThatThrownBy(() -> ZPoint.verifyZPointLE(new long[] {0}, new long[] {1, 1}))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("start:[0] and end:[1, 1] differ in dimensions");

    assertThatThrownBy(() -> ZPoint.verifyZPointLE(new long[] {0, 2}, new long[] {1, 1}))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("start:[0, 2] is not <= end:[1, 1]");
  }

  @Test
  public void test() {
    var point = new ZPoint(1, 2, 3);
    assertThat(point.getCoords()).isEqualTo(new long[] {1, 2, 3});
    assertThat(point.ndim()).isEqualTo(3);

    assertThat(point.toString()).isEqualTo("z[1, 2, 3]");

    assertJsonEquals(point, "[1,2,3]");
  }

  @Test
  public void testScalar() {
    var scalar = ZPoint.scalar();
    assertThat(scalar.ndim()).isEqualTo(0);
    assertThat(scalar.getCoords()).isEqualTo(new long[] {});
    assertThat(scalar.toString()).isEqualTo("z[]");

    assertJsonEquals(scalar, "[]");

    assertThat(scalar).isEqualTo(new ZPoint());
    assertThat(scalar).isEqualTo(new ZPoint(new long[] {}));
  }
}
