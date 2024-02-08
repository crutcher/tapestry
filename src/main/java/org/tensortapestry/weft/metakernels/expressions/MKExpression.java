package org.tensortapestry.weft.metakernels.expressions;

import java.util.List;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import javax.annotation.Nonnull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.tensortapestry.weft.metakernels.antlr.generated.MKExpressionsBaseVisitor;
import org.tensortapestry.weft.metakernels.antlr.generated.MKExpressionsLexer;
import org.tensortapestry.weft.metakernels.antlr.generated.MKExpressionsParser;
import org.tensortapestry.zspace.ops.CellWiseOps;

public abstract class MKExpression {

  @Getter
  @RequiredArgsConstructor
  public enum BinOp {
    ADD("+", 2, Integer::sum),
    SUB("-", 2, (a, b) -> a - b),
    MUL("*", 3, (a, b) -> a * b),
    DIV("/", 3, (a, b) -> a / b),
    MOD("%", 3, (a, b) -> a % b),
    POW("^", 1, CellWiseOps::intPow);

    private final String op;
    private final int precedence;

    @SuppressWarnings("ImmutableEnumChecker")
    private final IntBinaryOperator opFn;

    public static BinOp fromOpString(String op) {
      for (var value : values()) {
        if (value.op.equals(op)) {
          return value;
        }
      }
      throw new IllegalArgumentException("Unknown operator: " + op);
    }
  }

  @Value
  @EqualsAndHashCode(callSuper = false)
  @RequiredArgsConstructor
  public static class BinOpExpr extends MKExpression {

    @Nonnull
    BinOp op;

    @Nonnull
    MKExpression left;

    @Nonnull
    MKExpression right;

    @Override
    public String format() {
      String leftStr = left.format();
      String rightStr = right.format();

      if (left.precedence() < this.op.precedence) {
        leftStr = "(" + leftStr + ")";
      }
      if (right.precedence() <= this.op.precedence) {
        rightStr = "(" + rightStr + ")";
      }

      return String.format("%s %s %s", leftStr, op.op, rightStr);
    }

    @Override
    public int precedence() {
      return op.precedence;
    }

    @Override
    public int eval(Function<String, Integer> lookup) {
      return op.opFn.applyAsInt(left.eval(lookup), right.eval(lookup));
    }
  }

  @Value
  @EqualsAndHashCode(callSuper = false)
  @RequiredArgsConstructor
  public static class NegateExpr extends MKExpression {

    @Nonnull
    MKExpression expr;

    @Override
    public String format() {
      return String.format("-%s", expr.format());
    }

    @Override
    public int precedence() {
      return 1;
    }

    @Override
    public int eval(Function<String, Integer> lookup) {
      return -expr.eval(lookup);
    }
  }

  @Value
  @EqualsAndHashCode(callSuper = false)
  @RequiredArgsConstructor
  public static class IdentExpr extends MKExpression {

    List<String> name;

    @Override
    public String format() {
      return String.join(".", name);
    }

    @Override
    public int precedence() {
      return 5;
    }

    @Override
    public int eval(Function<String, Integer> lookup) {
      return lookup.apply(format());
    }
  }

  @Value
  @EqualsAndHashCode(callSuper = false)
  @RequiredArgsConstructor
  public static class NumberExpr extends MKExpression {

    int value;

    @Override
    public String format() {
      return Integer.toString(value);
    }

    @Override
    public int precedence() {
      return 5;
    }

    @Override
    public int eval(Function<String, Integer> lookup) {
      return value;
    }
  }

  public static class ParseVisitor extends MKExpressionsBaseVisitor<MKExpression> {

    @Override
    public MKExpression visitExprProgram(MKExpressionsParser.ExprProgramContext ctx) {
      return visit(ctx.e);
    }

    @Override
    public MKExpression visitBinOpExpr(MKExpressionsParser.BinOpExprContext ctx) {
      return new BinOpExpr(BinOp.fromOpString(ctx.op.getText()), visit(ctx.lhs), visit(ctx.rhs));
    }

    @Override
    public MKExpression visitParensExpr(MKExpressionsParser.ParensExprContext ctx) {
      return visit(ctx.e);
    }

    @Override
    public MKExpression visitNegateExpr(MKExpressionsParser.NegateExprContext ctx) {
      return new NegateExpr(visit(ctx.e));
    }

    @Override
    public MKExpression visitIdentifierExpr(MKExpressionsParser.IdentifierExprContext ctx) {
      return new IdentExpr(ctx.dottedId().ID().stream().map(ParseTree::getText).toList());
    }

    @Override
    public MKExpression visitNumberExpr(MKExpressionsParser.NumberExprContext ctx) {
      return new NumberExpr(Integer.parseInt(ctx.integer().getText()));
    }
  }

  public abstract String format();

  public abstract int precedence();

  public abstract int eval(Function<String, Integer> lookup);

  public static MKExpression parse(String source) {
    var lexer = new MKExpressionsLexer(CharStreams.fromString(source));
    var tokens = new CommonTokenStream(lexer);
    var parser = new MKExpressionsParser(tokens);
    var tree = parser.exprProgram();

    var visitor = new ParseVisitor();
    return visitor.visit(tree);
  }
}
