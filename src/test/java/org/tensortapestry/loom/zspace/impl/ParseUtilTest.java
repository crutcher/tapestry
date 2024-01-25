package org.tensortapestry.loom.zspace.impl;

import org.junit.Test;
import org.tensortapestry.loom.zspace.experimental.ZSpaceTestAssertions;

public class ParseUtilTest implements ZSpaceTestAssertions {

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
