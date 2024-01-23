package loom.common.lazy;

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

    assertThat(String.format("%s", lazyString)).isEqualTo("Hello World");
    assertThat("%s".formatted(lazyString)).isEqualTo("Hello World");
  }
}
