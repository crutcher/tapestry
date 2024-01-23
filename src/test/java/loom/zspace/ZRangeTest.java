package loom.zspace;

import java.util.List;
import loom.testing.CommonAssertions;
import org.junit.Test;

public class ZRangeTest implements CommonAssertions {

  @Test
  public void test_clone() {
    var r = new ZRange(new ZPoint(1, 2, 3), new ZPoint(4, 5, 6));
    assertThat(r.clone()).isEqualTo(r).isSameAs(r);
  }

  @Test
  public void test_builder() {
    assertThat(ZRange.builder().start(2, 3).end(10, 10).build())
      .isEqualTo(new ZRange(new ZPoint(2, 3), new ZPoint(10, 10)));
    assertThat(ZRange.builder().start(new ZPoint(2, 3)).end(new ZPoint(10, 10)).build())
      .isEqualTo(new ZRange(new ZPoint(2, 3), new ZPoint(10, 10)));
    assertThat(
      ZRange.builder().start(ZTensor.newVector(2, 3)).end(ZTensor.newVector(10, 10)).build()
    )
      .isEqualTo(new ZRange(new ZPoint(2, 3), new ZPoint(10, 10)));

    assertThat(ZRange.builder().shape(2, 3).build())
      .isEqualTo(new ZRange(new ZPoint(0, 0), new ZPoint(2, 3)));
    assertThat(ZRange.builder().start(10, 10).shape(2, 3).build())
      .isEqualTo(new ZRange(new ZPoint(10, 10), new ZPoint(12, 13)));

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> ZRange.builder().start(2, 3).end(10, 10).shape(2, 3).build())
      .withMessageContaining("Cannot set both shape and end");
  }

  @Test
  public void test_0dim_range() {
    // A zero-dimensional range is a slightly weird object.

    var range = new ZRange(new ZPoint(), new ZPoint());
    assertThat(range.getNDim()).isEqualTo(0);
    assertThat(range.getSize()).isEqualTo(1);

    assertThat(range.isEmpty()).isFalse();
    assertThat(range.getShape()).isEqualTo(ZTensor.newVector());

    String pretty = "zr[]";
    assertThat(range).hasToString(pretty);
    String json = "{\"start\":[],\"end\":[]}";
    assertThat(range.toJsonString()).isEqualTo(json);

    assertThat(ZRange.parse(pretty)).isEqualTo(range);
    assertThat(ZRange.parse(json)).isEqualTo(range);

    assertThat(range.contains(range)).isTrue();

    // Does a zero-dimensional range contain a zero-dimensional point?
    assertThat(range.contains(new ZPoint())).isTrue();
    assertThat(range.getInclusiveEnd()).isEqualTo(new ZPoint());
  }

  @Test
  public void test_constructor() {
    {
      var range = new ZRange(new ZPoint(), new ZPoint());
      assertThat(range.getNDim()).isEqualTo(0);
      assertThat(range.getSize()).isEqualTo(1);
    }
    {
      var range = new ZRange(new ZPoint(1, 2, 3), new ZPoint(4, 5, 6));
      assertThat(range.getNDim()).isEqualTo(3);
    }

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> new ZRange(new ZPoint(1, 2, 3), new ZPoint(4, 5)))
      .withMessageContaining("shape [3] != expected shape [2]");
    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> new ZRange(new ZPoint(1, 1), new ZPoint(0, 1)))
      .withMessageContaining("start [1, 1] must be <= end [0, 1]");
  }

  @SuppressWarnings("DuplicateExpressions")
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

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> ZRange.parse("abc"))
      .withMessageContaining("Invalid ZRange: \"abc\"");
  }

  @Test
  public void test_boundingRange() {
    assertThat(
      ZRange.boundingRange(
        new ZRange(new ZPoint(1, 2, 3), new ZPoint(4, 10, 6)),
        new ZRange(new ZPoint(2, 0, 4), new ZPoint(5, 6, 7))
      )
    )
      .isEqualTo(new ZRange(new ZPoint(1, 0, 3), new ZPoint(5, 10, 7)));

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> ZRange.boundingRange(List.of()))
      .withMessageContaining("no ranges");
  }

  @Test
  public void test_permute() {
    var r = new ZRange(new ZPoint(5, 6, 7), new ZPoint(8, 9, 10));
    assertThat(r.permute(1, 2, 0)).isEqualTo(new ZRange(new ZPoint(6, 7, 5), new ZPoint(9, 10, 8)));
  }

  @Test
  public void test_fromShape() {
    {
      var range = ZRange.newFromShape(2, 3);
      assertThat(range.getNDim()).isEqualTo(2);
      assertThat(range.getSize()).isEqualTo(6);
    }
    {
      var range = ZRange.newFromShape(ZTensor.newVector());
      assertThat(range.getNDim()).isEqualTo(0);
      assertThat(range.getSize()).isEqualTo(1);
    }
    {
      var range = ZRange.newFromShape(new ZPoint(2, 3));
      assertThat(range.getNDim()).isEqualTo(2);
      assertThat(range.getSize()).isEqualTo(6);
    }

    {
      HasZTensor start = new ZPoint(4, 5);
      HasZTensor shape = new ZPoint(2, 3);
      var range = ZRange.builder().start(start).shape(shape).build();
      assertThat(range.getNDim()).isEqualTo(2);
      assertThat(range.getSize()).isEqualTo(6);
      assertThat(range.getStart()).isEqualTo(new ZPoint(4, 5));
      assertThat(range.getEnd()).isEqualTo(new ZPoint(6, 8));
    }
  }

  @Test
  public void test_of() {
    {
      var range = ZRange.of(new ZPoint(2, 3), new ZPoint(4, 5));
      assertThat(range.getNDim()).isEqualTo(2);
      assertThat(range.getSize()).isEqualTo(4);
    }
    {
      var range = ZRange.of(ZTensor.newVector(2, 3), ZTensor.newVector(4, 5));
      assertThat(range.getNDim()).isEqualTo(2);
      assertThat(range.getSize()).isEqualTo(4);
    }
  }

  @Test
  public void test_byCoords() {
    var range = new ZRange(new ZPoint(2, 3), new ZPoint(4, 5));

    assertThat(range.byCoords(BufferMode.SAFE).stream().toList())
      .containsExactly(
        new int[] { 2, 3 },
        new int[] { 2, 4 },
        new int[] { 3, 3 },
        new int[] { 3, 4 }
      );

    assertThat(range.byCoords(BufferMode.REUSED).stream().toList())
      .containsExactly(
        new int[] { 3, 4 },
        new int[] { 3, 4 },
        new int[] { 3, 4 },
        new int[] { 3, 4 }
      );
    assertThat(range.byCoords(BufferMode.REUSED).stream().map(int[]::clone).toList())
      .containsExactly(
        new int[] { 2, 3 },
        new int[] { 2, 4 },
        new int[] { 3, 3 },
        new int[] { 3, 4 }
      );

    // Empty ranges.
    assertThat(ZRange.newFromShape(0, 0).byCoords(BufferMode.SAFE).stream().toList()).isEmpty();

    // Scalar ranges.
    assertThat(new ZRange(new ZPoint(), new ZPoint()).byCoords(BufferMode.SAFE).stream().toList())
      .containsExactly(new int[] {});
  }

  @Test
  public void test_inclusiveEnd() {
    {
      var range = new ZRange(new ZPoint(2, 3), new ZPoint(4, 5));
      assertThat(range.isEmpty()).isFalse();
      assertThat(range.getInclusiveEnd()).isEqualTo(new ZPoint(3, 4));
    }
    {
      var range = new ZRange(new ZPoint(2, 3), new ZPoint(2, 3));
      assertThat(range.isEmpty()).isTrue();
      assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(range::getInclusiveEnd)
        .withMessageContaining("Empty range");
    }
  }

  @Test
  public void test_contains() {
    {
      // 0-dim ranges.
      var r0 = new ZRange(new ZPoint(), new ZPoint());
      assertThat(r0.contains(r0.getStart())).isTrue();
      assertThat(r0.contains(r0.getEnd())).isTrue();
    }

    var range = ZRange.newFromShape(2, 3);

    assertThat(range.contains(range)).isTrue();
    assertThat(range.contains(range.getStart())).isTrue();
    assertThat(range.contains(range.getInclusiveEnd())).isTrue();
    assertThat(range.contains(range.getEnd())).isFalse();

    {
      // Empty Ranges
      ZRange empty = ZRange.newFromShape(0, 0);
      assertThat(empty.contains(empty)).isTrue();
      assertThat(empty.contains(empty.getStart())).isFalse();
      assertThat(range.contains(empty)).isTrue();
      assertThat(range.contains(empty.translate(range.getEnd()))).isTrue();
      assertThat(range.contains(empty.translate(ZTensor.newVector(-1, 0)))).isFalse();
    }

    assertThat(range.contains(new ZPoint(1, 1))).isTrue();
    assertThat(range.contains(new ZPoint(-2, 1))).isFalse();

    assertThat(range.contains(ZTensor.newVector(1, 1))).isTrue();
    assertThat(range.contains(ZTensor.newVector(-2, 1))).isFalse();

    assertThat(range.contains(ZRange.of(ZTensor.newVector(0, 0), ZTensor.newVector(1, 1))))
      .isTrue();
    assertThat(range.contains(ZRange.of(ZTensor.newVector(1, 2), ZTensor.newVector(2, 3))))
      .isTrue();

    assertThat(range.contains(ZRange.of(ZTensor.newVector(0, -1), ZTensor.newVector(1, 1))))
      .isFalse();
    assertThat(range.contains(ZRange.of(ZTensor.newVector(0, 0), ZTensor.newVector(3, 1))))
      .isFalse();
  }

  @Test
  public void test_intersection() {
    assertThat(ZRange.newFromShape().intersection(ZRange.newFromShape()))
      .isEqualTo(ZRange.newFromShape());

    var range = ZRange.newFromShape(2, 3);
    assertThat(range.intersection(range)).isEqualTo(range);
    assertThat(range.intersection(ZRange.of(new ZPoint(-2, 1), new ZPoint(2, 5))))
      .isEqualTo(ZRange.of(new ZPoint(0, 1), new ZPoint(2, 3)));

    assertThat(range.intersection(new ZRange(new ZPoint(-5, -5), new ZPoint(-1, -1)))).isNull();
  }

  @Test
  public void test_resolveDim() {
    var range = ZRange.newFromShape(2, 3);
    assertThat(range.resolveDim(0)).isEqualTo(0);
    assertThat(range.resolveDim(1)).isEqualTo(1);
    assertThat(range.resolveDim(-1)).isEqualTo(1);
    assertThat(range.resolveDim(-2)).isEqualTo(0);

    assertThatExceptionOfType(IndexOutOfBoundsException.class)
      .isThrownBy(() -> range.resolveDim(2))
      .withMessageContaining("invalid dimension: index 2 out of range [0, 2)");
    assertThatExceptionOfType(IndexOutOfBoundsException.class)
      .isThrownBy(() -> range.resolveDim(-3))
      .withMessageContaining("invalid dimension: index -3 out of range [0, 2)");
  }

  @Test
  public void test_split() {
    var range = ZRange.newFromShape(2, 3);

    assertThat(range.split(1, 2))
      .containsExactly(
        ZRange.of(new ZPoint(0, 0), new ZPoint(2, 2)),
        ZRange.of(new ZPoint(0, 2), new ZPoint(2, 3))
      );
    assertThat(range.split(-1, 2))
      .containsExactly(
        ZRange.of(new ZPoint(0, 0), new ZPoint(2, 2)),
        ZRange.of(new ZPoint(0, 2), new ZPoint(2, 3))
      );

    assertThat(range.split(-1, 3)).containsExactly(range);

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> range.split(0, -2))
      .withMessage("chunkSize must be > 0: -2");
  }
}
