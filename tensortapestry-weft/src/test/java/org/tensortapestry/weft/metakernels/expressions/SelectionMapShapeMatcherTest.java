package org.tensortapestry.weft.metakernels.expressions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.annotations.VisibleForTesting;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.*;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;
import org.tensortapestry.common.collections.CollectionContracts;
import org.tensortapestry.common.json.JsonUtil;
import org.tensortapestry.common.testing.CommonAssertions;
import org.tensortapestry.weft.metakernels.antlr.generated.IndexedDimShapesExpressionsBaseVisitor;
import org.tensortapestry.weft.metakernels.antlr.generated.IndexedDimShapesExpressionsLexer;
import org.tensortapestry.weft.metakernels.antlr.generated.IndexedDimShapesExpressionsParser;
import org.tensortapestry.zspace.ZPoint;

class SelectionMapShapeMatcherTest implements CommonAssertions {
  @Value
  @Builder
  public static class ShapeMatch {
    @Value
    @Builder
    public static class DimMatch {
      String name;
      int index;
      int value;
    }

    @Value
    @Builder
    public static class GroupMatch {
      String name;
      int start;
      int end;
      ZPoint value;
    }

    @Nonnull
    ShapeExperssionMatcher pattern;

    @Nonnull
    ZPoint shape;

    @Singular
    @Nonnull
    Map<String, GroupMatch> groups;

    @Singular
    @Nonnull
    Map<String, DimMatch> dims;

    public ShapeMatch(
      @Nonnull
      ShapeExperssionMatcher pattern,
      @Nonnull
      ZPoint shape,
      @Nonnull
      Map<String, GroupMatch> groups,
      @Nonnull
      Map<String, DimMatch> dims
    ) {
      CollectionContracts.expectDistinct(dims.keySet(), groups.keySet(), "dims", "groups", "keys");
      CollectionContracts.expectMapKeysMatchItemKeys(dims, DimMatch::getName);
      CollectionContracts.expectMapKeysMatchItemKeys(groups, GroupMatch::getName);

      this.pattern = pattern;
      this.shape = shape;
      this.dims = Map.copyOf(dims);
      this.groups = Map.copyOf(groups);
    }
  }

  @Data
  @RequiredArgsConstructor
  public abstract static class ShapePatternItem {

    private final String name;

    @Getter
    @EqualsAndHashCode(callSuper = false)
    public static class NamedDim extends ShapePatternItem {

      public NamedDim(String name) {
        super(name);
      }

      @Override
      public String toString() {
        return getName();
      }

      @Override
      @Nonnull
      public Stream<ShapePatternItem> flatLeaves() {
        return Stream.of(this);
      }

      @Override
      @Nullable
      public ShapePatternItem find(String name) {
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
      public Stream<ShapePatternItem> flatLeaves() {
        return Stream.of(this);
      }
    }

    @Getter
    @EqualsAndHashCode(callSuper = false)
    public static class EllipsisGroup extends NamedDim {

      public EllipsisGroup(String name) {
        super(name);
      }

      @Override
      public String toString() {
        return getName() + "...";
      }

      @Override
      @Nonnull
      public Stream<ShapePatternItem> flatLeaves() {
        return Stream.of(this);
      }
    }

    @Getter
    @EqualsAndHashCode(callSuper = false)
    public static class PatternGroup extends ShapePatternItem {

      private final List<ShapePatternItem> expressions;

      public PatternGroup(String name, List<ShapePatternItem> expressions) {
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
      public Stream<ShapePatternItem> flatLeaves() {
        return expressions.stream().flatMap(ShapePatternItem::flatLeaves);
      }

      @Override
      @Nullable
      public ShapePatternItem find(String name) {
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
    public abstract Stream<ShapePatternItem> flatLeaves();

    @Nullable
    public abstract ShapePatternItem find(String name);
  }

  @VisibleForTesting
  static class ShapeExpressionVisitor extends IndexedDimShapesExpressionsBaseVisitor<Object> {

    @Override
    public List<ShapePatternItem> visitProg(IndexedDimShapesExpressionsParser.ProgContext ctx) {
      return visitPatternList(ctx.patternList());
    }

    @Override
    public List<ShapePatternItem> visitPatternList(
      IndexedDimShapesExpressionsParser.PatternListContext ctx
    ) {
      return visitPatternSequence(ctx.patternSequence());
    }

    @Override
    public ShapePatternItem.PatternGroup visitGroupPattern(
      IndexedDimShapesExpressionsParser.GroupPatternContext ctx
    ) {
      var name = ctx.name.getText();
      var expressions = visitPatternSequence(ctx.patternSequence());
      return new ShapePatternItem.PatternGroup(name, expressions);
    }

    @Override
    public List<ShapePatternItem> visitPatternSequence(
      IndexedDimShapesExpressionsParser.PatternSequenceContext ctx
    ) {
      var expressions = ctx
        .pattern()
        .stream()
        .map(this::visit)
        .map(ShapePatternItem.class::cast)
        .toList();
      return expressions;
    }

    @Override
    public ShapePatternItem.EllipsisGroup visitEllipsisPattern(
      IndexedDimShapesExpressionsParser.EllipsisPatternContext ctx
    ) {
      var name = ctx.name.getText();
      return new ShapePatternItem.EllipsisGroup(name);
    }

    @Override
    public ShapePatternItem.IndexedDim visitIndexName(
      IndexedDimShapesExpressionsParser.IndexNameContext ctx
    ) {
      var name = ctx.name.getText();
      var index = ctx.index.getText();
      return new ShapePatternItem.IndexedDim(name, index);
    }

    @Override
    public ShapePatternItem.NamedDim visitGlobalName(
      IndexedDimShapesExpressionsParser.GlobalNameContext ctx
    ) {
      return new ShapePatternItem.NamedDim(ctx.name.getText());
    }
  }

  @Value
  public static class ShapeExperssionMatcher {
    public static ShapeExperssionMatcher parse(String source) {
      var lexer = new IndexedDimShapesExpressionsLexer(CharStreams.fromString(source));
      var tokens = new CommonTokenStream(lexer);
      var parser = new IndexedDimShapesExpressionsParser(tokens);
      var tree = parser.prog();

      var visitor = new ShapeExpressionVisitor();
      @SuppressWarnings("unchecked")
      var expressions = (List<ShapePatternItem>) visitor.visit(tree);

      return new ShapeExperssionMatcher(expressions);
    }

    List<ShapePatternItem> items;
    List<ShapePatternItem> leaves;
    List<ShapePatternItem.PatternGroup> depthFirstGroupOrder;
    int ellipsisStart;
    Set<String> indexNames;

    public ShapeExperssionMatcher(List<ShapePatternItem> items) {
      this.items = List.copyOf(items);

      var names = new HashSet<String>();
      var indexNames = new HashSet<String>();
      var duplicates = new LinkedHashSet<String>();
      var ellipsisList = new ArrayList<ShapePatternItem.EllipsisGroup>();

      int eStart = -1;
      List<ShapePatternItem> leaves = new ArrayList<>();
      List<ShapePatternItem> visitQueue = new ArrayList<>(items);
      List<ShapePatternItem.PatternGroup> depthFirstGroupOrder = new ArrayList<>();
      while (!visitQueue.isEmpty()) {
        var expr = visitQueue.removeFirst();

        var name = expr.getName();

        if (!names.add(name)) {
          duplicates.add(name);
        }

        switch (expr) {
          case ShapePatternItem.EllipsisGroup ellipsis -> {
            eStart = leaves.size();
            leaves.add(expr);
            ellipsisList.add(ellipsis);
          }
          case ShapePatternItem.IndexedDim indexedDim -> {
            leaves.add(expr);
            indexNames.add(indexedDim.getIndex());
          }
          case ShapePatternItem.PatternGroup patternGroup -> {
            depthFirstGroupOrder.addFirst(patternGroup);
            visitQueue.addAll(patternGroup.getExpressions());
          }
          default -> {
            leaves.add(expr);
            // pass
          }
        }
      }
      this.indexNames = Set.copyOf(indexNames);
      this.leaves = List.copyOf(leaves);
      this.depthFirstGroupOrder = List.copyOf(depthFirstGroupOrder);
      this.ellipsisStart = eStart;

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
    }

    public String toExpression() {
      return items.toString();
    }

    public ShapeMatch match(ZPoint shape) {
      int numDims = shape.getNDim();
      var smBuilder = ShapeMatch.builder().shape(shape);
      smBuilder.pattern(this);

      var leaves = getLeaves();

      int minSize = leaves.size();
      int ellipsisStart = getEllipsisStart();

      if (ellipsisStart != -1) {
        minSize--;
      }

      if (
        (ellipsisStart == -1 && numDims != leaves.size()) ||
        (ellipsisStart != -1 && numDims < minSize)
      ) {
        throw new IllegalArgumentException(
          "Mismatched pattern and dims (%d): %s".formatted(numDims, this)
        );
      }

      var dims = new HashMap<String, ShapeMatch.DimMatch>();
      var groups = new HashMap<String, ShapeMatch.GroupMatch>();

      int dimOffset = 0;
      for (int leafIdx = 0; leafIdx < leaves.size(); leafIdx++) {
        var pattern = leaves.get(leafIdx);
        var name = pattern.getName();

        if (leafIdx == ellipsisStart) {
          int expansionSize = numDims - (leaves.size() - 1);
          int start = ellipsisStart + dimOffset;
          int end = start + expansionSize;
          var groupMatch = new ShapeMatch.GroupMatch(name, start, end, ZPoint.of(shape.unwrap().sliceDim(0, start, end)));
          groups.put(name, groupMatch);

          dimOffset = expansionSize - 1;
          continue;
        }

        var val = shape.get(leafIdx + dimOffset);
        var dimMatch = new ShapeMatch.DimMatch(name, leafIdx + dimOffset, val);
        dims.put(name, dimMatch);
      }

      for (var patternGroup : getDepthFirstGroupOrder()) {
        int start;
        {
          var first = patternGroup.getExpressions().getFirst();
          if (first instanceof ShapePatternItem.NamedDim) {
            start = dims.get(first.getName()).getIndex();
          } else {
            start = groups.get(first.getName()).getStart();
          }
        }

        int end;
        {
          var last = patternGroup.getExpressions().getLast();
          if (last instanceof ShapePatternItem.NamedDim) {
            end = dims.get(last.getName()).getIndex() + 1;
          } else {
            end = groups.get(last.getName()).getEnd();
          }
        }

        List<Integer> value = new ArrayList<>();
        for (var p : patternGroup.getExpressions()) {
          var name = p.getName();
          if (p instanceof ShapePatternItem.NamedDim) {
            value.add(dims.get(name).getValue());
          } else {
            groups.get(name).getValue().unwrap().forEachValue(value::add);
          }
        }

        var groupMatch = new ShapeMatch.GroupMatch(patternGroup.getName(), start, end, ZPoint.of(value));

        groups.put(groupMatch.getName(), groupMatch);
      }

      smBuilder.dims(dims);
      smBuilder.groups(groups);

      return smBuilder.build();
    }
  }

  @Test
  public void test_shapeMatch() {
    var expr = "[$batch..., $shape[$i]=($height, $width, $channels[$i]), $features[$i]]";
    var matcher = ShapeExperssionMatcher.parse(expr);
    var shape = ZPoint.of(100, 128, 256, 512, 3, 8);

    var result = matcher.match(shape);

    System.out.println(JsonUtil.toYaml(result));

  }

  @Test
  void test_round_trip() {
    String source = "[$batch..., $shape=($height, $width), $features[$i]]";
    var expr = ShapeExperssionMatcher.parse(source);
    assertThat(expr.toExpression()).isEqualTo(source);
  }

  @Test
  void test_validate() {
    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> ShapeExperssionMatcher.parse("[$batch, $batch]"))
      .withMessageContaining("Duplicate names: [$batch]");

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> ShapeExperssionMatcher.parse("[$batch, $outer=($foo, $batch)]"))
      .withMessageContaining("Duplicate names: [$batch]");

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> ShapeExperssionMatcher.parse("[$batch, $features[$batch]]"))
      .withMessageContaining("Overlap between names and index names: [$batch]");

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> ShapeExperssionMatcher.parse("[$batch..., $shape...]"))
      .withMessageContaining("Multiple ellipsis: [$batch..., $shape...]");
  }
}
