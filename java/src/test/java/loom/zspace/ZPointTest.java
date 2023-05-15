package loom.zspace;

import loom.testing.CommonAssertions;
import org.junit.Test;

public class ZPointTest implements CommonAssertions {
  @Test
  public void testValidateRange() {
    assertThatThrownBy(() -> ZPoint.verifyZPointLE(new int[] {0}, new int[] {1, 1}))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("start:[0] and end:[1, 1] differ in dimensions");

    assertThatThrownBy(() -> ZPoint.verifyZPointLE(new int[] {0, 2}, new int[] {1, 1}))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("start:[0, 2] is not <= end:[1, 1]");
  }

  @Test
  public void test() {
    var point = new ZPoint(new int[] {1, 2, 3});
    assertThat(point.getCoords()).isEqualTo(new int[] {1, 2, 3});
    assertThat(point.ndim()).isEqualTo(3);

    assertThat(point.toString()).isEqualTo("<[1, 2, 3]>");

    assertJsonEquals(point, "[1,2,3]");
  }
}
