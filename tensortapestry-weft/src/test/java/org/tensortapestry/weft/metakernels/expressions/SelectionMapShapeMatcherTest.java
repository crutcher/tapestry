package org.tensortapestry.weft.metakernels.expressions;

import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;
import org.tensortapestry.common.collections.EnumerationUtils;
import org.tensortapestry.common.testing.CommonAssertions;
import org.tensortapestry.weft.metakernels.antlr.generated.IndexedDimShapesExpressionsBaseVisitor;
import org.tensortapestry.weft.metakernels.antlr.generated.IndexedDimShapesExpressionsLexer;
import org.tensortapestry.weft.metakernels.antlr.generated.IndexedDimShapesExpressionsParser;
import org.tensortapestry.zspace.ZPoint;
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
  public static class DimShapeIndex extends DimShapeMatcher.DimGroupBase {
  }

  @Value
  @RequiredArgsConstructor
  public static class DimLayout {

    DimLocationIndex locations;
    DimShapeIndex shapes;
  }

  @Value
  public static class ShapeExpressionMatcher {

    private final List<ShapePattern> patterns;

    public ShapeExpressionMatcher(List<ShapePattern> patterns) {
      this.patterns = patterns;
      validateExpressionList(patterns);
    }

    public ShapeExpressionMatcher(String expression) {
      this(parseShapeExpression(expression));
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
    }

    @Nonnull
    public abstract Stream<ShapePattern> flatLeaves();
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

  public static class ShapeListMatcher {

    private final ShapeExpressionMatcher shapeMatcher;

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

  @Test
  public void test() {
    String tensorPattern = "[$batch..., $shape[$i]=($height, $width, $channels[$i]), $features[$i]]";
    String maskPattern = "[$shape[$i]=($height, $width, $channels[$i])]";
    var matcher = new ShapeListMatcher(tensorPattern);

    var tensorShapes = List.of(
      ZPoint.of(100, 128, 256, 512, 7, 3),
      ZPoint.of(100, 128, 256, 512, 2, 12)
    );
    var maskShapes = List.of(
      ZPoint.of(256, 512, 7),
      ZPoint.of(256, 512, 2)
    );

    // %batch := [100, 128]
    // $shape := [
    //   [256, 512, 7],
    //   [256, 512, 3]
    // ]
    // $height := 256
    // $width := 512
    // $channels := [ 7, 2 ]
    // $features := [3, 12]

    /*
    Map<String, DimSize> r = matcher.match(shapes);

    r.get("$i") == IndexSize(value=2)
    r.get("$height") == ScalarDim(value=256, index=2)
    r.get("$width") == ScalarDim(value=512, index=3)
    r.get("$batch") == DimGroup(value=List.of(100, 128), index=0)

    r.get("$shape") == IndexGroup(List.of(
      DimGroup(value=List.of(256, 512, 7), index=1),
      DimGroup(value=List.of(256, 512, 3), index=1)
    ))

    r.get("$features") == IndexGroup(List.of(
      ScalarDim(value=3, index=5),
      ScalarDim(12, index=5)))
    r.get("$channels") == IndexGroup(List.of(
      ScalarDim(value=7, index=4),
      ScalarDim(value=2, index=4)))


    r.sizes().scalar("$height") == 256
    r.sizes().scalar("$width") == 512
    r.sizes().group("$batch") == List.of(100, 128)
    r.sizes().indexScalar("$channels") == List.of(7, 2)
    r.sizes().indexScalar("$features") == List.of(3, 12)

    r.sizes().group("$shape") == List.of(
      List.of(256, 512, 7),
      List.of(256, 512, 3)
      )

    r.sizes().indexGroup("$shape") == List.of(
      List.of(256, 512, 7),
      List.of(256, 512, 3)
      )

     */


    var layouts = matcher.match(tensorShapes);

    // The goal is, from a pattern like ...:
    // {
    //   <group_name>: {
    //     "shape": <shape_expression>
    //     <cardinality?>
    // }
    //
    // And given a map of `{ <name>: [<shape>, ...] }`:
    //
    // Produce a structure with group size counts,
    // index locations, and bindings to the dim, group, and index variables.
    //
    // Open questions, what is the expected result?
    // - we expect the index locations to be the same for all shapes.
    // - we expect non-indexed groups and dimensions to be the same for all shapes.
    //
    // What should the result object look like?
    //
    // Notionally, we want to be able to recover:
    // - the shapes passed in
    // - the index locations associated with the patterns
    // - the shapes associated with the index locations
    //
    // Some fields are scalar valued, and some are list valued.
    // How to represent this?
    // - Polymorphic return types?
    // - Distinct lookup machinery?
    /*

    //          | *scalar* | *group*
    // *global* | int      | int[]
    // *index*  | int[]    | ??? int[][] ???

    r.shapes() : Map<String, List<List<Integer>>>
    r.contains(<name>) : boolean
    r.isScalar() : boolean
    r.isGroup() : boolean // contains(<name>) && !isScalar()
    r.index().scalar(<name>) : int
    r.index().group(<name>) : List<Integer>


    result.index().scalar("$height") == 256
    result.sizes().scalar("$height") == 256
    result.index().group("$shape") == List.of(2, 3)
    result.sizes().group("$shape") == List.of(256, 512)

    result.index().scalar("$features") == 4
    result.sizes().group("$features") == List.of(3, 12)

    // no index for $i ?
    result.sizes().scalar("$i") == 2

    // how to represent indexed groups?
    // ["$shape[$i]=($height, $width, $channels[$i])"]

     */
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
