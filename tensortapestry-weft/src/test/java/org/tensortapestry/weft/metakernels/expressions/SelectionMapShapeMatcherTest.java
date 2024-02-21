package org.tensortapestry.weft.metakernels.expressions;


import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import lombok.*;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.CommonAssertions;
import org.tensortapestry.weft.metakernels.antlr.generated.DimShapePatternBaseVisitor;
import org.tensortapestry.weft.metakernels.antlr.generated.IndexedDimShapesExpressionsBaseVisitor;
import org.tensortapestry.weft.metakernels.antlr.generated.IndexedDimShapesExpressionsLexer;
import org.tensortapestry.weft.metakernels.antlr.generated.IndexedDimShapesExpressionsParser;

import java.util.List;
import java.util.stream.Collectors;

class SelectionMapShapeMatcherTest implements CommonAssertions {

  @Data
  @RequiredArgsConstructor
  public abstract static class ShapeExpression {
    private final String name;

    @Getter
    @EqualsAndHashCode(callSuper = false)
    public static class NamedDim extends ShapeExpression {
      public NamedDim(String name) {
        super(name);
      }

      @Override
      public String toString() {
        return getName();
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
    }

    @Getter
    @EqualsAndHashCode(callSuper = false)
    public static class PatternGroup extends ShapeExpression {
      private final List<ShapeExpression> expressions;

      public PatternGroup(String name, List<ShapeExpression> expressions) {
        super(name);
        this.expressions = expressions;
      }

      @Override
      public String toString() {
        return getName() + "=" +
               expressions.stream().map(Object::toString).collect(Collectors.joining(", ", "(", ")"));
      }
    }
  }

  @VisibleForTesting
  static class ShapeExpressionVisitor extends IndexedDimShapesExpressionsBaseVisitor<Object> {
    @Override
    public List<ShapeExpression> visitProg(IndexedDimShapesExpressionsParser.ProgContext ctx) {
      return visitPatternList(ctx.patternList());
    }

    @Override
    public List<ShapeExpression> visitPatternList(IndexedDimShapesExpressionsParser.PatternListContext ctx) {
      return visitPatternSequence(ctx.patternSequence());
    }

    @Override
    public ShapeExpression.PatternGroup visitGroupPattern(IndexedDimShapesExpressionsParser.GroupPatternContext ctx) {
      var name = ctx.name.getText();
      var expressions = visitPatternSequence(ctx.patternSequence());
      return new ShapeExpression.PatternGroup(name, expressions);
    }

    @Override
    public List<ShapeExpression> visitPatternSequence(IndexedDimShapesExpressionsParser.PatternSequenceContext ctx) {
      var expressions = ctx.pattern().stream()
        .map(this::visit)
        .map(ShapeExpression.class::cast)
        .toList();
      return expressions;
    }

    @Override
    public ShapeExpression.NamedEllipsis visitEllipsisPattern(IndexedDimShapesExpressionsParser.EllipsisPatternContext ctx) {
      var name = ctx.name.getText();
      return new ShapeExpression.NamedEllipsis(name);
    }

    @Override
    public ShapeExpression.IndexedDim visitIndexName(IndexedDimShapesExpressionsParser.IndexNameContext ctx) {
      var name = ctx.name.getText();
      var index = ctx.index.getText();
      return new ShapeExpression.IndexedDim(name, index);
    }

    @Override
    public ShapeExpression.NamedDim visitGlobalName(IndexedDimShapesExpressionsParser.GlobalNameContext ctx) {
      return new ShapeExpression.NamedDim(ctx.name.getText());
    }
  }

  @VisibleForTesting
  static List<ShapeExpression> parseShapeExpression(String source) {
    var lexer = new IndexedDimShapesExpressionsLexer(CharStreams.fromString(source));
    var tokens = new CommonTokenStream(lexer);
    var parser = new IndexedDimShapesExpressionsParser(tokens);
    var tree = parser.prog();

    var visitor = new ShapeExpressionVisitor();
    @SuppressWarnings("unchecked")
    var result = (List<ShapeExpression>) visitor.visit(tree);
    return result;
  }

  @Test
  void test_round_trip() {
    String source = "[$batch..., $shape=($height, $width), $features[$i]]";
    var expr = parseShapeExpression(source);
    assertThat(expr).hasToString(source);
  }

}