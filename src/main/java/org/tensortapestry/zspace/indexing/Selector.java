package org.tensortapestry.zspace.indexing;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.tensortapestry.common.text.TextUtils;

public abstract class Selector {

  /**
   * Parse a selector expression.
   *
   * @param selector the selector expression.
   * @return the selectors.
   */
  @Nonnull
  public static List<Selector> parseSelectors(@Nonnull String selector) {
    List<Selector> selectors = new ArrayList<>();
    for (var atom : TextUtils.COMMA_SPLITTER.split(selector)) {
      selectors.add(parseSelectorAtom(atom));
    }
    return selectors;
  }

  /**
   * Parse a selector atom.
   *
   * @param atom the atom.
   * @return the selector.
   */
  @Nonnull
  public static Selector parseSelectorAtom(@Nonnull String atom) {
    atom = atom.trim();

    if (atom.equals("...")) {
      return new Ellipsis();
    } else if (atom.equals("+")) {
      return new NewAxis();
    } else if (atom.contains(":")) {
      var parts = TextUtils.COLON_SPLITTER.split(atom).iterator();
      var start = parts.next();
      var stop = parts.next();
      var step = parts.hasNext() ? parts.next() : null;
      return new Slice(
        start.isEmpty() ? null : Integer.parseInt(start),
        stop.isEmpty() ? null : Integer.parseInt(stop),
        step == null || step.isEmpty() ? null : Integer.parseInt(step)
      );
    } else {
      return new Index(Integer.parseInt(atom));
    }
  }
}
