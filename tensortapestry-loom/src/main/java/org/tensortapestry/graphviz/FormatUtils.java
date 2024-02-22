package org.tensortapestry.graphviz;

import java.awt.*;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

import lombok.experimental.UtilityClass;

@UtilityClass
public class FormatUtils {

  @Nonnull
  public String escape(@Nonnull String s) {
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  @Nonnull
  public String colorToRgbaString(Color c) {
    var s = "#%02x%02x%02x".formatted(c.getRed(), c.getGreen(), c.getBlue());
    if (c.getAlpha() != 255) {
      s += ":%02x".formatted(c.getAlpha());
    }
    return s;
  }

  @Nonnull
  public String formatValue(@Nonnull Object value) {
    return switch (value) {
      case String s -> "\"%s\"".formatted(escape(s));
      case Color c -> "\"%s\"".formatted(colorToRgbaString(c));
      default -> value.toString();
    };
  }

  @Nonnull
  public String formatAttribute(@Nonnull String key, @Nonnull Object value) {
    return key + "=" + formatValue(value);
  }

  @Nonnull
  public String joinAttributes(@Nonnull AbstractGraphvizAttributeMap<?, ?> attributes, String sep) {
    return attributes
      .entrySet()
      .stream()
      .map(e -> formatAttribute(e.getKey(), e.getValue()))
      .collect(Collectors.joining(sep));
  }

  @Nonnull
  public String formatContextAttributes(
    @Nonnull AbstractGraphvizAttributeMap<?, ?> attributes,
    int indent
  ) {
    if (attributes.isEmpty()) {
      return "";
    }
    var prefix = "  ".repeat(indent);
    return prefix + joinAttributes(attributes, ";\n" + prefix) + ";\n";
  }

  @Nonnull
  public String formatItemAttributes(
    @Nonnull AbstractGraphvizAttributeMap<?, ?> attributes,
    int indent
  ) {
    if (attributes.isEmpty()) {
      return "";
    }

    var prefix = "  ".repeat(indent);
    var itemPrefix = "  ".repeat(indent + 2);

    return (
      " [\n" + itemPrefix + joinAttributes(attributes, ",\n" + itemPrefix) + "\n" + prefix + "]"
    );
  }
}
