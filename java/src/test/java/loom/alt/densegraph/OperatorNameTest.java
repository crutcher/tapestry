package loom.alt.densegraph;

import loom.testing.CommonAssertions;
import org.junit.Test;

public class OperatorNameTest implements CommonAssertions {
  @Test
  public void testParse() {
    assertThat(OperatorName.parse("foo.bar/baz.quux"))
        .isEqualTo(new OperatorName("foo.bar", "baz.quux"));

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> new OperatorName("foo.", "baz"));
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> new OperatorName("foo", "baz."));

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> OperatorName.parse("foo./baz.quux"));
  }

  @Test
  public void testJson() {
    assertJsonEquals(new OperatorName("foo.bar", "baz.quux"), "\"foo.bar/baz.quux\"");
  }
}
