package org.tensortapestry.weft.metakernels.expressions;

import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;
import org.tensortapestry.common.collections.EnumerationUtils;
import org.tensortapestry.common.json.JsonUtil;
import org.tensortapestry.common.testing.CommonAssertions;
import org.tensortapestry.weft.metakernels.antlr.generated.IndexedDimShapesExpressionsBaseVisitor;
import org.tensortapestry.weft.metakernels.antlr.generated.IndexedDimShapesExpressionsLexer;
import org.tensortapestry.weft.metakernels.antlr.generated.IndexedDimShapesExpressionsParser;
import org.tensortapestry.zspace.ZPoint;
import org.tensortapestry.zspace.ZTensor;
import org.tensortapestry.zspace.indexing.IndexingFns;

class SelectionMapShapeMatcherTest implements CommonAssertions {

  /**
   * Common base class for index groups.
   */
  @Data
  @SuperBuilder
  public abstract static class DimGroupBase {

    int size;

    @Singular
    @Nonnull
    Map<String, Integer> dims;

    @Singular
    @Nonnull
    Map<String, List<Integer>> groups;

    public int getDim(String name) {
      return dims.get(name);
    }

    public List<Integer> getGroup(String name) {
      return groups.get(name);
    }
  }

  @Value
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  public static class DimLocationIndex extends DimGroupBase {

    public DimShapeIndex toShapeIndex(ZPoint shape) {
      return toShapeIndex(shape.toArray());
    }

    public DimShapeIndex toShapeIndex(List<Integer> shape) {
      return toShapeIndex(IndexingFns.unboxList(shape));
    }

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
  public static class DimShapeIndex extends DimShapeMatcher.DimGroupBase {}

  @Value
  @RequiredArgsConstructor
  public static class DimLayout {

    DimLocationIndex locations;
    DimShapeIndex shapes;
  }

  @Value
  public static class ShapeExpressionMatcher {

    List<ShapePattern> patterns;

    public ShapeExpressionMatcher(List<ShapePattern> patterns) {
      this.patterns = List.copyOf(validateExpressionList(patterns));
    }

    public ShapeExpressionMatcher(String expression) {
      this(parseShapeExpression(expression));
    }

    public ShapePattern find(String name) {
      return patterns
        .stream()
        .map(p -> p.find(name))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
    }

    public DimLayout match(ZPoint shape) {
      var locations = matchLocations(shape.getNDim());
      return new DimLayout(locations, locations.toShapeIndex(shape));
    }

    public DimLocationIndex matchLocations(int numDims) {
      Map<String, Integer> dims = new LinkedHashMap<>();
      Map<String, List<Integer>> groups = new LinkedHashMap<>();

      var leaves = patterns.stream().flatMap(ShapePattern::flatLeaves).collect(Collectors.toList());

      int minSize = leaves.size();
      int expansionIndex = -1;

      var optionalEllipsis = EnumerationUtils
        .enumerate(leaves)
        .stream()
        .filter(e -> e.getValue() instanceof ShapePattern.NamedEllipsis)
        .findFirst();

      if (optionalEllipsis.isPresent()) {
        var p = optionalEllipsis.get();
        var idx = p.getKey();

        expansionIndex = idx;
        minSize--;
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

      List<ShapePattern.PatternGroup> schedule = new ArrayList<>();
      List<ShapePattern> queue = new ArrayList<>(patterns);
      while (!queue.isEmpty()) {
        var cur = queue.removeFirst();
        if (cur instanceof ShapePattern.PatternGroup group) {
          schedule.addFirst(group);
          queue.addAll(0, group.getExpressions());
        }
      }

      while (!schedule.isEmpty()) {
        var cur = schedule.removeFirst();
        List<Integer> indexes = new ArrayList<>();
        for (var p : cur.getExpressions()) {
          if (p instanceof ShapePattern.NamedDim dim) {
            indexes.add(dims.get(dim.getName()));
          } else {
            indexes.addAll(groups.get(p.getName()));
          }
        }
        groups.put(cur.getName(), List.copyOf(indexes));
      }

      return DimLocationIndex.builder().size(numDims).dims(dims).groups(groups).build();
    }
  }

  @Data
  @RequiredArgsConstructor
  public abstract static class ShapePattern {

    private final String name;

    @Getter
    @EqualsAndHashCode(callSuper = false)
    public static class NamedDim extends ShapePattern {

      public NamedDim(String name) {
        super(name);
      }

      @Override
      public String toString() {
        return getName();
      }

      @Override
      @Nonnull
      public Stream<ShapePattern> flatLeaves() {
        return Stream.of(this);
      }

      @Override
      @Nullable public ShapePattern find(String name) {
        return getName().equals(name) ? this : null;
      }
    }

    @Getter
    @EqualsAndHashCode(callSuper = false)
    public static class IndexedDim extends NamedDim {

      private final String index;

      public IndexedDim(String name, String index) {
        super(name);
        this.index = index;
      }

      @Override
      public String toString() {
        return getName() + "[" + index + "]";
      }

      @Override
      @Nonnull
      public Stream<ShapePattern> flatLeaves() {
        return Stream.of(this);
      }
    }

    @Getter
    @EqualsAndHashCode(callSuper = false)
    public static class NamedEllipsis extends NamedDim {

      public NamedEllipsis(String name) {
        super(name);
      }

      @Override
      public String toString() {
        return getName() + "...";
      }

      @Override
      @Nonnull
      public Stream<ShapePattern> flatLeaves() {
        return Stream.of(this);
      }
    }

    @Getter
    @EqualsAndHashCode(callSuper = false)
    public static class PatternGroup extends ShapePattern {

      private final List<ShapePattern> expressions;

      public PatternGroup(String name, List<ShapePattern> expressions) {
        super(name);
        this.expressions = expressions;
      }

      @Override
      public String toString() {
        return (
          getName() +
          "=" +
          expressions.stream().map(Object::toString).collect(Collectors.joining(", ", "(", ")"))
        );
      }

      @Override
      @Nonnull
      public Stream<ShapePattern> flatLeaves() {
        return expressions.stream().flatMap(ShapePattern::flatLeaves);
      }

      @Override
      @Nullable public ShapePattern find(String name) {
        return getName().equals(name)
          ? this
          : expressions
            .stream()
            .map(e -> e.find(name))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
      }
    }

    @Nonnull
    public abstract Stream<ShapePattern> flatLeaves();

    @Nullable public abstract ShapePattern find(String name);
  }

  @VisibleForTesting
  static class ShapeExpressionVisitor extends IndexedDimShapesExpressionsBaseVisitor<Object> {

    @Override
    public List<ShapePattern> visitProg(IndexedDimShapesExpressionsParser.ProgContext ctx) {
      return visitPatternList(ctx.patternList());
    }

    @Override
    public List<ShapePattern> visitPatternList(
      IndexedDimShapesExpressionsParser.PatternListContext ctx
    ) {
      return visitPatternSequence(ctx.patternSequence());
    }

    @Override
    public ShapePattern.PatternGroup visitGroupPattern(
      IndexedDimShapesExpressionsParser.GroupPatternContext ctx
    ) {
      var name = ctx.name.getText();
      var expressions = visitPatternSequence(ctx.patternSequence());
      return new ShapePattern.PatternGroup(name, expressions);
    }

    @Override
    public List<ShapePattern> visitPatternSequence(
      IndexedDimShapesExpressionsParser.PatternSequenceContext ctx
    ) {
      var expressions = ctx
        .pattern()
        .stream()
        .map(this::visit)
        .map(ShapePattern.class::cast)
        .toList();
      return expressions;
    }

    @Override
    public ShapePattern.NamedEllipsis visitEllipsisPattern(
      IndexedDimShapesExpressionsParser.EllipsisPatternContext ctx
    ) {
      var name = ctx.name.getText();
      return new ShapePattern.NamedEllipsis(name);
    }

    @Override
    public ShapePattern.IndexedDim visitIndexName(
      IndexedDimShapesExpressionsParser.IndexNameContext ctx
    ) {
      var name = ctx.name.getText();
      var index = ctx.index.getText();
      return new ShapePattern.IndexedDim(name, index);
    }

    @Override
    public ShapePattern.NamedDim visitGlobalName(
      IndexedDimShapesExpressionsParser.GlobalNameContext ctx
    ) {
      return new ShapePattern.NamedDim(ctx.name.getText());
    }
  }

  @VisibleForTesting
  @CanIgnoreReturnValue
  static List<ShapePattern> validateExpressionList(List<ShapePattern> expressions) {
    var names = new HashSet<String>();
    var indexNames = new HashSet<String>();
    var duplicates = new LinkedHashSet<String>();
    var ellipsisList = new ArrayList<ShapePattern.NamedEllipsis>();

    var queue = new ArrayList<ShapePattern>();
    queue.addAll(expressions);
    while (!queue.isEmpty()) {
      var expr = queue.removeFirst();

      var name = expr.getName();

      if (!names.add(name)) {
        duplicates.add(name);
      }

      switch (expr) {
        case ShapePattern.NamedEllipsis ellipsis -> {
          ellipsisList.add(ellipsis);
        }
        case ShapePattern.IndexedDim indexedDim -> {
          indexNames.add(indexedDim.getIndex());
        }
        case ShapePattern.PatternGroup patternGroup -> {
          queue.addAll(patternGroup.getExpressions());
        }
        default -> {
          // pass
        }
      }
    }

    var overlap = new HashSet<>(names);
    overlap.retainAll(indexNames);
    if (!overlap.isEmpty()) {
      throw new IllegalArgumentException("Overlap between names and index names: " + overlap);
    }

    if (!duplicates.isEmpty()) {
      throw new IllegalArgumentException("Duplicate names: " + duplicates);
    }
    if (ellipsisList.size() > 1) {
      throw new IllegalArgumentException("Multiple ellipsis: " + ellipsisList);
    }

    return expressions;
  }

  @VisibleForTesting
  static List<ShapePattern> parseShapeExpression(String source) {
    var lexer = new IndexedDimShapesExpressionsLexer(CharStreams.fromString(source));
    var tokens = new CommonTokenStream(lexer);
    var parser = new IndexedDimShapesExpressionsParser(tokens);
    var tree = parser.prog();

    var visitor = new ShapeExpressionVisitor();
    @SuppressWarnings("unchecked")
    var expressions = (List<ShapePattern>) visitor.visit(tree);
    return validateExpressionList(expressions);
  }

  @Value
  public static class ShapeListMatcher {

    ShapeExpressionMatcher shapeMatcher;

    public ShapeListMatcher(String expression) {
      this.shapeMatcher = new ShapeExpressionMatcher(expression);
    }

    public List<DimLayout> match(List<ZPoint> shapes) {
      var layouts = shapes.stream().map(shapeMatcher::match).toList();

      return layouts;
    }
  }

  /*
  @Test
  public void non() {
    var matcher = X(Map.of(
      "tensors", Map.of(
        "expression", "[@batch..., @shape=($height, $width), @features[$i]]",
        "cardinality", "+"

      ),
      "masks", Map.of(
        "expression", "[$shape=($height, $width), $features[$i]]",
        "cardinality", "+"
      )
    ));

    var y = Map.of(
      "tensors", List.of(
        ZPoint.of(100, 128, 256, 512, 3),
        ZPoint.of(100, 128, 256, 512, 12)
      ),
      "masks", List.of(
        ZPoint.of(256, 512, 3),
        ZPoint.of(256, 512, 12)
      )
    );

    var result = matcher.match(y);

    result.size("$height") == 256
    result.group("$shape") == List.of(256, 512);
    result.indexSize("$features") = List.of(3, 12);
  }
   */

  @Value
  public static class ShapeListPatternMatch {

    /**
     * The shapes that were matched.
     */
    List<ZPoint> shapes;

    /**
     * A map from index variables to sizes.
     */
    Map<String, Integer> indexSizes;
  }

  @Value
  public static class ShapeConfig {

    String pattern;
    String cardinality;
  }

  @Value
  @Builder
  public static class TensorMapShapeMatch {

    @Value
    @Builder
    public static class TensorShapeIndex {

      List<ZPoint> shapes;
      Map<String, ZPoint> patternIndex;
      DimLocationIndex locations;
    }

    Map<String, TensorShapeIndex> tensors;

    Map<String, ZTensor> env;
  }

  @Value
  public static class SelectionMapMatcher {

    @Value
    @Jacksonized
    @Builder
    public static class SelectionMapMatcherConfig {

      @Value
      @Jacksonized
      @Builder
      public static class GroupPattern {

        @Builder.Default
        boolean optional = false;

        String expression;
        String cardinality;
      }

      @Singular
      @Nonnull
      Map<String, GroupPattern> groups;
    }

    @Value
    @Builder
    public static class Match {

      @Value
      @Builder
      public static class ShapeListMatch {

        ShapeListMatcher matcher;
        List<ZPoint> shapes;
        List<DimLayout> layout;
      }

      @Singular
      @Nonnull
      Map<String, ShapeListMatch> shapeLists;
    }

    SelectionMapMatcherConfig mapMatcherConfig;
    Map<String, ShapeListMatcher> matchers;

    SelectionMapMatcher(SelectionMapMatcherConfig mapMatcherConfig) {
      this.mapMatcherConfig = mapMatcherConfig;
      Map<String, ShapeListMatcher> matchers = new HashMap<>();

      for (var groupPattern : mapMatcherConfig.getGroups().entrySet()) {
        var name = groupPattern.getKey();
        var pattern = groupPattern.getValue();

        var cardinality = pattern.getCardinality();

        if (cardinality.equals("*") || cardinality.equals("+")) {
          // pass

        } else if (!mapMatcherConfig.getGroups().containsKey(cardinality)) {
          throw new IllegalArgumentException(
            "Unknown cardinality for \"%s\": \"%s\"".formatted(name, cardinality)
          );
        }

        var matcher = new ShapeListMatcher(pattern.getExpression());
        matchers.put(name, matcher);
      }

      this.matchers = Map.copyOf(matchers);
    }

    public Match match(Map<String, List<ZPoint>> shapeGroups) {
      {
        var keys = new HashSet<>(shapeGroups.keySet());
        keys.removeAll(mapMatcherConfig.getGroups().keySet());
        if (!keys.isEmpty()) {
          throw new IllegalArgumentException(
            "Input shape groups %s have no matching pattern entries.".formatted(keys)
          );
        }
      }

      var matchBuilder = Match.builder();

      for (var kv : mapMatcherConfig.getGroups().entrySet()) {
        var key = kv.getKey();
        var config = kv.getValue();
        var matcher = matchers.get(key);

        var shapes = shapeGroups.get(key);
        if (shapes == null) {
          if (config.isOptional()) {
            continue;
          }

          throw new IllegalArgumentException("Missing required shape group: %s".formatted(key));
        }

        var count = shapes.size();
        var cardinality = config.getCardinality();
        if (cardinality.equals("*")) {
          // pass;
        } else if (cardinality.equals("+")) {
          if (count == 0) {
            throw new IllegalArgumentException(
              "Expected cardinality of \"%s\" for \"%s\", found: %d".formatted(
                  config.getCardinality(),
                  key,
                  count
                )
            );
          }
        } else {
          var thatCount = shapeGroups.getOrDefault(cardinality, List.of()).size();
          if (count != thatCount) {
            throw new IllegalArgumentException(
              "Input \"%s\" has cardinality \"%s\", which is (%d), but has size (%d)".formatted(
                  key,
                  cardinality,
                  thatCount,
                  count
                )
            );
          }
        }

        var dimLayout = matcher.match(shapes);

        matchBuilder.shapeList(
          key,
          Match.ShapeListMatch.builder().matcher(matcher).shapes(shapes).layout(dimLayout).build()
        );
      }

      return matchBuilder.build();
    }
  }

  @Test
  public void test_xxx() {
    var config = SelectionMapMatcher.SelectionMapMatcherConfig
      .builder()
      .group(
        "tensors",
        SelectionMapMatcher.SelectionMapMatcherConfig.GroupPattern
          .builder()
          .expression("[$batch..., $shape[$i]=($height, $width, $channels[$i]), $features[$i]]")
          .cardinality("+")
          .build()
      )
      .group(
        "masks",
        SelectionMapMatcher.SelectionMapMatcherConfig.GroupPattern
          .builder()
          .expression("[$shape[$i]=($height, $width, $channels[$i])]")
          .optional(true)
          .cardinality("tensors")
          .build()
      )
      .build();

    var mapMatcher = new SelectionMapMatcher(config);

    var shapeGroups = Map.of(
      "tensors",
      List.of(ZPoint.of(100, 128, 256, 512, 3, 8), ZPoint.of(100, 128, 256, 512, 1, 12)),
      "masks",
      List.of(ZPoint.of(256, 512, 3), ZPoint.of(256, 512, 1))
    );

    var m = mapMatcher.match(shapeGroups);
    System.out.println(JsonUtil.toPrettyJson(m));
  }

  @Test
  public void test_shape_expression_matcher() {
    String source = "[$batch..., $shape=($height, $width), $features[$i]]";
    var matcher = new ShapeExpressionMatcher(source);

    var shape = ZPoint.of(100, 128, 256, 512, 3);

    var layout = matcher.match(shape);

    assertThat(layout.getLocations())
      .isEqualTo(
        DimLocationIndex
          .builder()
          .size(5)
          .group("$batch", List.of(0, 1))
          .group("$shape", List.of(2, 3))
          .dim("$height", 2)
          .dim("$width", 3)
          .dim("$features", 4)
          .build()
      );

    assertThat(layout.getShapes())
      .isEqualTo(
        DimShapeIndex
          .builder()
          .size(5)
          .group("$batch", List.of(100, 128))
          .group("$shape", List.of(256, 512))
          .dim("$height", 256)
          .dim("$width", 512)
          .dim("$features", 3)
          .build()
      );
  }

  @Test
  void test_round_trip() {
    String source = "[$batch..., $shape=($height, $width), $features[$i]]";
    var expr = parseShapeExpression(source);
    assertThat(expr).hasToString(source);
  }

  @Test
  void test_validate() {
    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> parseShapeExpression("[$batch, $batch]"))
      .withMessageContaining("Duplicate names: [$batch]");

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> parseShapeExpression("[$batch, $outer=($foo, $batch)]"))
      .withMessageContaining("Duplicate names: [$batch]");

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> parseShapeExpression("[$batch, $features[$batch]]"))
      .withMessageContaining("Overlap between names and index names: [$batch]");

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> parseShapeExpression("[$batch..., $shape...]"))
      .withMessageContaining("Multiple ellipsis: [$batch..., $shape...]");
  }
}
