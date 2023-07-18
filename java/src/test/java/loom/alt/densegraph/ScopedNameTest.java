package loom.alt.densegraph;

import loom.testing.CommonAssertions;
import org.junit.Test;

public class ScopedNameTest implements CommonAssertions {
  @Test
  public void testParse() {
    assertThat(ScopedName.parse("foo.bar/baz.quux"))
        .isEqualTo(new ScopedName("foo.bar", "baz.quux"));

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> new ScopedName("foo.", "baz"));
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> new ScopedName("foo", "baz."));

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> ScopedName.parse("foo./baz.quux"));
  }

  @Test
  public void testJson() {
    assertJsonEquals(new ScopedName("foo.bar", "baz.quux"), "\"foo.bar/baz.quux\"");
  }
}
