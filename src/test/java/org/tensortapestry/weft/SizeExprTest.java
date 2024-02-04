package org.tensortapestry.weft;

import java.util.List;
import java.util.Map;
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
import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.CommonAssertions;
import org.tensortapestry.zspace.ops.CellWise;

public class SizeExprTest implements CommonAssertions {

  public abstract static class SizeExpr {

    @Getter
    @RequiredArgsConstructor
    public enum BinOp {
      ADD("+", 2, Integer::sum),
      SUB("-", 2, (a, b) -> a - b),
      MUL("*", 3, (a, b) -> a * b),
      DIV("/", 3, (a, b) -> a / b),
      MOD("%", 3, (a, b) -> a % b),
      POW("^", 1, CellWise::intPow);

      private final String op;
      private final int precedence;
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
    public static class BinOpExpr extends SizeExpr {

      @Nonnull
      BinOp op;

      @Nonnull
      SizeExpr left;

      @Nonnull
      SizeExpr right;

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
    public static class NegateExpr extends SizeExpr {

      @Nonnull
      SizeExpr expr;

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
    public static class IdentExpr extends SizeExpr {

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
    public static class NumberExpr extends SizeExpr {

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

    public static class SizeExprVisitor extends SizeExprBaseVisitor<SizeExpr> {

      @Override
      public SizeExpr visitExprProgram(SizeExprParser.ExprProgramContext ctx) {
        return visit(ctx.e);
      }

      @Override
      public SizeExpr visitBinOpExpr(SizeExprParser.BinOpExprContext ctx) {
        return new BinOpExpr(BinOp.fromOpString(ctx.op.getText()), visit(ctx.lhs), visit(ctx.rhs));
      }

      @Override
      public SizeExpr visitParensExpr(SizeExprParser.ParensExprContext ctx) {
        return visit(ctx.e);
      }

      @Override
      public SizeExpr visitNegateExpr(SizeExprParser.NegateExprContext ctx) {
        return new NegateExpr(visit(ctx.e));
      }

      @Override
      public SizeExpr visitIdentifierExpr(SizeExprParser.IdentifierExprContext ctx) {
        return new IdentExpr(ctx.dotted_id().ID().stream().map(ParseTree::getText).toList());
      }

      @Override
      public SizeExpr visitNumberExpr(SizeExprParser.NumberExprContext ctx) {
        return new NumberExpr(Integer.parseInt(ctx.integer().getText()));
      }
    }

    public abstract String format();

    public abstract int precedence();

    public abstract int eval(Function<String, Integer> lookup);

    public static SizeExpr parse(String source) {
      var lexer = new SizeExprLexer(CharStreams.fromString(source));
      var tokens = new CommonTokenStream(lexer);
      var parser = new SizeExprParser(tokens);
      var tree = parser.exprProgram();

      var visitor = new SizeExprVisitor();
      return visitor.visit(tree);
    }
  }

  @Test
  public void test_parseSizeExpr() {
    var input = "a.b + (2 + (-b)) * c + 3";
    var expr = SizeExpr.parse(input);

    assertThat(expr.eval(Map.of("a.b", 1, "b", 2, "c", 3)::get)).isEqualTo(4);

    System.out.println(expr.format());
    assertThat(expr.format()).isEqualTo(input);
  }
}
