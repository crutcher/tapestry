package org.tensortapestry.loom.zspace.serialization;

import org.assertj.core.api.WithAssertions;
import org.junit.Test;

public class ParseUtilTest implements WithAssertions {

  @Test
  public void test_splitCommas() {
    assertThat(ParseUtil.splitCommas("")).containsExactly("");
    assertThat(ParseUtil.splitCommas("a, b , c")).containsExactly("a", "b", "c");
    assertThat(ParseUtil.splitCommas("a ")).containsExactly("a");
  }

  @Test
  public void test_splitColons() {
    assertThat(ParseUtil.splitColons("")).containsExactly("");
    assertThat(ParseUtil.splitColons("a: b : c")).containsExactly("a", "b", "c");
    assertThat(ParseUtil.splitColons("a ")).containsExactly("a");
  }
}
