package loom.common.text;

import com.google.common.base.Splitter;
import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class IndentUtils {

  public final Splitter NEWLINE_SPLITTER = Splitter.on("\n");

  /**
   * Get the length of the whitespace prefix of a string.
   *
   * @param text the string.
   * @return the length of the whitespace prefix.
   */
  public int whitespacePrefixLength(String text) {
    int k = 0;
    while (k < text.length() && Character.isWhitespace(text.charAt(k))) {
      k++;
    }
    return k;
  }

  /**
   * Split a string into lines, and remove the common indent from each line.
   *
   * <p>Also strips trailing whitespace from each line; and trailing empty lines.
   *
   * @param text the text to split.
   * @return the list of lines.
   */
  public List<String> splitAndRemoveCommonIndent(String text) {
    var lines = NEWLINE_SPLITTER.splitToList(text.stripTrailing());

    var prefixLen = lines.stream().mapToInt(IndentUtils::whitespacePrefixLength).min();
    if (prefixLen.isPresent()) {
      var k = prefixLen.getAsInt();
      if (k > 0) {
        lines = lines.stream().map(line -> line.substring(k)).toList();
      }
    }

    return lines;
  }

  /**
   * Indent a list of lines.
   *
   * @param prefix the prefix to add to each line.
   * @param lines the lines.
   * @return the indented text.
   */
  public String indent(String prefix, List<String> lines) {
    return lines.stream()
        .map(line -> (prefix + line).stripTrailing())
        .collect(Collectors.joining("\n"));
  }

  /**
   * Indent a list of lines.
   *
   * @param indent the number of spaces to indent.
   * @param lines the lines.
   * @return the indented text.
   */
  public String indent(int indent, List<String> lines) {
    return indent(" ".repeat(indent), lines);
  }

  /**
   * Indent a block of text.
   *
   * @param prefix the prefix to add to each line.
   * @param text the text.
   * @return the indented text.
   */
  public String indent(String prefix, String text) {
    return indent(prefix, NEWLINE_SPLITTER.splitToList(text));
  }

  /**
   * Indent a block of text.
   *
   * @param indent the number of spaces to indent.
   * @param text the text.
   * @return the indented text.
   */
  public String indent(int indent, String text) {
    return indent(" ".repeat(indent), text);
  }

  /**
   * Reindent a block of text.
   *
   * <p>First, the text is split into lines, and the common indent is removed from each line. Then,
   * the text is indented with the given prefix.
   *
   * @param prefix the prefix to add to each line.
   * @param text the text.
   * @return the indented text.
   */
  public String reindent(String prefix, String text) {
    return indent(prefix, splitAndRemoveCommonIndent(text));
  }

  /**
   * Reindent a block of text.
   *
   * <p>First, the text is split into lines, and the common indent is removed from each line. Then,
   * the text is indented with the given number of spaces.
   *
   * @param indent the number of spaces to indent.
   * @param text the text.
   * @return the indented text.
   */
  public String reindent(int indent, String text) {
    return indent(indent, splitAndRemoveCommonIndent(text));
  }
}
