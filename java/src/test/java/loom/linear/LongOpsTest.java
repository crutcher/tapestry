package loom.linear;

import java.util.function.BinaryOperator;
import loom.testing.CommonAssertions;
import org.junit.Test;

public class LongOpsTest implements CommonAssertions {
  @Test
  public void test_isEmpty() {
    assertThat(LongOps.isEmpty(new long[] {})).isTrue();
    assertThat(LongOps.isEmpty(new long[] {1, 2, 3})).isFalse();

    assertThat(LongOps.isEmpty(new long[][] {})).isTrue();
    assertThat(LongOps.isEmpty(new long[][] {{1, 2, 3}, {4, 5, 6}})).isFalse();
  }

  @Test
  public void test_format() {
    assertThat(LongOps.format(new long[] {})).isEqualTo("[]");
    assertThat(LongOps.format(new long[] {1, 2, 3})).isEqualTo("[1, 2, 3]");

    assertThat(LongOps.format(new long[][] {})).isEqualTo("[[]]");
    assertThat(LongOps.format(new long[][] {{1, 2, 3}, {4, 5, 6}}))
        .isEqualTo("[[1, 2, 3], [4, 5, 6]]");

    assertThat(LongOps.formatPretty(new long[][] {}, "x := ", "\n| ")).isEqualTo("x := [[]]");

    String expected =
        """
                        x := [
                        |   [1, 2, 3],
                        |   [4, 5, 6]
                        | ]""";

    assertThat(LongOps.formatPretty(new long[][] {{1, 2, 3}, {4, 5, 6}}, "x := ", "\n| "))
        .isEqualTo(expected);
  }

  @Test
  public void test_vec_full_zeros_ones() {
    assertThat(LongOps.vec_full(3, 12)).isEqualTo(new long[] {12, 12, 12});
    assertThat(LongOps.vec_ones(3)).isEqualTo(new long[] {1, 1, 1});
    assertThat(LongOps.zeros(3)).isEqualTo(new long[] {0, 0, 0});

    long[] ref = new long[] {1, 2, 3};
    assertThat(LongOps.full_like(ref, 12)).isEqualTo(new long[] {12, 12, 12});
    assertThat(LongOps.ones_like(ref)).isEqualTo(new long[] {1, 1, 1});
    assertThat(LongOps.zeros_like(ref)).isEqualTo(new long[] {0, 0, 0});
  }

  @Test
  public void test_mat_zeros() {
    long[][] ref = new long[3][2];

    assertThat(LongOps.mat_zeros(3, 2)).isEqualTo(new long[][] {{0, 0}, {0, 0}, {0, 0}});
    assertThat(LongOps.zeros_like(ref)).isEqualTo(new long[][] {{0, 0}, {0, 0}, {0, 0}});
  }

  @Test
  public void test_mat_identity() {
    assertThat(LongOps.identity(3)).isEqualTo(new long[][] {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}});
  }

  @Test
  public void test_mat_diag() {
    assertThat(LongOps.diaganol(new long[] {1, 2, 3}))
        .isEqualTo(new long[][] {{1, 0, 0}, {0, 2, 0}, {0, 0, 3}});
  }

  @Test
  public void test_shape() {
    assertThat(LongOps.shape(new long[][] {})).isEqualTo(new long[] {0, 0});
    assertThat(LongOps.shape(new long[][] {{3}, {2}})).isEqualTo(new long[] {2, 1});
  }

  @Test
  public void testCheckDimsMatch() {
    assertThatThrownBy(() -> LongOps.checkDimsMatch(new long[] {1, 2}, new long[] {1, 2, 3}))
        .isInstanceOf(LinearDimError.class)
        .hasMessageContaining("dims mismatch: |2| vs |3|");
  }

  @Test
  public void test_uniOp() {
    assertThat(LongOps.uniOp((x) -> x + 1, new long[] {})).isEqualTo(new long[] {});
    assertThat(LongOps.uniOp((x) -> x + 1, new long[] {12, 3})).isEqualTo(new long[] {13, 4});
  }

  @Test
  public void test_neg() {
    assertThat(LongOps.neg(new long[] {})).isEqualTo(new long[] {});
    assertThat(LongOps.neg(new long[] {2, -3, 0})).isEqualTo(new long[] {-2, 3, 0});
  }

  @Test
  public void test_binOp() throws LinearDimError {
    BinaryOperator<Long> fn = (x, y) -> x + 2 * y;

    {
      long[] empty = new long[] {};
      long[] lhs = new long[] {3, 2};

      // long[], long[]
      long[] rhs = new long[] {-1, 9};
      assertThat(LongOps.binOp(fn, empty, empty)).isEqualTo(empty);
      assertThat(LongOps.binOp(fn, lhs, rhs)).isEqualTo(new long[] {1, 20});
      assertThatThrownBy(() -> LongOps.binOp(fn, lhs, empty))
          .isInstanceOf(LinearDimError.class)
          .hasMessageContaining("dims mismatch: |2| vs |0|");

      // long[], long
      assertThat(LongOps.binOp(fn, empty, 12)).isEqualTo(empty);
      assertThat(LongOps.binOp(fn, lhs, 12)).isEqualTo(new long[] {27, 26});

      // long, long[]
      assertThat(LongOps.binOp(fn, 12, empty)).isEqualTo(empty);
      assertThat(LongOps.binOp(fn, 12, lhs)).isEqualTo(new long[] {18, 16});
    }

    {
      long[][] empty = new long[0][0];
      long[][] lhs = new long[][] {{3, 2}, {1, 1}};

      // long[][], long[][]
      long[][] rhs = new long[][] {{-1, 9}, {2, 0}};
      assertThat(LongOps.binOp(fn, empty, empty)).isEqualTo(empty);
      assertThat(LongOps.binOp(fn, lhs, rhs)).isEqualTo(new long[][] {{1, 20}, {5, 1}});
      assertThatThrownBy(() -> LongOps.binOp(fn, lhs, empty))
          .isInstanceOf(LinearDimError.class)
          .hasMessageContaining("dims mismatch: |2, 2| vs |0, 0|");

      // long[][], long
      assertThat(LongOps.binOp(fn, empty, 12)).isEqualTo(empty);
      assertThat(LongOps.binOp(fn, lhs, 12)).isEqualTo(new long[][] {{27, 26}, {25, 25}});

      // long, long[][]
      assertThat(LongOps.binOp(fn, 12, empty)).isEqualTo(empty);
      assertThat(LongOps.binOp(fn, 12, lhs)).isEqualTo(new long[][] {{18, 16}, {14, 14}});
    }
  }

  @Test
  public void test_add() throws LinearDimError {
    {
      long[] empty = new long[] {};
      long[] lhs = new long[] {3, 2};

      // long[], long[]
      long[] rhs = new long[] {-1, 9};
      assertThat(LongOps.add(empty, empty)).isEqualTo(empty);
      assertThat(LongOps.add(lhs, rhs)).isEqualTo(new long[] {2, 11});
      assertThatThrownBy(() -> LongOps.add(lhs, empty))
          .isInstanceOf(LinearDimError.class)
          .hasMessageContaining("dims mismatch: |2| vs |0|");

      // long[], long
      assertThat(LongOps.add(empty, 12)).isEqualTo(empty);
      assertThat(LongOps.add(lhs, 12)).isEqualTo(new long[] {15, 14});

      // long, long[]
      assertThat(LongOps.add(12, empty)).isEqualTo(empty);
      assertThat(LongOps.add(12, lhs)).isEqualTo(new long[] {15, 14});
    }

    {
      long[][] empty = new long[0][0];
      long[][] lhs = new long[][] {{3, 2}, {1, 1}};

      // long[][], long[][]
      long[][] rhs = new long[][] {{-1, 9}, {2, 0}};
      assertThat(LongOps.add(empty, empty)).isEqualTo(empty);
      assertThat(LongOps.add(lhs, rhs)).isEqualTo(new long[][] {{2, 11}, {3, 1}});
      assertThatThrownBy(() -> LongOps.add(lhs, empty))
          .isInstanceOf(LinearDimError.class)
          .hasMessageContaining("dims mismatch: |2, 2| vs |0, 0|");

      // long[][], long
      assertThat(LongOps.add(empty, 12)).isEqualTo(empty);
      assertThat(LongOps.add(lhs, 12)).isEqualTo(new long[][] {{15, 14}, {13, 13}});

      // long, long[][]
      assertThat(LongOps.add(12, empty)).isEqualTo(empty);
      assertThat(LongOps.add(12, lhs)).isEqualTo(new long[][] {{15, 14}, {13, 13}});
    }
  }

  @Test
  public void test_sub() throws LinearDimError {
    {
      long[] empty = new long[] {};
      long[] lhs = new long[] {3, 2};

      // long[], long[]
      long[] rhs = new long[] {-1, 9};
      assertThat(LongOps.sub(empty, empty)).isEqualTo(empty);
      assertThat(LongOps.sub(lhs, rhs)).isEqualTo(new long[] {4, -7});
      assertThatThrownBy(() -> LongOps.sub(lhs, empty))
          .isInstanceOf(LinearDimError.class)
          .hasMessageContaining("dims mismatch: |2| vs |0|");

      // long[], long
      assertThat(LongOps.sub(empty, 12)).isEqualTo(empty);
      assertThat(LongOps.sub(lhs, 12)).isEqualTo(new long[] {-9, -10});

      // long, long[]
      assertThat(LongOps.sub(12, empty)).isEqualTo(empty);
      assertThat(LongOps.sub(12, lhs)).isEqualTo(new long[] {9, 10});
    }

    {
      long[][] empty = new long[0][0];
      long[][] lhs = new long[][] {{3, 2}, {1, 1}};

      // long[][], long[][]
      long[][] rhs = new long[][] {{-1, 9}, {2, 0}};
      assertThat(LongOps.sub(empty, empty)).isEqualTo(empty);
      assertThat(LongOps.sub(lhs, rhs)).isEqualTo(new long[][] {{4, -7}, {-1, 1}});
      assertThatThrownBy(() -> LongOps.sub(lhs, empty))
          .isInstanceOf(LinearDimError.class)
          .hasMessageContaining("dims mismatch: |2, 2| vs |0, 0|");

      // long[][], long
      assertThat(LongOps.sub(empty, 12)).isEqualTo(empty);
      assertThat(LongOps.sub(lhs, 12)).isEqualTo(new long[][] {{-9, -10}, {-11, -11}});

      // long, long[][]
      assertThat(LongOps.sub(12, empty)).isEqualTo(empty);
      assertThat(LongOps.sub(12, lhs)).isEqualTo(new long[][] {{9, 10}, {11, 11}});
    }
  }

  @Test
  public void test_mul() throws LinearDimError {
    {
      long[] empty = new long[] {};
      long[] lhs = new long[] {3, 2};

      // long[], long[]
      long[] rhs = new long[] {-1, 9};
      assertThat(LongOps.mul(empty, empty)).isEqualTo(empty);
      assertThat(LongOps.mul(lhs, rhs)).isEqualTo(new long[] {-3, 18});
      assertThatThrownBy(() -> LongOps.mul(lhs, empty))
          .isInstanceOf(LinearDimError.class)
          .hasMessageContaining("dims mismatch: |2| vs |0|");

      // long[], long
      assertThat(LongOps.mul(empty, 12)).isEqualTo(empty);
      assertThat(LongOps.mul(lhs, 12)).isEqualTo(new long[] {36, 24});

      // long, long[]
      assertThat(LongOps.mul(12, empty)).isEqualTo(empty);
      assertThat(LongOps.mul(12, lhs)).isEqualTo(new long[] {36, 24});
    }

    {
      long[][] empty = new long[0][0];
      long[][] lhs = new long[][] {{3, 2}, {1, 1}};

      // long[][], long[][]
      long[][] rhs = new long[][] {{-1, 9}, {2, 0}};
      assertThat(LongOps.mul(empty, empty)).isEqualTo(empty);
      assertThat(LongOps.mul(lhs, rhs)).isEqualTo(new long[][] {{-3, 18}, {2, 0}});
      assertThatThrownBy(() -> LongOps.mul(lhs, empty))
          .isInstanceOf(LinearDimError.class)
          .hasMessageContaining("dims mismatch: |2, 2| vs |0, 0|");

      // long[][], long
      assertThat(LongOps.mul(empty, 12)).isEqualTo(empty);
      assertThat(LongOps.mul(lhs, 12)).isEqualTo(new long[][] {{36, 24}, {12, 12}});

      // long, long[][]
      assertThat(LongOps.mul(12, empty)).isEqualTo(empty);
      assertThat(LongOps.mul(12, lhs)).isEqualTo(new long[][] {{36, 24}, {12, 12}});
    }
  }

  @Test
  public void test_div() throws LinearDimError {
    {
      long[] empty = new long[] {};
      long[] lhs = new long[] {3, 2};

      // long[], long[]
      long[] rhs = new long[] {-1, 9};
      assertThat(LongOps.div(empty, empty)).isEqualTo(empty);
      assertThat(LongOps.div(lhs, rhs)).isEqualTo(new long[] {-3, 0});
      assertThatThrownBy(() -> LongOps.div(lhs, empty))
          .isInstanceOf(LinearDimError.class)
          .hasMessageContaining("dims mismatch: |2| vs |0|");

      assertThatThrownBy(() -> LongOps.div(lhs, LongOps.zeros_like(lhs)))
          .isInstanceOf(ArithmeticException.class);

      // long[], long
      assertThat(LongOps.div(empty, 12)).isEqualTo(empty);
      assertThat(LongOps.div(lhs, 12)).isEqualTo(new long[] {0, 0});

      assertThatThrownBy(() -> LongOps.div(lhs, 0)).isInstanceOf(ArithmeticException.class);

      // long, long[]
      assertThat(LongOps.div(12, empty)).isEqualTo(empty);
      assertThat(LongOps.div(12, lhs)).isEqualTo(new long[] {4, 6});

      assertThatThrownBy(() -> LongOps.div(12, LongOps.zeros(1)))
          .isInstanceOf(ArithmeticException.class);
    }

    {
      long[][] empty = new long[0][0];
      long[][] lhs = new long[][] {{3, 2}, {1, 1}};

      // long[][], long[][]
      long[][] rhs = new long[][] {{-1, 9}, {2, 1}};
      assertThat(LongOps.div(empty, empty)).isEqualTo(empty);
      assertThat(LongOps.div(lhs, rhs)).isEqualTo(new long[][] {{-3, 0}, {0, 1}});
      assertThatThrownBy(() -> LongOps.div(lhs, empty))
          .isInstanceOf(LinearDimError.class)
          .hasMessageContaining("dims mismatch: |2, 2| vs |0, 0|");

      assertThatThrownBy(() -> LongOps.div(lhs, LongOps.zeros_like(lhs)))
          .isInstanceOf(ArithmeticException.class);

      // long[][], long
      assertThat(LongOps.div(empty, 12)).isEqualTo(empty);
      assertThat(LongOps.div(lhs, 12)).isEqualTo(new long[][] {{0, 0}, {0, 0}});

      assertThatThrownBy(() -> LongOps.div(lhs, 0)).isInstanceOf(ArithmeticException.class);

      // long, long[][]
      assertThat(LongOps.div(12, empty)).isEqualTo(empty);
      assertThat(LongOps.div(12, lhs)).isEqualTo(new long[][] {{4, 6}, {12, 12}});

      assertThatThrownBy(() -> LongOps.div(12, LongOps.mat_zeros(1, 2)))
          .isInstanceOf(ArithmeticException.class);
    }
  }

  @Test
  public void test_mod() throws LinearDimError {
    {
      long[] empty = new long[] {};
      long[] lhs = new long[] {3, 2};

      // long[], long[]
      long[] rhs = new long[] {-1, 9};
      assertThat(LongOps.mod(empty, empty)).isEqualTo(empty);
      assertThat(LongOps.mod(lhs, rhs)).isEqualTo(new long[] {0, 2});
      assertThatThrownBy(() -> LongOps.mod(lhs, empty))
          .isInstanceOf(LinearDimError.class)
          .hasMessageContaining("dims mismatch: |2| vs |0|");

      assertThatThrownBy(() -> LongOps.mod(lhs, LongOps.zeros_like(lhs)))
          .isInstanceOf(ArithmeticException.class);

      // long[], long
      assertThat(LongOps.mod(empty, 12)).isEqualTo(empty);
      assertThat(LongOps.mod(lhs, 12)).isEqualTo(new long[] {3, 2});

      assertThatThrownBy(() -> LongOps.mod(lhs, 0)).isInstanceOf(ArithmeticException.class);

      // long, long[]
      assertThat(LongOps.mod(9, empty)).isEqualTo(empty);
      assertThat(LongOps.mod(9, lhs)).isEqualTo(new long[] {0, 1});

      assertThatThrownBy(() -> LongOps.mod(12, LongOps.zeros(1)))
          .isInstanceOf(ArithmeticException.class);
    }

    {
      long[][] empty = new long[0][0];
      long[][] lhs = new long[][] {{3, 2}, {1, 1}};

      // long[][], long[][]
      long[][] rhs = new long[][] {{-1, 9}, {2, 1}};
      assertThat(LongOps.mod(empty, empty)).isEqualTo(empty);
      assertThat(LongOps.mod(lhs, rhs)).isEqualTo(new long[][] {{0, 2}, {1, 0}});
      assertThatThrownBy(() -> LongOps.mod(lhs, empty))
          .isInstanceOf(LinearDimError.class)
          .hasMessageContaining("dims mismatch: |2, 2| vs |0, 0|");

      assertThatThrownBy(() -> LongOps.mod(lhs, LongOps.zeros_like(lhs)))
          .isInstanceOf(ArithmeticException.class);

      // long[][], long
      assertThat(LongOps.mod(empty, 12)).isEqualTo(empty);
      assertThat(LongOps.mod(lhs, 12)).isEqualTo(new long[][] {{3, 2}, {1, 1}});

      assertThatThrownBy(() -> LongOps.mod(lhs, 0)).isInstanceOf(ArithmeticException.class);

      // long, long[][]
      assertThat(LongOps.mod(8, empty)).isEqualTo(empty);
      assertThat(LongOps.mod(8, lhs)).isEqualTo(new long[][] {{2, 0}, {0, 0}});

      assertThatThrownBy(() -> LongOps.mod(12, LongOps.mat_zeros(1, 2)))
          .isInstanceOf(ArithmeticException.class);
    }
  }

  @Test
  public void test_partialCompare() throws LinearDimError {
    assertThat(LongOps.partialCompare(new long[] {}, new long[] {}))
        .isEqualTo(PartialOrdering.EQUAL);

    long[] base = new long[] {1, 1};
    long[] gt1 = new long[] {2, 1};
    long[] gt2 = new long[] {1, 2};
    long[] lt1 = new long[] {0, 1};
    long[] lt2 = new long[] {1, 0};
    long[] uncomp = new long[] {2, -1};

    assertThat(LongOps.partialCompare(base, base)).isEqualTo(PartialOrdering.EQUAL);

    assertThat(LongOps.partialCompare(base, gt1)).isEqualTo(PartialOrdering.LESS_THAN);
    assertThat(LongOps.partialCompare(base, gt2)).isEqualTo(PartialOrdering.LESS_THAN);

    assertThat(LongOps.partialCompare(base, lt1)).isEqualTo(PartialOrdering.GREATER_THAN);
    assertThat(LongOps.partialCompare(base, lt2)).isEqualTo(PartialOrdering.GREATER_THAN);

    assertThat(LongOps.partialCompare(base, uncomp)).isEqualTo(PartialOrdering.INCOMPARABLE);

    assertThat(LongOps.eq(base, base)).isEqualTo(true);
    assertThat(LongOps.eq(base, lt1)).isEqualTo(false);
    assertThat(LongOps.eq(base, lt2)).isEqualTo(false);
    assertThat(LongOps.eq(base, gt1)).isEqualTo(false);
    assertThat(LongOps.eq(base, gt2)).isEqualTo(false);
    assertThat(LongOps.eq(base, uncomp)).isEqualTo(false);

    assertThat(LongOps.ne(base, base)).isEqualTo(false);
    assertThat(LongOps.ne(base, lt1)).isEqualTo(true);
    assertThat(LongOps.ne(base, lt2)).isEqualTo(true);
    assertThat(LongOps.ne(base, gt1)).isEqualTo(true);
    assertThat(LongOps.ne(base, gt2)).isEqualTo(true);
    assertThat(LongOps.ne(base, uncomp)).isEqualTo(true);

    assertThat(LongOps.lt(base, base)).isEqualTo(false);
    assertThat(LongOps.lt(base, lt1)).isEqualTo(false);
    assertThat(LongOps.lt(base, lt2)).isEqualTo(false);
    assertThat(LongOps.lt(base, gt1)).isEqualTo(true);
    assertThat(LongOps.lt(base, gt2)).isEqualTo(true);
    assertThat(LongOps.lt(base, uncomp)).isEqualTo(false);

    assertThat(LongOps.le(base, base)).isEqualTo(true);
    assertThat(LongOps.le(base, lt1)).isEqualTo(false);
    assertThat(LongOps.le(base, lt1)).isEqualTo(false);
    assertThat(LongOps.le(base, gt1)).isEqualTo(true);
    assertThat(LongOps.le(base, gt2)).isEqualTo(true);
    assertThat(LongOps.le(base, uncomp)).isEqualTo(false);

    assertThat(LongOps.gt(base, base)).isEqualTo(false);
    assertThat(LongOps.gt(base, lt1)).isEqualTo(true);
    assertThat(LongOps.gt(base, lt2)).isEqualTo(true);
    assertThat(LongOps.gt(base, gt1)).isEqualTo(false);
    assertThat(LongOps.gt(base, gt2)).isEqualTo(false);
    assertThat(LongOps.gt(base, uncomp)).isEqualTo(false);

    assertThat(LongOps.ge(base, base)).isEqualTo(true);
    assertThat(LongOps.ge(base, lt1)).isEqualTo(true);
    assertThat(LongOps.ge(base, lt2)).isEqualTo(true);
    assertThat(LongOps.ge(base, gt1)).isEqualTo(false);
    assertThat(LongOps.ge(base, gt2)).isEqualTo(false);
    assertThat(LongOps.ge(base, uncomp)).isEqualTo(false);
  }
}
