package loom.common;

import java.util.regex.Pattern;

public class NamePatterns {
  private NamePatterns() {}

  public static final Pattern IDENTIFIER_ATOM = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");
  public static final Pattern DOTTED_IDENTIFIER =
      Pattern.compile(IDENTIFIER_ATOM + "(\\." + IDENTIFIER_ATOM + ")*");
}
