package loom.common.lazy;

import loom.testing.BaseTestClass;
import org.junit.Test;

public class ThunkTest extends BaseTestClass {
  @Test
  public void test_lazy() {
    var t = Thunk.of(() -> "abc" + "xyz");
    var v = t.get();
    assertThat(t.get()).isEqualTo("abcxyz").isSameAs(v);
  }

  @Test
  public void test_fixed() {
    var t = Thunk.fixed("abc");
    var v = t.get();
    assertThat(t.get()).isEqualTo("abc").isSameAs(v);
  }

  @Test
  public void test_error() {
    var t =
        new Thunk<>(
            () -> {
              throw new RuntimeException("boo");
            });
    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(t::get)
        .withMessageContaining("boo");
    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(t::get)
        .withMessageContaining("boo");
  }
}
