package org.tensortapestry.weft.metakernels.expressions;

import com.google.common.annotations.VisibleForTesting;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.tensortapestry.common.collections.EnumerationUtils;
import org.tensortapestry.zspace.indexing.IndexingFns;

@Value
public class DimShapeMatcher {

  @Data
  @RequiredArgsConstructor
  public abstract static class ShapePattern {

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

      @Override
      Stream<ShapePattern> flatLeaves() {
        return Stream.of(this);
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

      @Override
      Stream<ShapePattern> flatLeaves() {
        return Stream.of(this);
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

      @Override
      Stream<ShapePattern> flatLeaves() {
        return children.stream().flatMap(ShapePattern::flatLeaves);
      }
    }

    private final String name;

    abstract Stream<ShapePattern> flatLeaves();
  }

  @Data
  @SuperBuilder
  public static class DimGroupBase {

    int size;

    @Singular
    @Nonnull
    Map<String, Integer> dims;

    @Singular
    @Nonnull
    Map<String, List<Integer>> groups;
  }

  @Value
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  public static class DimMatchIndex extends DimGroupBase {

    public DimShapeIndex toShapeIndex(int[] shape) {
      return DimShapeIndex
        .builder()
        .size(size)
        .dims(
          dims
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> shape[e.getValue()]))
        )
        .groups(
          groups
            .entrySet()
            .stream()
            .collect(
              Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().stream().map(i -> shape[i]).toList()
              )
            )
        )
        .build();
    }
  }

  @Value
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  public static class DimShapeIndex extends DimGroupBase {}

  private Stream<ShapePattern> flatLeaves() {
    return patterns.stream().flatMap(ShapePattern::flatLeaves);
  }

  public DimMatchIndex layout(int numDims) {
    Map<String, Integer> dims = new LinkedHashMap<>();
    Map<String, List<Integer>> groups = new LinkedHashMap<>();

    var leaves = flatLeaves().toList();

    int minSize = leaves.size();
    int expansionIndex = -1;

    var optionalRange = EnumerationUtils
      .enumerate(leaves)
      .stream()
      .filter(e -> e.getValue() instanceof ShapePattern.DimRange)
      .findFirst();
    if (optionalRange.isPresent()) {
      var idx = optionalRange.get().getKey();
      var range = (ShapePattern.DimRange) optionalRange.get().getValue();

      expansionIndex = idx;
      if (range.allowEmpty) {
        minSize--;
      }
    }

    if (
      (expansionIndex == -1 && numDims != leaves.size()) ||
      (expansionIndex != -1 && numDims < minSize)
    ) {
      throw new IllegalArgumentException(
        "Mismatched pattern and dims (%d): %s".formatted(numDims, this)
      );
    }

    if (expansionIndex == -1) {
      for (var p : EnumerationUtils.enumerate(leaves)) {
        dims.put(p.getValue().getName(), p.getKey());
      }
    } else {
      int expansionSize = numDims - (leaves.size() - 1);

      for (var p : EnumerationUtils.enumerate(leaves.subList(0, expansionIndex))) {
        dims.put(p.getValue().getName(), p.getKey());
      }
      for (var p : EnumerationUtils
        .enumerate(leaves.subList(expansionIndex + 1, leaves.size()))
        .withOffset(expansionIndex + expansionSize)) {
        dims.put(p.getValue().getName(), p.getKey());
      }
      int offset = expansionIndex;
      groups.put(
        leaves.get(expansionIndex).getName(),
        Arrays.stream(IndexingFns.iota(expansionSize)).map(i -> i + offset).boxed().toList()
      );
    }

    List<ShapePattern.DimGroup> schedule = new ArrayList<>();
    List<ShapePattern> queue = new ArrayList<>(patterns);
    while (!queue.isEmpty()) {
      var cur = queue.removeFirst();
      if (cur instanceof ShapePattern.DimGroup group) {
        schedule.addFirst(group);
        queue.addAll(0, group.children);
      }
    }

    while (!schedule.isEmpty()) {
      var cur = schedule.removeFirst();
      List<Integer> indexes = new ArrayList<>();
      for (var p : cur.children) {
        if (p instanceof ShapePattern.NamedDim dim) {
          indexes.add(dims.get(dim.getName()));
        } else {
          indexes.addAll(groups.get(p.getName()));
        }
      }
      groups.put(cur.getName(), List.copyOf(indexes));
    }

    return DimMatchIndex.builder().size(numDims).dims(dims).groups(groups).build();
  }

  @VisibleForTesting
  static class ParseVisitor extends DimShapePatternBaseVisitor<List<ShapePattern>> {

    @Override
    public List<ShapePattern> visitPatternList(DimShapePatternParser.PatternListContext ctx) {
      return ctx.pattern().stream().flatMap(p -> visit(p).stream()).toList();
    }

    @Override
    public List<ShapePattern> visitGroupPattern(DimShapePatternParser.GroupPatternContext ctx) {
      return List.of(new ShapePattern.DimGroup(ctx.name.getText(), ctx.patternList().accept(this)));
    }

    @Override
    public List<ShapePattern> visitStarPattern(DimShapePatternParser.StarPatternContext ctx) {
      return List.of(ShapePattern.DimRange.star(ctx.name.getText()));
    }

    @Override
    public List<ShapePattern> visitPlusPattern(DimShapePatternParser.PlusPatternContext ctx) {
      return List.of(ShapePattern.DimRange.plus(ctx.name.getText()));
    }

    @Override
    public List<ShapePattern> visitSingleDim(DimShapePatternParser.SingleDimContext ctx) {
      return List.of(new ShapePattern.NamedDim(ctx.ID().getText()));
    }
  }

  public static DimShapeMatcher parse(String source) {
    return new DimShapeMatcher(parsePatternList(source));
  }

  @VisibleForTesting
  static List<ShapePattern> validatePatternList(List<ShapePattern> pl) {
    Set<String> names = new HashSet<>();
    LinkedHashSet<String> duplicates = new LinkedHashSet<>();
    List<ShapePattern> ranges = new ArrayList<>();

    List<ShapePattern> queue = new ArrayList<>(pl);
    while (!queue.isEmpty()) {
      var cur = queue.removeFirst();
      if (!names.add(cur.name)) {
        duplicates.add(cur.name);
      }
      if (cur instanceof ShapePattern.DimRange range) {
        ranges.add(range);
      }
      if (cur instanceof ShapePattern.DimGroup group) {
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

  @VisibleForTesting
  static List<ShapePattern> parsePatternList(String source) {
    var lexer = new DimShapePatternLexer(CharStreams.fromString(source));
    var tokens = new CommonTokenStream(lexer);
    var parser = new DimShapePatternParser(tokens);
    var tree = parser.patternList();

    var visitor = new ParseVisitor();
    return validatePatternList(visitor.visit(tree));
  }

  List<ShapePattern> patterns;
}
