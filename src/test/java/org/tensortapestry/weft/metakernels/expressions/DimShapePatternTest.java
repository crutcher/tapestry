package org.tensortapestry.weft.metakernels.expressions;

import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.CommonAssertions;

public class DimShapePatternTest implements CommonAssertions {

  // *batch, shape=(height, width), channels

  @Data
  @RequiredArgsConstructor
  public abstract static class ShapePattern {

    private final String name;
  }

  @Value
  @EqualsAndHashCode(callSuper = false)
  public static class NamedDim extends ShapePattern {

    public NamedDim(String name) {
      super(name);
    }

    @Override
    @Nonnull
    public String toString() {
      return getName();
    }
  }

  @Value
  @EqualsAndHashCode(callSuper = false)
  public static class DimRange extends ShapePattern {

    boolean allowEmpty;

    public static DimRange star(String name) {
      return new DimRange(name, true);
    }

    public static DimRange plus(String name) {
      return new DimRange(name, false);
    }

    public DimRange(String name, boolean allowEmpty) {
      super(name);
      this.allowEmpty = allowEmpty;
    }

    @Override
    @Nonnull
    public String toString() {
      return (allowEmpty ? "*" : "+") + getName();
    }
  }

  @Value
  @EqualsAndHashCode(callSuper = false)
  public static class DimGroup extends ShapePattern {

    List<ShapePattern> children;

    public DimGroup(String name, List<ShapePattern> children) {
      super(name);
      this.children = List.copyOf(children);
    }

    @Override
    @Nonnull
    public String toString() {
      return (
        getName() +
        "=" +
        children.stream().map(Object::toString).collect(Collectors.joining(", ", "[", "]"))
      );
    }
  }

  public static List<ShapePattern> validatePatternList(List<ShapePattern> pl) {
    Set<String> names = new HashSet<>();
    LinkedHashSet<String> duplicates = new LinkedHashSet<>();
    List<ShapePattern> ranges = new ArrayList<>();

    List<ShapePattern> queue = new ArrayList<>(pl);
    while (!queue.isEmpty()) {
      var cur = queue.removeFirst();
      if (!names.add(cur.name)) {
        duplicates.add(cur.name);
      }
      if (cur instanceof DimRange range) {
        ranges.add(range);
      }
      if (cur instanceof DimGroup group) {
        queue.addAll(0, group.children);
      }
    }

    if (ranges.size() > 1) {
      throw new IllegalArgumentException("More than one range: %s :: %s".formatted(ranges, pl));
    }

    if (!duplicates.isEmpty()) {
      throw new IllegalArgumentException("Duplicate names: %s :: %s".formatted(duplicates, pl));
    }

    return pl;
  }

  public static class ParseVisitor extends DimShapePatternBaseVisitor<List<ShapePattern>> {

    @Override
    public List<ShapePattern> visitPatternList(DimShapePatternParser.PatternListContext ctx) {
      return ctx.pattern().stream().flatMap(p -> visit(p).stream()).toList();
    }

    @Override
    public List<ShapePattern> visitGroupPattern(DimShapePatternParser.GroupPatternContext ctx) {
      return List.of(new DimGroup(ctx.name.getText(), ctx.patternList().accept(this)));
    }

    @Override
    public List<ShapePattern> visitStarPattern(DimShapePatternParser.StarPatternContext ctx) {
      return List.of(DimRange.star(ctx.name.getText()));
    }

    @Override
    public List<ShapePattern> visitPlusPattern(DimShapePatternParser.PlusPatternContext ctx) {
      return List.of(DimRange.plus(ctx.name.getText()));
    }

    @Override
    public List<ShapePattern> visitSingleDim(DimShapePatternParser.SingleDimContext ctx) {
      return List.of(new NamedDim(ctx.ID().getText()));
    }
  }

  public static List<ShapePattern> parsePatternList(String source) {
    var lexer = new DimShapePatternLexer(CharStreams.fromString(source));
    var tokens = new CommonTokenStream(lexer);
    var parser = new DimShapePatternParser(tokens);
    var tree = parser.patternList();

    var visitor = new ParseVisitor();
    return validatePatternList(visitor.visit(tree));
  }

  @Test
  public void test_parse() {
    var source = "*batch, shape=[height, width], channels";
    var pl = parsePatternList(source);

    assertThat(pl)
      .containsExactly(
        DimRange.star("batch"),
        new DimGroup("shape", List.of(new NamedDim("height"), new NamedDim("width"))),
        new NamedDim("channels")
      );
  }

  @Test
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public void test_too_many_ranges() {
    var source = "*batch, shape=[height, +width], channels, *extra";
    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> parsePatternList(source))
      .withMessage(
        "More than one range: [*batch, +width, *extra] :: [*batch, shape=[height, +width], channels, *extra]"
      );
  }

  @Test
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public void test_duplicate_names() {
    var source = "*batch, shape=[height, width], channels, batch, width";
    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> parsePatternList(source))
      .withMessage("Duplicate names: [batch, width] :: [*batch, shape=[height, width], channels, batch, width]");

  }
}
