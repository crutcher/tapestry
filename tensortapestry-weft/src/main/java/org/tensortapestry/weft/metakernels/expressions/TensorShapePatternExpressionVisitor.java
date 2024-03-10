package org.tensortapestry.weft.metakernels.expressions;

import java.util.List;
import org.tensortapestry.weft.metakernels.antlr.generated.IndexedDimShapesExpressionsBaseVisitor;
import org.tensortapestry.weft.metakernels.antlr.generated.IndexedDimShapesExpressionsParser;

class TensorShapePatternExpressionVisitor extends IndexedDimShapesExpressionsBaseVisitor<Object> {

  @Override
  public List<TensorShapePatternMatcher.PatternItem> visitProg(
    IndexedDimShapesExpressionsParser.ProgContext ctx
  ) {
    return visitPatternList(ctx.patternList());
  }

  @Override
  public List<TensorShapePatternMatcher.PatternItem> visitPatternList(
    IndexedDimShapesExpressionsParser.PatternListContext ctx
  ) {
    return visitPatternSequence(ctx.patternSequence());
  }

  @Override
  public TensorShapePatternMatcher.PatternItem.PatternGroup visitGroupPattern(
    IndexedDimShapesExpressionsParser.GroupPatternContext ctx
  ) {
    var name = (TensorShapePatternMatcher.Symbol) visit(ctx.qualName());
    var expressions = visitPatternSequence(ctx.patternSequence());
    return new TensorShapePatternMatcher.PatternItem.PatternGroup(name, expressions);
  }

  @Override
  public List<TensorShapePatternMatcher.PatternItem> visitPatternSequence(
    IndexedDimShapesExpressionsParser.PatternSequenceContext ctx
  ) {
    var expressions = ctx
      .pattern()
      .stream()
      .map(this::visit)
      .map(TensorShapePatternMatcher.PatternItem.class::cast)
      .toList();
    return expressions;
  }

  @Override
  public Object visitSingleDimPattern(
    IndexedDimShapesExpressionsParser.SingleDimPatternContext ctx
  ) {
    return new TensorShapePatternMatcher.PatternItem.SimpleDim(
      (TensorShapePatternMatcher.Symbol) visit(ctx.qualName())
    );
  }

  @Override
  public TensorShapePatternMatcher.PatternItem.EllipsisGroup visitEllipsisPattern(
    IndexedDimShapesExpressionsParser.EllipsisPatternContext ctx
  ) {
    var name = (TensorShapePatternMatcher.Symbol) visit(ctx.qualName());
    return new TensorShapePatternMatcher.PatternItem.EllipsisGroup(name);
  }

  @Override
  public TensorShapePatternMatcher.IndexedSymbol visitIndexName(
    IndexedDimShapesExpressionsParser.IndexNameContext ctx
  ) {
    var name = ctx.name.getText();
    var index = ctx.index.getText();
    return TensorShapePatternMatcher.IndexedSymbol.of(name, index);
  }

  @Override
  public TensorShapePatternMatcher.Symbol visitGlobalName(
    IndexedDimShapesExpressionsParser.GlobalNameContext ctx
  ) {
    return TensorShapePatternMatcher.Symbol.of(ctx.name.getText());
  }
}
