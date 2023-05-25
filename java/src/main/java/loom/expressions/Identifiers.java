package loom.expressions;

import java.util.regex.Pattern;

public class Identifiers {
  private Identifiers() {}

  public static Pattern ATOMIC_IDENTIFIER_PATTERN = Pattern.compile("[a-zA-Z][a-zA-Z0-9_]*");
  public static Pattern DOTTED_IDENTIFIER_PATTERN =
      Pattern.compile("[a-zA-Z][a-zA-Z0-9_]*(.[a-zA-Z][a-zA-Z0-9_]*)*");

  public static String validAtomicIdentifier(String name) {
    if (!ATOMIC_IDENTIFIER_PATTERN.matcher(name).matches()) {
      throw new IllegalArgumentException("invalid atomic identifier: " + name);
    }
    return name;
  }

  public static String validDottedIdentifier(String name) {
    if (!DOTTED_IDENTIFIER_PATTERN.matcher(name).matches()) {
      throw new IllegalArgumentException("invalid dotted identifier: " + name);
    }
    return name;
  }
}
