package loom.expressions;

import java.util.regex.Pattern;

/**
 * Utility functions for identifiers.
 */
public final class IdentifiersFns {
    private IdentifiersFns() {
    }

    public static final Pattern ATOMIC_IDENTIFIER_PATTERN = Pattern.compile("[a-zA-Z][a-zA-Z0-9_]*");

    public static final Pattern DOTTED_IDENTIFIER_PATTERN =
            Pattern.compile(
                    String.format(
                            "%s(\\.%s)*",
                            ATOMIC_IDENTIFIER_PATTERN.pattern(), ATOMIC_IDENTIFIER_PATTERN.pattern()));

    /**
     * Validate an atomic identifier.
     *
     * @param name the identifier to validate.
     * @return the identifier.
     * @throws IllegalArgumentException if the identifier is invalid.
     */
    public static String validAtomicIdentifier(String name) {
        if (!ATOMIC_IDENTIFIER_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("invalid atomic identifier: " + name);
        }
        return name;
    }

    /**
     * Validate a dotted identifier.
     *
     * @param name the identifier to validate.
     * @return the identifier.
     * @throws IllegalArgumentException if the identifier is invalid.
     */
    public static String validDottedIdentifier(String name) {
        if (!DOTTED_IDENTIFIER_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("invalid dotted identifier: " + name);
        }
        return name;
    }

    /**
     * Split a dotted identifier into its components.
     *
     * @param name the identifier to split.
     * @return the components of the identifier.
     */
    public static String[] splitDottedIdentifier(String name) {
        validDottedIdentifier(name);
        return name.split("\\.");
    }
}
