package org.tensortapestry.zspace.indexing;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.*;
import org.tensortapestry.common.text.TextUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class Selector {

  /**
   * Create a new ellipsis selector.
   *
   * @return the new ellipsis selector.
   */
  public static Ellipsis ellipsis() {
    return new Ellipsis();
  }

  /**
   * Create a new axis selector.
   *
   * @return the new axis selector.
   */
  public static NewAxis newAxis() {
    return new NewAxis();
  }

  /**
   * Create a new axis selector, as a broadcast dim of the given size.
   *
   * @param size the size of the new axis.
   * @return the new axis selector.
   */
  public static NewAxis newAxis(int size) {
    return new NewAxis(size);
  }

  /**
   * Create a new index selector.
   *
   * @param index the index.
   * @return the new index selector.
   */
  public static Index index(int index) {
    return new Index(index);
  }

  /**
   * Create a new full slice selector.
   *
   * @return the new slice selector.
   */
  public static Slice slice() {
    return new Slice(null, null, null);
  }

  /**
   * Create a new slice selector.
   *
   * @param start the start.
   * @param stop the stop.
   * @return the new slice selector.
   */
  public static Slice slice(@Nullable Integer start, @Nullable Integer stop) {
    return new Slice(start, stop, null);
  }

  /**
   * Create a new slice selector.
   *
   * @param start the start.
   * @param stop the stop.
   * @param step the step.
   * @return the new slice selector.
   */
  public static Slice slice(
    @Nullable Integer start,
    @Nullable Integer stop,
    @Nullable Integer step
  ) {
    return new Slice(start, stop, step);
  }

  /**
   * Create a new slice selector builder.
   *
   * @return the new slice selector builder.
   */
  public static Slice.SliceBuilder sliceBuilder() {
    return Slice.builder();
  }

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
    } else if (atom.startsWith("+")) {
      return new NewAxis(Integer.parseInt(atom.substring(1)));
    } else if (atom.contains(":")) {
      var parts = TextUtils.COLON_SPLITTER.trimResults().split(atom).iterator();
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

  @Value
  @EqualsAndHashCode(callSuper = false)
  public static class Ellipsis extends Selector {

    @Override
    public String toString() {
      return "...";
    }
  }

  @Value
  @EqualsAndHashCode(callSuper = false)
  public static class Index extends Selector {

    int index;

    public Index(int index) {
      this.index = index;
    }

    @Override
    public String toString() {
      return Integer.toString(index);
    }
  }

  @Value
  @EqualsAndHashCode(callSuper = false)
  public static class NewAxis extends Selector {

    int size;

    public NewAxis() {
      this(1);
    }

    public NewAxis(int size) {
      if (size < 1) {
        throw new IllegalArgumentException("Size must be greater than 0: " + size);
      }
      this.size = size;
    }

    @Override
    public String toString() {
      if (size > 1) {
        return "+" + size;
      } else {
        return "+";
      }
    }
  }

  @Value
  @EqualsAndHashCode(callSuper = false)
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Slice extends Selector {

    @Builder.Default
    @Nullable Integer start = null;

    @Builder.Default
    @Nullable Integer end = null;

    @Builder.Default
    @Nullable Integer step = null;

    public Slice(@Nullable Integer start, @Nullable Integer end) {
      this(start, end, null);
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public String toString() {
      var parts = new ArrayList<String>();
      if (start != null) {
        parts.add(start.toString());
      }
      parts.add(":");
      if (end != null) {
        parts.add(end.toString());
      }
      if (step != null) {
        parts.add(":");
        parts.add(step.toString());
      }
      return String.join("", parts);
    }
  }
}
