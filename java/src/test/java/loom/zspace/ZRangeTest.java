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

    assertThat(ZRange.parseZRange(pretty)).isEqualTo(range);
    assertThat(ZRange.parseZRange(json)).isEqualTo(range);

    assertThat(range.contains(range)).isTrue();

    // Does a zero-dimensional range contain a zero-dimensional point?
    // The naive interpretations of `(start <= p && p < end)` suggest no.
    assertThat(range.contains(new ZPoint())).isFalse();

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
        .isThrownBy(() -> ZRange.parseZRange("zr[1, 2, 3]"));

    {
      var range = new ZRange(new ZPoint(), new ZPoint());
      String pretty = "zr[]";
      String json = "{\"start\":[], \"end\":[]}";

      assertThat(range).hasToString(pretty);
      assertJsonEquals(range, json);

      assertThat(ZRange.parseZRange(pretty)).isEqualTo(range);
      assertThat(ZRange.parseZRange(json)).isEqualTo(range);
    }

    {
      var range = new ZRange(new ZPoint(2, 3), new ZPoint(4, 5));
      String pretty = "zr[2:4, 3:5]";
      String json = "{\"start\":[2, 3], \"end\":[4, 5]}";

      assertThat(range).hasToString(pretty);
      assertJsonEquals(range, json);

      assertThat(ZRange.parseZRange(pretty)).isEqualTo(range);
      assertThat(ZRange.parseZRange(json)).isEqualTo(range);
    }
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
}
