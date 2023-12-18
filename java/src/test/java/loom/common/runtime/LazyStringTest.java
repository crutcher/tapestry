package loom.common.runtime;

import loom.testing.BaseTestClass;
import org.junit.Test;

public class LazyStringTest extends BaseTestClass {
  @Test
  public void test_format() {
    var lazyString = LazyString.format("Hello %s", "World");
    var str = lazyString.toString();
    assertThat(str).isEqualTo("Hello World");
    assertThat(lazyString).hasToString("Hello World");
    assertThat(lazyString.get()).isSameAs(str);

    assertThat(lazyString.length()).isEqualTo(str.length());
    assertThat(lazyString.replace("World", "Banana")).isEqualTo("Hello Banana");
  }
}
