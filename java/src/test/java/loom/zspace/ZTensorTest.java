package loom.zspace;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Objects;
import java.util.function.BinaryOperator;
import loom.common.serialization.JsonUtil;
import loom.testing.CommonAssertions;
import org.junit.Test;

public class ZTensorTest implements CommonAssertions {
  @Test
  public void testAssertShape() {
    ZTensor t = ZTensor.from(new int[][] {{2, 3}, {4, 5}});

    assertThat(t.shapeAsArray()).isEqualTo(new int[] {2, 2});
    assertThat(t.shapeAsTensor()).isEqualTo(ZTensor.vector(2, 2));

    t.assertShape(2, 2);

    assertThatThrownBy(() -> t.assertShape(2, 2, 1, 2))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("shape [2, 2] != expected shape [2, 2, 1, 2]");
  }

  @Test
  public void test_hashCode() {
    ZTensor t = ZTensor.vector(1, 2, 3).immutable();
    assertThat(t).hasSameHashCodeAs(ZTensor.vector(1, 2, 3).immutable());

    ZTensor x = ZTensor.vector(3, 2, 1);
    var view = x.reverse(0);

    //noinspection ResultOfMethodCallIgnored
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(view::hashCode);

    var t2 = view.immutable();

    assertThat(t).hasSameHashCodeAs(t2);
  }

  @Test
  public void test_scalars() {
    var tensor = ZTensor.scalar(3);

    assertThat(tensor.ndim()).isEqualTo(0);
    assertThat(tensor.shapeAsArray()).isEqualTo(new int[] {});
    assertThat(tensor.shapeAsTensor()).isEqualTo(ZTensor.vector());

    assertThat(tensor)
        .hasToString("3")
        .isEqualTo(ZTensor.from(3))
        .extracting(ZTensor::toArray)
        .isEqualTo(3);

    assertThat(tensor.toT0()).isEqualTo(3);

    assertThat(tensor.add(ZTensor.scalar(2))).extracting(ZTensor::item).isEqualTo(5);

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> ZTensor.vector(2, 3).toT0());
  }

  @Test
  public void test_vectors() {
    var tensor = ZTensor.vector(3, 7);

    assertThat(tensor.ndim()).isEqualTo(1);
    assertThat(tensor.shapeAsArray()).isEqualTo(new int[] {2});
    assertThat(tensor.shapeAsTensor()).isEqualTo(ZTensor.vector(2));

    assertThat(tensor)
        .hasToString("[3, 7]")
        .isEqualTo(ZTensor.from(new int[] {3, 7}))
        .extracting(ZTensor::toArray)
        .isEqualTo(new int[] {3, 7});

    assertThat(tensor.toT1()).isEqualTo(new int[] {3, 7});

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> ZTensor.scalar(3).toT1());
  }

  @Test
  public void test_matrices() {
    var tensor = ZTensor.matrix(new int[] {3, 7}, new int[] {8, 9});

    assertThat(tensor.ndim()).isEqualTo(2);
    assertThat(tensor.shapeAsArray()).isEqualTo(new int[] {2, 2});
    assertThat(tensor.shapeAsTensor()).isEqualTo(ZTensor.vector(2, 2));

    assertThat(tensor)
        .hasToString("[[3, 7], [8, 9]]")
        .isEqualTo(ZTensor.from(new int[][] {{3, 7}, {8, 9}}))
        .extracting(ZTensor::toArray)
        .isEqualTo(new int[][] {{3, 7}, {8, 9}});

    assertThat(tensor.toT2()).isEqualTo(new int[][] {{3, 7}, {8, 9}});

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> ZTensor.scalar(3).toT2());
  }

  @Test
  public void test_JSON() {
    ZTensor z3 = ZTensor.zeros(0, 0, 0);
    assertJsonEquals(z3, "[[[]]]");

    // Degenerate tensors map to emtpy tensors.
    ZTensor deg = ZTensor.zeros(0, 5);
    assertThat(JsonUtil.toJson(deg)).isEqualTo("[[]]");

    ZTensor t = ZTensor.from(new int[][] {{2, 3}, {4, 5}});
    ZTensor s = ZTensor.scalar(3);

    assertJsonEquals(t, "[[2,3],[4,5]]");
    assertJsonEquals(s, "3");

    // As a field.
    assertJsonEquals(new JsonExampleContainer(t), "{\"tensor\": [[2,3],[4,5]]}");
    assertJsonEquals(new JsonExampleContainer(s), "{\"tensor\": 3}");
  }

  @Test
  public void test_create() {
    ZTensor t0 = ZTensor.scalar(3);
    ZTensor t1 = ZTensor.vector(2, 3, 4);
    ZTensor t2 = ZTensor.matrix(new int[] {2, 3}, new int[] {4, 5});

    assertThat(t0.ndim()).isEqualTo(0);
    assertThat(t0.size()).isEqualTo(1);
    assertThat(t0.item()).isEqualTo(3);

    assertThat(t1.ndim()).isEqualTo(1);
    assertThat(t1.size()).isEqualTo(3);
    assertThat(t1.get(1)).isEqualTo(3);

    assertThat(t2.ndim()).isEqualTo(2);
    assertThat(t2.size()).isEqualTo(4);
    assertThat(t2.get(1, 0)).isEqualTo(4);
  }

  @Test
  public void test_clone() {
    assertThat(ZTensor.vector(2, 3).clone()).isEqualTo(ZTensor.vector(2, 3));
  }

  @Test
  public void test_fromArray_toArray() {
    ZTensor t = ZTensor.from(new int[][] {{2, 3}, {4, 5}});

    assertThat(t.ndim()).isEqualTo(2);
    assertThat(t.size()).isEqualTo(4);
    assertThat(t.get(1, 0)).isEqualTo(4);

    ZTensor t2 = t.add(2);

    assertThat(t2.toArray()).isEqualTo(new int[][] {{4, 5}, {6, 7}});
  }

  @Test
  public void test_toString_parse() {
    ZTensor t = ZTensor.matrix(new int[][] {{2, 3}, {4, 5}});
    assertThat(t.toString()).isEqualTo("[[2, 3], [4, 5]]");

    assertThat(ZTensor.scalar(3).toString()).isEqualTo("3");
    assertThat(ZTensor.zeros(3, 0).toString()).isEqualTo("[[]]");

    assertThat(ZTensor.parse("3")).isEqualTo(ZTensor.scalar(3));
    assertThat(ZTensor.parse("[[2, 3]]")).isEqualTo(ZTensor.from(new int[][] {{2, 3}}));
    assertThat(ZTensor.parse("[[[]]]")).isEqualTo(ZTensor.zeros(0, 0, 0));

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> ZTensor.parse("[[2, "));
  }

  @Test
  public void test_zeros() {
    assertThat(ZTensor.zeros(2, 1)).isEqualTo(ZTensor.from(new int[][] {{0}, {0}}));
    assertThat(ZTensor.zeros_like(ZTensor.ones(2, 1))).isEqualTo(ZTensor.zeros(2, 1));
  }

  @Test
  public void test_ones() {
    assertThat(ZTensor.ones(2, 1)).isEqualTo(ZTensor.from(new int[][] {{1}, {1}}));
    assertThat(ZTensor.ones_like(ZTensor.zeros(2, 1))).isEqualTo(ZTensor.ones(2, 1));
  }

  @Test
  public void test_full() {
    assertThat(ZTensor.full(new int[] {2, 1}, 9)).isEqualTo(ZTensor.from(new int[][] {{9}, {9}}));
    assertThat(ZTensor.full_like(ZTensor.zeros(2, 1), 9))
        .isEqualTo(ZTensor.full(new int[] {2, 1}, 9));
  }

  @Test
  public void test_diagonal() {
    assertThat(ZTensor.diagonal()).isEqualTo(ZTensor.zeros(0, 0));
    assertThat(ZTensor.diagonal(2, 3, 4))
        .isEqualTo(ZTensor.from(new int[][] {{2, 0, 0}, {0, 3, 0}, {0, 0, 4}}));
  }

  @Test
  public void test_identity() {
    assertThat(ZTensor.identity_matrix(0)).isEqualTo(ZTensor.zeros(0, 0));
    assertThat(ZTensor.identity_matrix(3))
        .isEqualTo(ZTensor.from(new int[][] {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}}));
  }

  @Test
  public void test_selectDim() {
    ZTensor t = ZTensor.from(new int[][] {{2, 3}, {4, 5}});

    assertThat(t.selectDim(0, 0)).isEqualTo(ZTensor.from(new int[] {2, 3}));
    assertThat(t.selectDim(0, 1)).isEqualTo(ZTensor.from(new int[] {4, 5}));
    assertThat(t.selectDim(1, 0)).isEqualTo(ZTensor.from(new int[] {2, 4}));
    assertThat(t.selectDim(1, 1)).isEqualTo(ZTensor.from(new int[] {3, 5}));
  }

  @Test
  public void test_permute() {
    ZTensor t = ZTensor.from(new int[][][] {{{2, 3}, {4, 5}}, {{6, 7}, {8, 9}}});

    assertThat(t.permute(0, 1, 2)).isEqualTo(t);

    assertThat(t.permute(0, 2, 1))
        .isEqualTo(ZTensor.from(new int[][][] {{{2, 4}, {3, 5}}, {{6, 8}, {7, 9}}}));
  }

  @Test
  public void test_reorderDim() {
    var t = ZTensor.from(new int[][][] {{{2, 3}, {4, 5}}, {{6, 7}, {8, 9}}});

    var r = t.reorderDim(new int[] {1, 0}, 1);
    assertThat(r).isEqualTo(ZTensor.from(new int[][][] {{{4, 5}, {2, 3}}, {{8, 9}, {6, 7}}}));
  }

  @Test
  public void test_transpose() {
    ZTensor t = ZTensor.from(new int[][][] {{{2, 3, 4}, {5, 6, 7}}});
    assertThat(t.shapeAsArray()).isEqualTo(new int[] {1, 2, 3});

    {
      // No arguments
      var trans = t.transpose();
      assertThat(trans.shapeAsArray()).isEqualTo(new int[] {3, 2, 1});

      assertThat(trans).isEqualTo(t.T());

      assertThat(trans).isEqualTo(ZTensor.from(new int[][][] {{{2}, {5}}, {{3}, {6}}, {{4}, {7}}}));
    }

    {
      // arguments
      var trans = t.transpose(1, 0);
      assertThat(trans.shapeAsArray()).isEqualTo(new int[] {2, 1, 3});

      assertThat(trans).isEqualTo(ZTensor.from(new int[][][] {{{2, 3, 4}}, {{5, 6, 7}}}));
    }
  }

  @Test
  public void test_reverse() {
    ZTensor t = ZTensor.from(new int[][][] {{{2, 3, 4}, {5, 6, 7}}});

    assertThat(t.reverse(0)).isEqualTo(ZTensor.from(new int[][][] {{{2, 3, 4}, {5, 6, 7}}}));
    assertThat(t.reverse(1)).isEqualTo(ZTensor.from(new int[][][] {{{5, 6, 7}, {2, 3, 4}}}));
    assertThat(t.reverse(2)).isEqualTo(ZTensor.from(new int[][][] {{{4, 3, 2}, {7, 6, 5}}}));
  }

  @Test
  public void test_unsqueeze() {
    ZTensor t = ZTensor.from(new int[] {2, 3, 4});

    assertThat(t.unsqueeze(0)).isEqualTo(ZTensor.from(new int[][] {{2, 3, 4}}));
    assertThat(t.unsqueeze(1)).isEqualTo(ZTensor.from(new int[][] {{2}, {3}, {4}}));

    assertThat(t.unsqueeze(1).squeeze(1)).isEqualTo(t);
  }

  @Test
  public void test_broadcastDim() {
    ZTensor t = ZTensor.from(new int[][] {{2, 3}});

    assertThat(t.broadcastDim(0, 2)).isEqualTo(ZTensor.from(new int[][] {{2, 3}, {2, 3}}));
  }

  @Test
  public void test_broadcastTo() {
    ZTensor t = ZTensor.from(new int[][] {{2, 3}});

    assertThat(t.isBroadcastDim(0)).isFalse();
    assertThat(t.isBroadcastDim(1)).isFalse();

    ZTensor bview = t.broadcastTo(2, 2);
    assertThat(bview).isEqualTo(ZTensor.from(new int[][] {{2, 3}, {2, 3}}));

    assertThat(bview.isBroadcastDim(0)).isTrue();
    assertThat(bview.isBroadcastDim(1)).isFalse();

    bview.set(new int[] {0, 0}, 1);
    assertThat(bview).isEqualTo(ZTensor.from(new int[][] {{1, 3}, {1, 3}}));
  }

  @Test
  public void test_stream() {
    ZTensor t = ZTensor.matrix(new int[][] {{2, 3}, {4, 5}});

    var points = t.coordsStream().map(int[]::clone).toList();

    assertThat(points)
        .contains(new int[] {0, 0}, new int[] {0, 1}, new int[] {1, 0}, new int[] {1, 1});
  }

  @Test
  public void test_assign() {
    var t = ZTensor.zeros(2, 3);

    t.selectDim(0, 0).assign(ZTensor.vector(1, 2, 3));
    assertThat(t).isEqualTo(ZTensor.from(new int[][] {{1, 2, 3}, {0, 0, 0}}));
  }

  @Test
  public void test_uniOp() {
    assertThat(ZTensor.Ops.uniOp((x) -> x + 2, ZTensor.scalar(4))).isEqualTo(ZTensor.scalar(6));
    assertThat(ZTensor.Ops.uniOp((x) -> x + 2, ZTensor.vector())).isEqualTo(ZTensor.vector());
    assertThat(ZTensor.Ops.uniOp((x) -> x + 2, ZTensor.vector(2, 3)))
        .isEqualTo(ZTensor.vector(4, 5));
  }

  @Test
  public void test_neg() {
    assertThat(ZTensor.Ops.neg(ZTensor.scalar(4))).isEqualTo(ZTensor.scalar(-4));
    assertThat(ZTensor.Ops.neg(ZTensor.vector())).isEqualTo(ZTensor.vector());
    assertThat(ZTensor.Ops.neg(ZTensor.vector(2, 3))).isEqualTo(ZTensor.vector(-2, -3));

    assertThat(ZTensor.scalar(4).neg()).isEqualTo(ZTensor.scalar(-4));
    assertThat(ZTensor.vector().neg()).isEqualTo(ZTensor.vector());
    assertThat(ZTensor.vector(2, 3).neg()).isEqualTo(ZTensor.vector(-2, -3));
  }

  @Test
  public void test_binOp() {
    BinaryOperator<Integer> fn = (x, y) -> x + 2 * y;

    {
      ZTensor empty = ZTensor.vector();
      ZTensor lhs = ZTensor.vector(3, 2);

      // [2], [2]
      ZTensor rhs = ZTensor.vector(-1, 9);
      assertThat(ZTensor.Ops.binOp(fn, empty, empty)).isEqualTo(empty);

      assertThat(ZTensor.Ops.binOp(fn, lhs, rhs).toArray()).isEqualTo(new int[] {1, 20});
      assertThatThrownBy(() -> ZTensor.Ops.binOp(fn, lhs, empty))
          .isInstanceOf(IndexOutOfBoundsException.class)
          .hasMessageContaining("cannot broadcast shapes: [2], [0]");

      // Broadcast rules.
      // [2, 1], [2]
      assertThat(
              ZTensor.Ops.binOp(
                  Integer::sum, ZTensor.from(new int[][] {{1}, {2}}), ZTensor.vector(3, 4)))
          .isEqualTo(ZTensor.from(new int[][] {{4, 5}, {5, 6}}));
      assertThat(
              ZTensor.Ops.binOp(
                  Integer::sum, ZTensor.from(new int[][] {{1}, {2}}), ZTensor.scalar(5)))
          .isEqualTo(ZTensor.from(new int[][] {{6}, {7}}));

      // [2], <scalar>
      assertThat(ZTensor.Ops.binOp(fn, empty, 12)).isEqualTo(empty);
      assertThat(ZTensor.Ops.binOp(fn, lhs, 12).toArray()).isEqualTo(new int[] {27, 26});

      // <scalar>, [2]
      assertThat(ZTensor.Ops.binOp(fn, 12, empty)).isEqualTo(empty);
      assertThat(ZTensor.Ops.binOp(fn, 12, lhs).toArray()).isEqualTo(new int[] {18, 16});
    }

    {
      ZTensor empty = ZTensor.matrix();
      ZTensor lhs = ZTensor.from(new int[][] {{3, 2}, {1, 1}});

      // [2, 2], [2, 2]
      ZTensor rhs = ZTensor.from(new int[][] {{-1, 9}, {2, 0}});
      assertThat(ZTensor.Ops.binOp(fn, empty, empty)).isEqualTo(empty);
      assertThat(ZTensor.Ops.binOp(fn, lhs, rhs).toArray())
          .isEqualTo(new int[][] {{1, 20}, {5, 1}});
      assertThatThrownBy(() -> ZTensor.Ops.binOp(fn, lhs, empty))
          .isInstanceOf(IndexOutOfBoundsException.class)
          .hasMessageContaining("cannot broadcast shapes: [2, 2], [0, 0]");

      // [2, 2], <scalar>
      assertThat(ZTensor.Ops.binOp(fn, empty, 12)).isEqualTo(empty);
      assertThat(ZTensor.Ops.binOp(fn, lhs, 12).toArray())
          .isEqualTo(new int[][] {{27, 26}, {25, 25}});

      // <scalar>, [2, 2]
      assertThat(ZTensor.Ops.binOp(fn, 12, empty)).isEqualTo(empty);
      assertThat(ZTensor.Ops.binOp(fn, 12, lhs).toArray())
          .isEqualTo(new int[][] {{18, 16}, {14, 14}});
    }
  }

  @Test
  public void test_min() {
    var empty = ZTensor.zeros(0, 0);
    var lhs = ZTensor.from(new int[][] {{3, 2}, {1, 1}});

    // [2, 2], [2, 2]
    assertThat(ZTensor.Ops.min(empty, empty)).isEqualTo(empty);

    var rhs = ZTensor.from(new int[][] {{-1, 2}, {1, 0}});
    assertThat(ZTensor.Ops.min(lhs, rhs)).isEqualTo(ZTensor.from(new int[][] {{-1, 2}, {1, 0}}));

    assertThatThrownBy(() -> ZTensor.Ops.min(lhs, empty))
        .isInstanceOf(IndexOutOfBoundsException.class)
        .hasMessageContaining("cannot broadcast shapes: [2, 2], [0, 0]");

    // [2, 2], <scalar>
    assertThat(ZTensor.Ops.min(empty, 2)).isEqualTo(empty);

    assertThat(ZTensor.Ops.min(lhs, 2)).isEqualTo(ZTensor.from(new int[][] {{2, 2}, {1, 1}}));

    // <scalar>, [2, 2]
    assertThat(ZTensor.Ops.min(2, empty)).isEqualTo(empty);

    assertThat(ZTensor.Ops.min(2, lhs)).isEqualTo(ZTensor.from(new int[][] {{2, 2}, {1, 1}}));
  }

  @Test
  public void test_max() {
    var empty = ZTensor.zeros(0, 0);
    var lhs = ZTensor.from(new int[][] {{3, 2}, {1, 1}});

    // [2, 2], [2, 2]
    assertThat(ZTensor.Ops.max(empty, empty)).isEqualTo(empty);

    var rhs = ZTensor.from(new int[][] {{-1, 2}, {1, 6}});
    assertThat(ZTensor.Ops.max(lhs, rhs)).isEqualTo(ZTensor.from(new int[][] {{3, 2}, {1, 6}}));

    assertThatThrownBy(() -> ZTensor.Ops.max(lhs, empty))
        .isInstanceOf(IndexOutOfBoundsException.class)
        .hasMessageContaining("cannot broadcast shapes: [2, 2], [0, 0]");

    // [2, 2], <scalar>
    assertThat(ZTensor.Ops.max(empty, 2)).isEqualTo(empty);

    assertThat(ZTensor.Ops.max(lhs, 2)).isEqualTo(ZTensor.from(new int[][] {{3, 2}, {2, 2}}));

    // <scalar>, [2, 2]
    assertThat(ZTensor.Ops.max(2, empty)).isEqualTo(empty);

    assertThat(ZTensor.Ops.max(2, lhs)).isEqualTo(ZTensor.from(new int[][] {{3, 2}, {2, 2}}));
  }

  @Test
  public void test_add() {
    var empty = ZTensor.zeros(0, 0);
    var lhs = ZTensor.from(new int[][] {{3, 2}, {1, 1}});

    // [2, 2], [2, 2]
    assertThat(ZTensor.Ops.add(empty, empty)).isEqualTo(empty.add(empty)).isEqualTo(empty);

    var rhs = ZTensor.from(new int[][] {{-1, 9}, {2, 0}});
    assertThat(ZTensor.Ops.add(lhs, rhs))
        .isEqualTo(lhs.add(rhs))
        .isEqualTo(ZTensor.from(new int[][] {{2, 11}, {3, 1}}));

    assertThatThrownBy(() -> ZTensor.Ops.add(lhs, empty))
        .isInstanceOf(IndexOutOfBoundsException.class)
        .hasMessageContaining("cannot broadcast shapes: [2, 2], [0, 0]");

    // [2, 2], <scalar>
    assertThat(ZTensor.Ops.add(empty, 12)).isEqualTo(empty.add(12)).isEqualTo(empty);

    assertThat(ZTensor.Ops.add(lhs, 12))
        .isEqualTo(lhs.add(12))
        .isEqualTo(ZTensor.from(new int[][] {{15, 14}, {13, 13}}));

    // <scalar>, [2, 2]
    assertThat(ZTensor.Ops.add(12, empty)).isEqualTo(empty);

    assertThat(ZTensor.Ops.add(12, lhs)).isEqualTo(ZTensor.from(new int[][] {{15, 14}, {13, 13}}));

    var inplace = lhs.clone();
    ZTensor.Ops.add_(inplace, rhs);
    ZTensor.Ops.add_(inplace, 12);
    inplace.add_(rhs);
    inplace.add_(13);
    assertThat(inplace).isEqualTo(lhs.add(rhs).add(12).add(rhs).add(13));
  }

  @Test
  public void test_sub() {
    var empty = ZTensor.zeros(0, 0);
    var lhs = ZTensor.from(new int[][] {{3, 2}, {1, 1}});

    // [2, 2], [2, 2]
    assertThat(ZTensor.Ops.sub(empty, empty)).isEqualTo(empty.sub(empty)).isEqualTo(empty);

    var rhs = ZTensor.from(new int[][] {{-1, 9}, {2, 0}});
    assertThat(ZTensor.Ops.sub(lhs, rhs))
        .isEqualTo(lhs.sub(rhs))
        .isEqualTo(ZTensor.from(new int[][] {{4, -7}, {-1, 1}}));

    assertThatThrownBy(() -> ZTensor.Ops.sub(lhs, empty))
        .isInstanceOf(IndexOutOfBoundsException.class)
        .hasMessageContaining("cannot broadcast shapes: [2, 2], [0, 0]");

    // [2, 2], <scalar>
    assertThat(ZTensor.Ops.sub(empty, 12)).isEqualTo(empty.sub(12)).isEqualTo(empty);

    assertThat(ZTensor.Ops.sub(lhs, 12))
        .isEqualTo(lhs.sub(12))
        .isEqualTo(ZTensor.from(new int[][] {{-9, -10}, {-11, -11}}));

    // <scalar>, [2, 2]
    assertThat(ZTensor.Ops.sub(12, empty)).isEqualTo(empty);

    assertThat(ZTensor.Ops.sub(12, lhs)).isEqualTo(ZTensor.from(new int[][] {{9, 10}, {11, 11}}));

    var inplace = lhs.clone();
    ZTensor.Ops.sub_(inplace, rhs);
    ZTensor.Ops.sub_(inplace, 12);
    inplace.sub_(rhs);
    inplace.sub_(13);
    assertThat(inplace).isEqualTo(lhs.sub(rhs).sub(12).sub(rhs).sub(13));
  }

  @Test
  public void test_mul() {
    var empty = ZTensor.zeros(0, 0);
    var lhs = ZTensor.from(new int[][] {{3, 2}, {1, 1}});

    // [2, 2], [2, 2]
    assertThat(ZTensor.Ops.mul(empty, empty)).isEqualTo(empty.mul(empty)).isEqualTo(empty);

    var rhs = ZTensor.from(new int[][] {{-1, 9}, {2, 0}});
    assertThat(ZTensor.Ops.mul(lhs, rhs))
        .isEqualTo(lhs.mul(rhs))
        .isEqualTo(ZTensor.from(new int[][] {{-3, 18}, {2, 0}}));

    assertThatThrownBy(() -> ZTensor.Ops.mul(lhs, empty))
        .isInstanceOf(IndexOutOfBoundsException.class)
        .hasMessageContaining("cannot broadcast shapes: [2, 2], [0, 0]");

    // [2, 2], <scalar>
    assertThat(ZTensor.Ops.mul(empty, 12)).isEqualTo(empty.mul(12)).isEqualTo(empty);

    assertThat(ZTensor.Ops.mul(lhs, 12))
        .isEqualTo(lhs.mul(12))
        .isEqualTo(ZTensor.from(new int[][] {{36, 24}, {12, 12}}));

    // <scalar>, [2, 2]
    assertThat(ZTensor.Ops.mul(12, empty)).isEqualTo(empty);

    assertThat(ZTensor.Ops.mul(12, lhs)).isEqualTo(ZTensor.from(new int[][] {{36, 24}, {12, 12}}));

    var inplace = lhs.clone();
    ZTensor.Ops.mul_(inplace, rhs);
    ZTensor.Ops.mul_(inplace, 12);
    inplace.mul_(rhs);
    inplace.mul_(13);
    assertThat(inplace).isEqualTo(lhs.mul(rhs).mul(12).mul(rhs).mul(13));
  }

  @Test
  public void test_div() {
    var empty = ZTensor.zeros(0, 0);
    var lhs = ZTensor.from(new int[][] {{24, 12}, {9, 1}});

    // [2, 2], [2, 2]
    assertThat(ZTensor.Ops.div(empty, empty)).isEqualTo(empty.div(empty)).isEqualTo(empty);

    var rhs = ZTensor.from(new int[][] {{-1, 9}, {2, 1}});
    assertThat(ZTensor.Ops.div(lhs, rhs))
        .isEqualTo(lhs.div(rhs))
        .isEqualTo(ZTensor.from(new int[][] {{-24, 1}, {4, 1}}));

    assertThatThrownBy(() -> ZTensor.Ops.div(lhs, empty))
        .isInstanceOf(IndexOutOfBoundsException.class)
        .hasMessageContaining("cannot broadcast shapes: [2, 2], [0, 0]");

    // [2, 2], <scalar>
    assertThat(ZTensor.Ops.div(empty, 12)).isEqualTo(empty.div(12)).isEqualTo(empty);

    assertThat(ZTensor.Ops.div(lhs, 12))
        .isEqualTo(lhs.div(12))
        .isEqualTo(ZTensor.from(new int[][] {{2, 1}, {0, 0}}));

    // <scalar>, [2, 2]
    assertThat(ZTensor.Ops.div(12, empty)).isEqualTo(empty);

    assertThat(ZTensor.Ops.div(12, lhs)).isEqualTo(ZTensor.from(new int[][] {{0, 1}, {1, 12}}));

    // Div by 0
    assertThatThrownBy(() -> ZTensor.Ops.div(lhs, ZTensor.zeros_like(lhs)))
        .isInstanceOf(ArithmeticException.class);

    assertThatThrownBy(() -> ZTensor.Ops.div(lhs, 0)).isInstanceOf(ArithmeticException.class);

    assertThatThrownBy(() -> ZTensor.Ops.div(12, ZTensor.zeros_like(lhs)))
        .isInstanceOf(ArithmeticException.class);

    var inplace = lhs.mul(12345);
    ZTensor.Ops.div_(inplace, rhs);
    ZTensor.Ops.div_(inplace, 12);
    inplace.div_(rhs);
    inplace.div_(13);
    assertThat(inplace).isEqualTo(lhs.mul(12345).div(rhs).div(12).div(rhs).div(13));
  }

  @Test
  public void test_mod() {
    var empty = ZTensor.zeros(0, 0);
    var lhs = ZTensor.from(new int[][] {{24, 12}, {9, 1}});

    // [2, 2], [2, 2]
    assertThat(ZTensor.Ops.mod(empty, empty)).isEqualTo(empty.mod(empty)).isEqualTo(empty);

    var rhs = ZTensor.from(new int[][] {{-1, 9}, {2, 1}});
    assertThat(ZTensor.Ops.mod(lhs, rhs))
        .isEqualTo(lhs.mod(rhs))
        .isEqualTo(ZTensor.from(new int[][] {{0, 3}, {1, 0}}));

    assertThatThrownBy(() -> ZTensor.Ops.mod(lhs, empty))
        .isInstanceOf(IndexOutOfBoundsException.class)
        .hasMessageContaining("cannot broadcast shapes: [2, 2], [0, 0]");

    // [2, 2], <scalar>
    assertThat(ZTensor.Ops.mod(empty, 12)).isEqualTo(empty.mod(12)).isEqualTo(empty);

    assertThat(ZTensor.Ops.mod(lhs, 12))
        .isEqualTo(lhs.mod(12))
        .isEqualTo(ZTensor.from(new int[][] {{0, 0}, {9, 1}}));

    // <scalar>, [2, 2]
    assertThat(ZTensor.Ops.mod(12, empty)).isEqualTo(empty);

    assertThat(ZTensor.Ops.mod(12, lhs)).isEqualTo(ZTensor.from(new int[][] {{12, 0}, {3, 0}}));

    // mod by 0
    assertThatThrownBy(() -> ZTensor.Ops.mod(lhs, ZTensor.zeros_like(lhs)))
        .isInstanceOf(ArithmeticException.class);

    assertThatThrownBy(() -> ZTensor.Ops.mod(lhs, 0)).isInstanceOf(ArithmeticException.class);

    assertThatThrownBy(() -> ZTensor.Ops.mod(12, ZTensor.zeros_like(lhs)))
        .isInstanceOf(ArithmeticException.class);

    var inplace = lhs.mul(12345);
    ZTensor.Ops.mod_(inplace, rhs);
    ZTensor.Ops.mod_(inplace, 12);
    inplace.mod_(rhs);
    inplace.mod_(13);
    assertThat(inplace).isEqualTo(lhs.mul(12345).mod(rhs).mod(12).mod(rhs).mod(13));
  }

  @Test
  public void test_mutable() {
    var tensor = ZTensor.from(new int[][] {{1, 2}, {3, 4}});
    assertThat(tensor.isMutable()).isTrue();
    assertThat(tensor.isReadOnly()).isFalse();

    //noinspection ResultOfMethodCallIgnored
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(tensor::hashCode)
        .withMessageContaining("mutable");

    assertThat(tensor.isMutable()).isTrue();
    tensor.assertMutable();
    tensor.set(new int[] {0, 0}, 5);
    assertThat(tensor).isEqualTo(ZTensor.from(new int[][] {{5, 2}, {3, 4}}));

    var fixed = tensor.immutable();
    assertThat(fixed.isMutable()).isFalse();
    assertThat(fixed.isReadOnly()).isTrue();
    fixed.assertReadOnly();
    assertThat(fixed).isNotSameAs(tensor).extracting(ZTensor::isMutable).isEqualTo(false);
    assertThat(fixed.immutable()).isSameAs(fixed);
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> fixed.set(new int[] {0, 0}, 5))
        .withMessageContaining("immutable");

    assertThat(fixed.hashCode()).isEqualTo(tensor.clone(false).hashCode());
  }

  @Test
  public void test_mutable_view() {
    var tensor = ZTensor.zeros(2, 3);
    var view = tensor.T();
    tensor.set(new int[] {1, 0}, 3);

    assertThat(view).isEqualTo(ZTensor.from(new int[][] {{0, 3}, {0, 0}, {0, 0}}));

    view.add_(2);

    assertThat(tensor).isEqualTo(ZTensor.from(new int[][] {{2, 2, 2}, {5, 2, 2}}));
  }

  public static class JsonExampleContainer {
    public ZTensor tensor;

    //noinspection unused
    @JsonCreator
    public JsonExampleContainer() {}

    public JsonExampleContainer(ZTensor tensor) {
      this.tensor = tensor;
    }

    @Override
    public String toString() {
      return "ExampleContainer{" + "tensor=" + tensor + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof JsonExampleContainer that)) return false;
      return Objects.equals(tensor, that.tensor);
    }

    @Override
    public int hashCode() {
      return Objects.hash(tensor);
    }
  }
}
