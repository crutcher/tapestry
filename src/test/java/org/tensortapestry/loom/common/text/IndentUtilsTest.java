package org.tensortapestry.loom.common.text;

import java.util.ArrayList;
import org.tensortapestry.loom.testing.BaseTestClass;
import org.junit.Test;

public class IndentUtilsTest extends BaseTestClass {

  @Test
  public void testWhitespacePrefixLength() {
    assertThat(IndentUtils.whitespacePrefixLength("foo")).isEqualTo(0);
    assertThat(IndentUtils.whitespacePrefixLength(" foo")).isEqualTo(1);
    assertThat(IndentUtils.whitespacePrefixLength("  foo")).isEqualTo(2);
    assertThat(IndentUtils.whitespacePrefixLength("   foo")).isEqualTo(3);
    assertThat(IndentUtils.whitespacePrefixLength("    foo")).isEqualTo(4);
    assertThat(IndentUtils.whitespacePrefixLength("     foo")).isEqualTo(5);
  }

  @Test
  public void testSplitAndRemoveCommonIndent() {
    var sourceLines = new ArrayList<String>();
    sourceLines.add("   foo");
    sourceLines.add("    * bar");
    sourceLines.add("");
    var text = String.join("\n", sourceLines);

    var lines = IndentUtils.splitAndRemoveCommonIndent(text);
    assertThat(lines).containsExactly("foo", " * bar");
  }

  @Test
  public void testIndent_Prefix_List() {
    var lines = new ArrayList<String>();
    lines.add("foo");
    lines.add("bar");
    var text = IndentUtils.indent("|  ", lines);
    assertThat(text).isEqualTo("|  foo\n|  bar");
  }

  @Test
  public void testIndent_Indent_List() {
    var lines = new ArrayList<String>();
    lines.add("foo");
    lines.add("bar");
    var text = IndentUtils.indent(2, lines);
    assertThat(text).isEqualTo("  foo\n  bar");
  }

  @Test
  public void testIndent_Prefix_String() {
    var text = IndentUtils.indent("|  ", "foo\n * bar");
    assertThat(text).isEqualTo("|  foo\n|   * bar");
  }

  @Test
  public void testIndent_Indent_String() {
    var text = IndentUtils.indent(2, "foo\n * bar");
    assertThat(text).isEqualTo("  foo\n   * bar");
  }

  @Test
  public void testReindent_Indent() {
    var text = IndentUtils.reindent(3, "  foo\n   * bar");
    assertThat(text).isEqualTo("   foo\n    * bar");
  }

  @Test
  public void testReindent_Prefix() {
    var text = IndentUtils.reindent("|> ", "  foo\n   * bar");
    assertThat(text).isEqualTo("|> foo\n|>  * bar");
  }
}
