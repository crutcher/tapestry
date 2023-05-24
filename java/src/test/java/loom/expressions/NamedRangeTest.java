package loom.expressions;

import loom.testing.CommonAssertions;
import loom.zspace.DimensionMap;
import loom.zspace.ZRange;
import loom.zspace.ZTensor;
import org.junit.Test;

public class NamedRangeTest implements CommonAssertions {
  @Test
  public void test_string_parse_json() {
    var is =
        new NamedRange(
            new DimensionMap(new String[] {"x", "y", "z"}),
            ZRange.fromShape(ZTensor.vector(3, 4, 5)));

    String pretty = "i[x=0:3, y=0:4, z=0:5]";
    String json =
        "{\"dimensions\":[\"x\",\"y\",\"z\"],\"range\":{\"start\":[0, 0, 0], \"end\":[3, 4, 5]}}";

    assertThat(is).hasToString(pretty);
    assertJsonEquals(is, json);

    assertThat(NamedRange.parseIndexSpace(pretty)).isEqualTo(is);
    assertThat(NamedRange.parseIndexSpace(json)).isEqualTo(is);
  }
}
