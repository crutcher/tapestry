package org.tensortapestry.common.runtime;

import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.CommonAssertions;

@SuppressWarnings("ConstantConditions")
public class CheckThatTest implements CommonAssertions {

  @Test
  public void test_valueIsNotNull() {
    CheckThat.valueIsNotNull("foo", RuntimeException.class, "bar");
    CheckThat.valueIsNotNull("foo", RuntimeException.class, "bar %s", "baz");

    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> CheckThat.valueIsNotNull(null, RuntimeException.class, "bar"))
        .withMessage("bar");

    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> CheckThat.valueIsNotNull(null, RuntimeException.class, "bar %s", "baz"))
        .withMessage("bar baz");
  }
}
