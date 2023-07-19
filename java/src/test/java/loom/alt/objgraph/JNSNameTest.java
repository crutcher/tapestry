package loom.alt.objgraph;

import loom.testing.CommonAssertions;
import org.junit.Test;

public class JNSNameTest implements CommonAssertions {
  @Test
  public void testParse() {
    assertThat(JNSName.parse("{http://foo.bar}baz.quux"))
        .isEqualTo(new JNSName("http://foo.bar", "baz.quux"));

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> new JNSName("foo.", "baz"));
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> new JNSName("foo", "baz."));
  }

  @Test
  public void testJson() {
    assertJsonEquals(new JNSName("http://foo.bar", "baz.quux"), "\"{http://foo.bar}baz.quux\"");
  }
}
