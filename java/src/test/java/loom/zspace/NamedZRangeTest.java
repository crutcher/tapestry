package loom.zspace;

import loom.testing.CommonAssertions;
import org.junit.Test;

public class NamedZRangeTest implements CommonAssertions {
  @Test
  public void test_string_parse_json() {
    var range =
        new NamedZRange(new DimensionMap("x", "y", "z"), ZRange.fromShape(ZTensor.vector(3, 4, 5)));

    String pretty = "i[x=0:3, y=0:4, z=0:5]";
    String json =
        "{\"dimensions\":[\"x\",\"y\",\"z\"],\"range\":{\"start\":[0, 0, 0], \"end\":[3, 4, 5]}}";

    assertThat(range).hasToString(pretty);
    assertJsonEquals(range, json);

    assertThat(NamedZRange.parse(pretty)).isEqualTo(range);
    assertThat(NamedZRange.parse(json)).isEqualTo(range);
  }

  @Test
  public void test_permute() {
    var range =
        new NamedZRange(new DimensionMap("x", "y", "z"), ZRange.fromShape(ZTensor.vector(3, 4, 5)));

    assertThat(range.permute(1, 2, 0))
        .isEqualTo(
            new NamedZRange(
                new DimensionMap("y", "z", "x"), ZRange.fromShape(ZTensor.vector(4, 5, 3))));

    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> range.permute(1, 2, 3));
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> range.permute(1, 1, 2));
  }
}
