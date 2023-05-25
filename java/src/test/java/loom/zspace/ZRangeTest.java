package loom.zspace;

import loom.testing.CommonAssertions;
import org.junit.Test;

public class ZRangeTest implements CommonAssertions {
  @Test
  public void test_0dim_range() {
    // A zero-dimensional range is a slightly weird object.

    var range = new ZRange(new ZPoint(), new ZPoint());
    assertThat(range.ndim()).isEqualTo(0);
    assertThat(range.size).isEqualTo(1);

    assertThat(range.isEmpty()).isFalse();
    assertThat(range.shape).isEqualTo(ZTensor.vector());

    String pretty = "zr[]";
    assertThat(range).hasToString(pretty);
    String json = "{\"start\":[],\"end\":[]}";
    assertThat(range.toJsonString()).isEqualTo(json);

    assertThat(ZRange.parse(pretty)).isEqualTo(range);
    assertThat(ZRange.parse(json)).isEqualTo(range);

    assertThat(range.contains(range)).isTrue();

    // Does a zero-dimensional range contain a zero-dimensional point?
    assertThat(range.contains(new ZPoint())).isTrue();
    assertThat(range.inclusiveEnd()).isEqualTo(new ZPoint());
  }

  @Test
  public void test_constructor() {
    {
      var range = new ZRange(new ZPoint(), new ZPoint());
      assertThat(range.ndim()).isEqualTo(0);
      assertThat(range.size).isEqualTo(1);
    }
    {
      var range = new ZRange(new ZPoint(1, 2, 3), new ZPoint(4, 5, 6));
      assertThat(range.ndim()).isEqualTo(3);
    }
  }

  @Test
  public void test_hashCode() {
    var range = new ZRange(new ZPoint(1, 2, 3), new ZPoint(4, 5, 6));
    assertThat(range).hasSameHashCodeAs(new ZRange(new ZPoint(1, 2, 3), new ZPoint(4, 5, 6)));
  }

  @Test
  public void test_string_parse_json() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> ZRange.parse("zr[1, 2, 3]"));

    {
      var range = new ZRange(new ZPoint(), new ZPoint());
      String pretty = "zr[]";
      String json = "{\"start\":[], \"end\":[]}";

      assertThat(range).hasToString(pretty);
      assertJsonEquals(range, json);

      assertThat(ZRange.parse(pretty)).isEqualTo(range);
      assertThat(ZRange.parse(json)).isEqualTo(range);
    }

    {
      var range = new ZRange(new ZPoint(2, 3), new ZPoint(4, 5));
      String pretty = "zr[2:4, 3:5]";
      String json = "{\"start\":[2, 3], \"end\":[4, 5]}";

      assertThat(range).hasToString(pretty);
      assertJsonEquals(range, json);

      assertThat(ZRange.parse(pretty)).isEqualTo(range);
      assertThat(ZRange.parse(json)).isEqualTo(range);
    }
  }

  @Test
  public void test_boundingRange() {
    assertThat(
            ZRange.boundingRange(
                new ZRange(new ZPoint(1, 2, 3), new ZPoint(4, 10, 6)),
                new ZRange(new ZPoint(2, 0, 4), new ZPoint(5, 6, 7))))
        .isEqualTo(new ZRange(new ZPoint(1, 0, 3), new ZPoint(5, 10, 7)));
  }

  @Test
  public void test_permute() {
    var r = new ZRange(new ZPoint(5, 6, 7), new ZPoint(8, 9, 10));
    assertThat(r.permute(1, 2, 0)).isEqualTo(new ZRange(new ZPoint(6, 7, 5), new ZPoint(9, 10, 8)));
  }

  @Test
  public void test_fromShape() {
    {
      var range = ZRange.fromShape(2, 3);
      assertThat(range.ndim()).isEqualTo(2);
      assertThat(range.size).isEqualTo(6);
    }
    {
      var range = ZRange.fromShape(ZTensor.vector());
      assertThat(range.ndim()).isEqualTo(0);
      assertThat(range.size).isEqualTo(1);
    }
    {
      var range = ZRange.fromShape(new ZPoint(2, 3));
      assertThat(range.ndim()).isEqualTo(2);
      assertThat(range.size).isEqualTo(6);
    }
  }

  @Test
  public void test_of() {
    {
      var range = ZRange.of(new ZPoint(2, 3), new ZPoint(4, 5));
      assertThat(range.ndim()).isEqualTo(2);
      assertThat(range.size).isEqualTo(4);
    }
    {
      var range = ZRange.of(ZTensor.vector(2, 3), ZTensor.vector(4, 5));
      assertThat(range.ndim()).isEqualTo(2);
      assertThat(range.size).isEqualTo(4);
    }
  }

  @Test
  public void test_contains() {
    {
      // 0-dim ranges.
      var r0 = new ZRange(new ZPoint(), new ZPoint());
      assertThat(r0.contains(r0.start)).isTrue();
      assertThat(r0.contains(r0.end)).isTrue();
    }

    var range = ZRange.fromShape(2, 3);

    assertThat(range.contains(range)).isTrue();
    assertThat(range.contains(range.start)).isTrue();
    assertThat(range.contains(range.inclusiveEnd())).isTrue();
    assertThat(range.contains(range.end)).isFalse();

    {
      // Empty Ranges
      ZRange empty = ZRange.fromShape(0, 0);
      assertThat(empty.contains(empty)).isTrue();
      assertThat(empty.contains(empty.start)).isFalse();
      assertThat(range.contains(empty)).isTrue();
      assertThat(range.contains(empty.translate(range.end))).isTrue();
      assertThat(range.contains(empty.translate(ZTensor.vector(-1, 0)))).isFalse();
    }

    assertThat(range.contains(new ZPoint(1, 1))).isTrue();
    assertThat(range.contains(new ZPoint(-2, 1))).isFalse();

    assertThat(range.contains(ZTensor.vector(1, 1))).isTrue();
    assertThat(range.contains(ZTensor.vector(-2, 1))).isFalse();

    assertThat(range.contains(ZRange.of(ZTensor.vector(0, 0), ZTensor.vector(1, 1)))).isTrue();
    assertThat(range.contains(ZRange.of(ZTensor.vector(1, 2), ZTensor.vector(2, 3)))).isTrue();

    assertThat(range.contains(ZRange.of(ZTensor.vector(0, -1), ZTensor.vector(1, 1)))).isFalse();
    assertThat(range.contains(ZRange.of(ZTensor.vector(0, 0), ZTensor.vector(3, 1)))).isFalse();
  }
}
