package org.tensortapestry.common.text;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.CommonAssertions;

public class TextUtilsTest implements CommonAssertions {

  @Test
  public void test_longestCommonPrefix() {
    assertThat(TextUtils.longestCommonPrefix(List.of("abc", "abcd", "abcde"))).isEqualTo("abc");
    assertThat(TextUtils.longestCommonPrefix(List.of("abc", "abc", "abc"))).isEqualTo("abc");
    assertThat(TextUtils.longestCommonPrefix(List.of("abc", "def", "ghi"))).isEqualTo("");
    assertThat(TextUtils.longestCommonPrefix(List.of())).isEqualTo("");
  }
}
