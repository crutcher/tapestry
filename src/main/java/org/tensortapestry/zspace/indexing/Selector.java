package org.tensortapestry.zspace.indexing;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.tensortapestry.common.text.TextUtils;

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
    public static Slice slice(@Nullable Integer start, @Nullable Integer stop, @Nullable Integer step) {
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
