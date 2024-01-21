package loom.common.text;

import java.util.List;
import loom.testing.BaseTestClass;
import org.junit.Test;

public class TextUtilsTest extends BaseTestClass {

  @Test
  public void test_longestCommonPrefix() {
    assertThat(TextUtils.longestCommonPrefix(List.of("abc", "abcd", "abcde"))).isEqualTo("abc");
    assertThat(TextUtils.longestCommonPrefix(List.of("abc", "abc", "abc"))).isEqualTo("abc");
    assertThat(TextUtils.longestCommonPrefix(List.of("abc", "def", "ghi"))).isEqualTo("");
    assertThat(TextUtils.longestCommonPrefix(List.of())).isEqualTo("");
  }
}
