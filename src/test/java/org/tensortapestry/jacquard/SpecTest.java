package org.tensortapestry.jacquard;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;
import org.tensortapestry.common.testing.CommonAssertions;

public class SpecTest implements CommonAssertions {

  public abstract static class Expr {

    public abstract String format();
  }

  @Value
  @RequiredArgsConstructor
  public static class BinOp extends Expr {

    String op;
    Expr left;
    Expr right;

    @Override
    public String format() {
      return String.format("(%s %s %s)", left.format(), op, right.format());
    }
  }

  @Value
  @RequiredArgsConstructor
  public static class Negate extends Expr {

    Expr expr;

    @Override
    public String format() {
      return String.format("-%s", expr.format());
    }
  }

  @Value
  @RequiredArgsConstructor
  public static class Variable extends Expr {

    String name;

    @Override
    public String format() {
      return name;
    }
  }

  @Value
  @RequiredArgsConstructor
  public static class Number extends Expr {

    int value;

    @Override
    public String format() {
      return Integer.toString(value);
    }
  }

  public static class ExprVisitor extends SelectorSpecBaseVisitor<Expr> {

    Expr rollBinOpSequence(List<ParseTree> children) {
      var expr = visit(children.get(0));
      for (int i = 1; i < children.size(); i += 2) {
        var op = children.get(i).getText();
        var rhs = visit(children.get(i + 1));
        expr = new BinOp(op, expr, rhs);
      }
      return expr;
    }

    @Override
    public Expr visitP1Expression(SelectorSpecParser.P1ExpressionContext ctx) {
      return rollBinOpSequence(ctx.children);
    }

    @Override
    public Expr visitP2Expression(SelectorSpecParser.P2ExpressionContext ctx) {
      return rollBinOpSequence(ctx.children);
    }

    @Override
    public Expr visitP3Expression(SelectorSpecParser.P3ExpressionContext ctx) {
      return rollBinOpSequence(ctx.children);
    }

    @Override
    public Expr visitNumber(SelectorSpecParser.NumberContext ctx) {
      return new Number(Integer.parseInt(ctx.getText()));
    }

    @Override
    public Expr visitVariable(SelectorSpecParser.VariableContext ctx) {
      return new Variable(ctx.getText());
    }

    @Override
    public Expr visitSignedAtom(SelectorSpecParser.SignedAtomContext ctx) {
      if (ctx.children.size() == 1) {
        return visit(ctx.getChild(0));
      }

      var expr = visit(ctx.getChild(1));
      if (ctx.op.getType() == SelectorSpecParser.MINUS) {
        expr = new Negate(expr);
      }
      return expr;
    }
  }

  @Test
  public void test() {
    String example = "x - 7 + 3 y";
    var lexer = new SelectorSpecLexer(CharStreams.fromString(example));
    var tokens = new CommonTokenStream(lexer);
    var parser = new SelectorSpecParser(tokens);
    var tree = parser.expression();

    System.err.println(tree.toStringTree(parser));

    var visitor = new ExprVisitor();
    var expr = visitor.visit(tree);
    System.err.println(expr);
    System.err.println(expr.format());
    /*
    String javaClassContent = "public class SampleClass { void DoSomething(){} }";
    Java8Lexer java8Lexer = new Java8Lexer(CharStreams.fromString(javaClassContent));

    CommonTokenStream tokens = new CommonTokenStream(lexer);
    Java8Parser parser = new Java8Parser(tokens);
    ParseTree tree = parser.compilationUnit();

    ParseTreeWalker walker = new ParseTreeWalker();
    UppercaseMethodListener listener= new UppercaseMethodListener();

    walker.walk(listener, tree);

    assertThat(listener.getErrors().size(), is(1));
    assertThat(listener.getErrors().get(0), is("Method DoSomething is uppercased!"));

     */
  }
}
