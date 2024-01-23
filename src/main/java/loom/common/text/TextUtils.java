package loom.common.text;

import com.google.common.base.Splitter;
import java.util.List;

public class TextUtils {

  public static final Splitter COMMA_SPLITTER = Splitter.on(",");
  public static final Splitter COLON_SPLITTER = Splitter.on(':');

  /**
   * Returns the longest common prefix of the given strings.
   *
   * @param strs the strings
   * @return the longest common prefix
   */
  public static String longestCommonPrefix(List<String> strs) {
    if (strs.isEmpty()) {
      return "";
    }
    String prefix = strs.getFirst();
    for (int i = 1; i < strs.size(); i++) {
      while (strs.get(i).indexOf(prefix) != 0) {
        prefix = prefix.substring(0, prefix.length() - 1);
        if (prefix.isEmpty()) {
          return "";
        }
      }
    }
    return prefix;
  }
}
