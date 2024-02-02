package org.tensortapestry.common.runtime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.CommonAssertions;

public class ReflectionUtilsTest implements CommonAssertions {

  @Getter
  @SuperBuilder
  public abstract static class Base<X, Y> {

    private final X x;
    private final Y y;
  }

  @Getter
  @SuperBuilder
  public static class Level1 extends Base<String, Integer> {

    private final String z;
  }

  @Getter
  @SuperBuilder
  public static class Level2 extends Level1 {

    private final float w;
  }

  @Value
  @AllArgsConstructor
  public static class Example {

    public int abc;

    public Example(String abc) {
      this(Integer.parseInt(abc));
    }
  }

  @Test
  public void test_newInstance() {
    assertThat(ReflectionUtils.newInstance(Example.class, "123")).isEqualTo(new Example(123));

    assertThatExceptionOfType(RuntimeException.class)
      .isThrownBy(() -> ReflectionUtils.newInstance(Example.class, new Object()))
      .withCauseInstanceOf(NoSuchMethodException.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testGetTypeArgumentsForGenericSuperclass() {
    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() ->
        ReflectionUtils.getTypeArgumentsForGenericSuperclass(
          (Class<? extends Base<?, ?>>) (Class<?>) Integer.class,
          Base.class
        )
      );
    assertThat(ReflectionUtils.getTypeArgumentsForGenericSuperclass(Level1.class, Base.class))
      .containsExactly(String.class, Integer.class);
    assertThat(ReflectionUtils.getTypeArgumentsForGenericSuperclass(Level2.class, Base.class))
      .containsExactly(String.class, Integer.class);
  }

  @Test
  public void testCheckIsSubclass() {
    @SuppressWarnings("SameReturnValue")
    interface I {
      default int foo() {
        return 2;
      }
    }

    class A {}
    class B extends A implements I {}

    assertThat(new B().foo()).isEqualTo(2);

    ReflectionUtils.checkIsSubclass(B.class, A.class);
    assertThatExceptionOfType(ClassCastException.class)
      .isThrownBy(() -> ReflectionUtils.checkIsSubclass(A.class, B.class));

    ReflectionUtils.checkIsSubclass(B.class, I.class);
    assertThatExceptionOfType(ClassCastException.class)
      .isThrownBy(() -> ReflectionUtils.checkIsSubclass(A.class, I.class));
  }
}
