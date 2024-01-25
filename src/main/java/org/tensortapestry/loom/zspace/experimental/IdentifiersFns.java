package org.tensortapestry.loom.zspace.experimental;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import lombok.experimental.UtilityClass;

/** Utility functions for identifiers. */
@UtilityClass
public final class IdentifiersFns {

  public final Pattern ATOMIC_IDENTIFIER_PATTERN = Pattern.compile("[a-zA-Z][a-zA-Z0-9_]*");

  public final Pattern DOTTED_IDENTIFIER_PATTERN = Pattern.compile(
    String.format(
      "%s(\\.%s)*",
      ATOMIC_IDENTIFIER_PATTERN.pattern(),
      ATOMIC_IDENTIFIER_PATTERN.pattern()
    )
  );

  /**
   * Validate an atomic identifier.
   *
   * @param name the identifier to validate.
   * @return the identifier.
   * @throws IllegalArgumentException if the identifier is invalid.
   */
  @CanIgnoreReturnValue
  @Nonnull
  public String validAtomicIdentifier(@Nonnull String name) {
    if (!ATOMIC_IDENTIFIER_PATTERN.matcher(name).matches()) {
      throw new IllegalArgumentException("invalid atomic identifier: \"%s\"".formatted(name));
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
  @CanIgnoreReturnValue
  @Nonnull
  public String validDottedIdentifier(@Nonnull String name) {
    if (!DOTTED_IDENTIFIER_PATTERN.matcher(name).matches()) {
      throw new IllegalArgumentException("invalid dotted identifier: \"%s\"".formatted(name));
    }
    return name;
  }

  /**
   * Split a dotted identifier into its components.
   *
   * @param name the identifier to split.
   * @return the components of the identifier.
   */
  @Nonnull
  public String[] splitDottedIdentifier(@Nonnull String name) {
    validDottedIdentifier(name);
    return name.split("\\.");
  }
}
