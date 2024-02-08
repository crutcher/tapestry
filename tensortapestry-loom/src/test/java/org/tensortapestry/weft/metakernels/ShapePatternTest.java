package org.tensortapestry.weft.metakernels;

import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.tensortapestry.common.collections.EnumerationUtils;
import org.tensortapestry.common.testing.CommonAssertions;
import org.tensortapestry.common.text.TextUtils;
import org.tensortapestry.zspace.indexing.IndexingFns;

public class ShapePatternTest implements CommonAssertions {

  // "batch..., height, width, channels"

  public static String SHAPE_REGEX = "[a-zA-Z_][a-zA-Z0-9_]*[+*]?";
  public static Pattern SHAPE_PATTERN = Pattern.compile(SHAPE_REGEX);

  public static Map<String, List<Integer>> matchDimIndexPattern(String pattern, int numDims) {
    var keys = new ArrayList<String>();

    int expansionIndex = -1;
    int minCount = 0;
    for (var p : EnumerationUtils.enumerate(TextUtils.COMMA_SPLITTER.split(pattern))) {
      var idx = p.getKey();
      var item = p.getValue();

      item = item.trim();

      if (!SHAPE_PATTERN.matcher(item).matches()) {
        throw new IllegalArgumentException("Invalid shape pattern: " + pattern);
      }

      var last = item.charAt(item.length() - 1);
      if (last == '+' || last == '*') {
        if (expansionIndex != -1) {
          throw new IllegalArgumentException("More than one expansion pattern: " + pattern);
        }
        expansionIndex = idx;
        if (last == '+') {
          minCount = 1;
        }
        item = item.substring(0, item.length() - 1);
      }
      keys.add(item);
    }

    if (
      (expansionIndex == 1 && keys.size() != numDims) ||
      (expansionIndex != -1 && keys.size() < numDims + minCount - 1)
    ) {
      throw new IllegalArgumentException(
        "Mismatched pattern and dims (%d): %s".formatted(numDims, pattern)
      );
    }

    var result = new HashMap<String, List<Integer>>();

    if (expansionIndex == -1) {
      for (var p : EnumerationUtils.enumerate(keys)) {
        result.put(p.getValue(), List.of(p.getKey()));
      }
    } else {
      int expansionSize = numDims - (keys.size() - 1);

      for (var p : EnumerationUtils.enumerate(keys.subList(0, expansionIndex))) {
        result.put(p.getValue(), List.of(p.getKey()));
      }
      for (var p : EnumerationUtils
        .enumerate(keys.subList(expansionIndex + expansionSize - 1, keys.size()))
        .withOffset(expansionIndex + expansionSize)) {
        result.put(p.getValue(), List.of(p.getKey()));
      }
      int offset = expansionIndex;
      result.put(
        keys.get(expansionIndex),
        Arrays.stream(IndexingFns.iota(expansionSize)).map(i -> i + offset).boxed().toList()
      );
    }

    return Map.copyOf(result);
  }

  @Test
  public void test() {
    assertThat(matchDimIndexPattern("batch*, height, width, channels", 5))
      .isEqualTo(
        Map.of(
          "batch",
          List.of(0, 1),
          "height",
          List.of(2),
          "width",
          List.of(3),
          "channels",
          List.of(4)
        )
      );
  }
}
