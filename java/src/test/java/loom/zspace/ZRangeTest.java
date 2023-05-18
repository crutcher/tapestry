package loom.zspace;

import loom.testing.CommonAssertions;
import org.junit.Test;

public class ZRangeTest implements CommonAssertions {

  @Test
  public void testScalar() {
    var scalar = ZRange.scalar();
    assertThat(scalar.ndim()).isEqualTo(0);
    assertThat(scalar.shape()).isEqualTo(new long[] {});
    assertThat(scalar.size()).isEqualTo(1);

    assertThat(scalar.isScalar()).isTrue();
    assertThat(scalar.isEmpty()).isFalse();

    assertJsonEquals(scalar, "{\"start\":[],\"end\":[]}");
  }

  @Test
  public void testBasic() {
    var range = new ZRange(ZPoint.of(-3, 0), ZPoint.of(1, 1));
    assertJsonEquals(range, "{\"start\":[-3,0],\"end\":[1,1]}");

    assertThat(range.ndim()).isEqualTo(2);
    assertThat(range.shape()).isEqualTo(new long[] {4, 1});
    assertThat(range.size()).isEqualTo(4);

    assertThat(range.toString()).isEqualTo("zr[-3:1, 0:1]");
  }

  @Test
  public void testSize() {
    // scalar
    var scalarPoint = ZPoint.scalar();
    assertThat(ZRange.between(scalarPoint, scalarPoint).size()).isEqualTo(1);
  }
}
