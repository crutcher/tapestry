package loom.graph;

import loom.testing.CommonAssertions;
import org.junit.Test;

public class NSNameTest implements CommonAssertions {
  @Test
  public void testParse() {
    assertThat(NSName.parse("{http://foo.bar}baz.quux"))
        .isEqualTo(new NSName("http://foo.bar", "baz.quux"));

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> new NSName("foo.", "baz"));
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> new NSName("foo", "baz."));
  }

  @Test
  public void testJson() {
    assertJsonEquals(new NSName("http://foo.bar", "baz.quux"), "\"{http://foo.bar}baz.quux\"");
  }
}
