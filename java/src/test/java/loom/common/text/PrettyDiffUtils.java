package loom.common.text;

import com.google.common.base.Splitter;
import java.util.List;
import org.assertj.core.util.diff.DiffUtils;

public class PrettyDiffUtils {
  private PrettyDiffUtils() {}

  public static List<String> udiffLines(String a, String b) {
    var splitter = Splitter.on('\n').trimResults();
    var aLines = splitter.splitToList(a);
    var bLines = splitter.splitToList(b);

    var patch = DiffUtils.diff(aLines, bLines);
    var udiffLines = DiffUtils.generateUnifiedDiff(a, b, aLines, patch, 3);

    // Omit the first two lines of the udiff output, which contain '--- $a' and '+++ b'.
    return udiffLines.subList(2, udiffLines.size());
  }

  public static String indentUdiff(String prefix, String a, String b) {
    return IndentUtils.indent(prefix, String.join("\n", udiffLines(a, b)));
  }
}
