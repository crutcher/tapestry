package org.tensortapestry.common.testing;

import com.google.common.base.Splitter;
import java.util.List;
import org.assertj.core.util.diff.DiffUtils;
import org.tensortapestry.common.text.IndentUtils;

public class PrettyDiffUtils {

  private PrettyDiffUtils() {}

  public static List<String> udiffLines(String a, String b) {
    var splitter = Splitter.on('\n').trimResults();
    var aLines = splitter.splitToList(a);
    var bLines = splitter.splitToList(b);

    var patch = DiffUtils.diff(aLines, bLines);
    var udiffLines = DiffUtils.generateUnifiedDiff(a, b, aLines, patch, 3);

    // Omit the first two lines of the udiff output, which contain '--- $a' and '+++ b'.
    if (udiffLines.size() > 2) {
      return udiffLines.subList(2, udiffLines.size());
    }
    return udiffLines;
  }

  public static String indentUdiff(String prefix, String a, String b) {
    return IndentUtils.indent(prefix, String.join("\n", udiffLines(a, b)));
  }
}
