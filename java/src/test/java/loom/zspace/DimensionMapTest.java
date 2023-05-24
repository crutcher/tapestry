package loom.zspace;

import loom.testing.CommonAssertions;
import org.junit.Test;

public class DimensionMapTest implements CommonAssertions {
  @Test
  public void test_string_parse_json() {
    var dm = new DimensionMap(new String[] {"x", "y", "z"});

    String json = "[\"x\",\"y\",\"z\"]";
    assertThat(dm).hasToString(json);
    assertJsonEquals(dm, json);

    assertThat(DimensionMap.parseDimensionMap(json)).isEqualTo(dm);
  }

  @Test
  public void test_bad_names() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> new DimensionMap(new String[] {"x", "9"}));

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> new DimensionMap(new String[] {"x", ""}));
  }

  @Test
  public void test_lookup() {
    var dm = new DimensionMap(new String[] {"x", "y", "z"});

    assertThat(dm.indexOf("y")).isEqualTo(1);
    assertThat(dm.nameOf(1)).isEqualTo("y");

    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> dm.indexOf("w"));

    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> dm.nameOf(-1));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> dm.nameOf(3));
  }
}
