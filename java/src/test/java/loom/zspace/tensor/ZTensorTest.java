package loom.zspace.tensor;

import loom.common.JsonUtil;
import loom.linear.LinearDimError;
import loom.testing.CommonAssertions;
import org.junit.Test;

import java.util.Objects;
import java.util.function.BinaryOperator;

public class ZTensorTest implements CommonAssertions {
    @Test
    public void test_scalars() {
        var tensor = ZTensor.scalar(3);

        assertThat(tensor.ndim()).isEqualTo(0);
        assertThat(tensor.shapeAsArray()).isEqualTo(new int[]{});
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
        assertThat(tensor.shapeAsArray()).isEqualTo(new int[]{2});
        assertThat(tensor.shapeAsTensor()).isEqualTo(ZTensor.vector(2));

        assertThat(tensor)
                .hasToString("[3, 7]")
                .isEqualTo(ZTensor.from(new int[]{3, 7}))
                .extracting(ZTensor::toArray)
                .isEqualTo(new int[]{3, 7});

        assertThat(tensor.toT1()).isEqualTo(new int[]{3, 7});

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> ZTensor.scalar(3).toT1());
    }

    @Test
    public void test_matrices() {
        var tensor = ZTensor.matrix(new int[]{3, 7}, new int[]{8, 9});

        assertThat(tensor.ndim()).isEqualTo(2);
        assertThat(tensor.shapeAsArray()).isEqualTo(new int[]{2, 2});
        assertThat(tensor.shapeAsTensor()).isEqualTo(ZTensor.vector(2, 2));

        assertThat(tensor)
                .hasToString("[[3, 7], [8, 9]]")
                .isEqualTo(ZTensor.from(new int[][]{{3, 7}, {8, 9}}))
                .extracting(ZTensor::toArray)
                .isEqualTo(new int[][]{{3, 7}, {8, 9}});

        assertThat(tensor.toT2()).isEqualTo(new int[][]{{3, 7}, {8, 9}});

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

        ZTensor t = ZTensor.from(new int[][]{{2, 3}, {4, 5}});
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
        ZTensor t2 = ZTensor.matrix(new int[]{2, 3}, new int[]{4, 5});

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
        ZTensor t = ZTensor.from(new int[][]{{2, 3}, {4, 5}});

        assertThat(t.ndim()).isEqualTo(2);
        assertThat(t.size()).isEqualTo(4);
        assertThat(t.get(1, 0)).isEqualTo(4);

        ZTensor t2 = t.add(2);

        assertThat(t2.toArray()).isEqualTo(new int[][]{{4, 5}, {6, 7}});
    }

    @Test
    public void test_toString() {
        ZTensor t = ZTensor.matrix(new int[][]{{2, 3}, {4, 5}});
        assertThat(t.toString()).isEqualTo("[[2, 3], [4, 5]]");
    }

    @Test
    public void test_stream() {
        ZTensor t = ZTensor.matrix(new int[][]{{2, 3}, {4, 5}});

        var points = t.coordsStream().map(int[]::clone).toList();

        assertThat(points)
                .contains(new int[]{0, 0}, new int[]{0, 1}, new int[]{1, 0}, new int[]{1, 1});
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

            assertThat(ZTensor.Ops.binOp(fn, lhs, rhs).toArray()).isEqualTo(new int[]{1, 20});
            assertThatThrownBy(() -> ZTensor.Ops.binOp(fn, lhs, empty))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Shapes don't match: [2] vs [0]");

            // [2], <scalar>
            assertThat(ZTensor.Ops.binOp(fn, empty, 12)).isEqualTo(empty);
            assertThat(ZTensor.Ops.binOp(fn, lhs, 12).toArray()).isEqualTo(new int[]{27, 26});

            // <scalar>, [2]
            assertThat(ZTensor.Ops.binOp(fn, 12, empty)).isEqualTo(empty);
            assertThat(ZTensor.Ops.binOp(fn, 12, lhs).toArray()).isEqualTo(new int[]{18, 16});
        }

        {
            ZTensor empty = ZTensor.matrix();
            ZTensor lhs = ZTensor.from(new int[][]{{3, 2}, {1, 1}});

            // [2, 2], [2, 2]
            ZTensor rhs = ZTensor.from(new int[][]{{-1, 9}, {2, 0}});
            assertThat(ZTensor.Ops.binOp(fn, empty, empty)).isEqualTo(empty);
            assertThat(ZTensor.Ops.binOp(fn, lhs, rhs).toArray())
                    .isEqualTo(new int[][]{{1, 20}, {5, 1}});
            assertThatThrownBy(() -> ZTensor.Ops.binOp(fn, lhs, empty))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Shapes don't match: [2, 2] vs [0, 0]");

            // [2, 2], <scalar>
            assertThat(ZTensor.Ops.binOp(fn, empty, 12)).isEqualTo(empty);
            assertThat(ZTensor.Ops.binOp(fn, lhs, 12).toArray())
                    .isEqualTo(new int[][]{{27, 26}, {25, 25}});

            // <scalar>, [2, 2]
            assertThat(ZTensor.Ops.binOp(fn, 12, empty)).isEqualTo(empty);
            assertThat(ZTensor.Ops.binOp(fn, 12, lhs).toArray())
                    .isEqualTo(new int[][]{{18, 16}, {14, 14}});
        }
    }

    @Test
    public void test_add() throws LinearDimError {
        var empty = ZTensor.zeros(0, 0);
        var lhs = ZTensor.from(new int[][]{{3, 2}, {1, 1}});

        // [2, 2], [2, 2]
        assertThat(ZTensor.Ops.add(empty, empty)).isEqualTo(empty.add(empty)).isEqualTo(empty);

        var rhs = ZTensor.from(new int[][]{{-1, 9}, {2, 0}});
        assertThat(ZTensor.Ops.add(lhs, rhs))
                .isEqualTo(lhs.add(rhs))
                .isEqualTo(ZTensor.from(new int[][]{{2, 11}, {3, 1}}));

        assertThatThrownBy(() -> ZTensor.Ops.add(lhs, empty))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Shapes don't match: [2, 2] vs [0, 0]");

        // [2, 2], <scalar>
        assertThat(ZTensor.Ops.add(empty, 12)).isEqualTo(empty.add(12)).isEqualTo(empty);

        assertThat(ZTensor.Ops.add(lhs, 12))
                .isEqualTo(lhs.add(12))
                .isEqualTo(ZTensor.from(new int[][]{{15, 14}, {13, 13}}));

        // <scalar>, [2, 2]
        assertThat(ZTensor.Ops.add(12, empty)).isEqualTo(empty);

        assertThat(ZTensor.Ops.add(12, lhs)).isEqualTo(ZTensor.from(new int[][]{{15, 14}, {13, 13}}));
    }

    @Test
    public void test_sub() throws LinearDimError {
        var empty = ZTensor.zeros(0, 0);
        var lhs = ZTensor.from(new int[][]{{3, 2}, {1, 1}});

        // [2, 2], [2, 2]
        assertThat(ZTensor.Ops.sub(empty, empty)).isEqualTo(empty.sub(empty)).isEqualTo(empty);

        var rhs = ZTensor.from(new int[][]{{-1, 9}, {2, 0}});
        assertThat(ZTensor.Ops.sub(lhs, rhs))
                .isEqualTo(lhs.sub(rhs))
                .isEqualTo(ZTensor.from(new int[][]{{4, -7}, {-1, 1}}));

        assertThatThrownBy(() -> ZTensor.Ops.sub(lhs, empty))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Shapes don't match: [2, 2] vs [0, 0]");

        // [2, 2], <scalar>
        assertThat(ZTensor.Ops.sub(empty, 12)).isEqualTo(empty.sub(12)).isEqualTo(empty);

        assertThat(ZTensor.Ops.sub(lhs, 12))
                .isEqualTo(lhs.sub(12))
                .isEqualTo(ZTensor.from(new int[][]{{-9, -10}, {-11, -11}}));

        // <scalar>, [2, 2]
        assertThat(ZTensor.Ops.sub(12, empty)).isEqualTo(empty);

        assertThat(ZTensor.Ops.sub(12, lhs)).isEqualTo(ZTensor.from(new int[][]{{9, 10}, {11, 11}}));
    }

    @Test
    public void test_mul() throws LinearDimError {
        var empty = ZTensor.zeros(0, 0);
        var lhs = ZTensor.from(new int[][]{{3, 2}, {1, 1}});

        // [2, 2], [2, 2]
        assertThat(ZTensor.Ops.mul(empty, empty)).isEqualTo(empty.mul(empty)).isEqualTo(empty);

        var rhs = ZTensor.from(new int[][]{{-1, 9}, {2, 0}});
        assertThat(ZTensor.Ops.mul(lhs, rhs))
                .isEqualTo(lhs.mul(rhs))
                .isEqualTo(ZTensor.from(new int[][]{{-3, 18}, {2, 0}}));

        assertThatThrownBy(() -> ZTensor.Ops.mul(lhs, empty))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Shapes don't match: [2, 2] vs [0, 0]");

        // [2, 2], <scalar>
        assertThat(ZTensor.Ops.mul(empty, 12)).isEqualTo(empty.mul(12)).isEqualTo(empty);

        assertThat(ZTensor.Ops.mul(lhs, 12))
                .isEqualTo(lhs.mul(12))
                .isEqualTo(ZTensor.from(new int[][]{{36, 24}, {12, 12}}));

        // <scalar>, [2, 2]
        assertThat(ZTensor.Ops.mul(12, empty)).isEqualTo(empty);

        assertThat(ZTensor.Ops.mul(12, lhs)).isEqualTo(ZTensor.from(new int[][]{{36, 24}, {12, 12}}));
    }

    @Test
    public void test_div() throws LinearDimError {
        var empty = ZTensor.zeros(0, 0);
        var lhs = ZTensor.from(new int[][]{{24, 12}, {9, 1}});

        // [2, 2], [2, 2]
        assertThat(ZTensor.Ops.div(empty, empty)).isEqualTo(empty.div(empty)).isEqualTo(empty);

        var rhs = ZTensor.from(new int[][]{{-1, 9}, {2, 1}});
        assertThat(ZTensor.Ops.div(lhs, rhs))
                .isEqualTo(lhs.div(rhs))
                .isEqualTo(ZTensor.from(new int[][]{{-24, 1}, {4, 1}}));

        assertThatThrownBy(() -> ZTensor.Ops.div(lhs, empty))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Shapes don't match: [2, 2] vs [0, 0]");

        // [2, 2], <scalar>
        assertThat(ZTensor.Ops.div(empty, 12)).isEqualTo(empty.div(12)).isEqualTo(empty);

        assertThat(ZTensor.Ops.div(lhs, 12))
                .isEqualTo(lhs.div(12))
                .isEqualTo(ZTensor.from(new int[][]{{2, 1}, {0, 0}}));

        // <scalar>, [2, 2]
        assertThat(ZTensor.Ops.div(12, empty)).isEqualTo(empty);

        assertThat(ZTensor.Ops.div(12, lhs)).isEqualTo(ZTensor.from(new int[][]{{0, 1}, {1, 12}}));

        // Div by 0
        assertThatThrownBy(() -> ZTensor.Ops.div(lhs, ZTensor.zeros_like(lhs)))
                .isInstanceOf(ArithmeticException.class);

        assertThatThrownBy(() -> ZTensor.Ops.div(lhs, 0)).isInstanceOf(ArithmeticException.class);

        assertThatThrownBy(() -> ZTensor.Ops.div(12, ZTensor.zeros_like(lhs)))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    public void test_mod() throws LinearDimError {
        var empty = ZTensor.zeros(0, 0);
        var lhs = ZTensor.from(new int[][]{{24, 12}, {9, 1}});

        // [2, 2], [2, 2]
        assertThat(ZTensor.Ops.mod(empty, empty)).isEqualTo(empty.mod(empty)).isEqualTo(empty);

        var rhs = ZTensor.from(new int[][]{{-1, 9}, {2, 1}});
        assertThat(ZTensor.Ops.mod(lhs, rhs))
                .isEqualTo(lhs.mod(rhs))
                .isEqualTo(ZTensor.from(new int[][]{{0, 3}, {1, 0}}));

        assertThatThrownBy(() -> ZTensor.Ops.mod(lhs, empty))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Shapes don't match: [2, 2] vs [0, 0]");

        // [2, 2], <scalar>
        assertThat(ZTensor.Ops.mod(empty, 12)).isEqualTo(empty.mod(12)).isEqualTo(empty);

        assertThat(ZTensor.Ops.mod(lhs, 12))
                .isEqualTo(lhs.mod(12))
                .isEqualTo(ZTensor.from(new int[][]{{0, 0}, {9, 1}}));

        // <scalar>, [2, 2]
        assertThat(ZTensor.Ops.mod(12, empty)).isEqualTo(empty);

        assertThat(ZTensor.Ops.mod(12, lhs)).isEqualTo(ZTensor.from(new int[][]{{12, 0}, {3, 0}}));

        // mod by 0
        assertThatThrownBy(() -> ZTensor.Ops.mod(lhs, ZTensor.zeros_like(lhs)))
                .isInstanceOf(ArithmeticException.class);

        assertThatThrownBy(() -> ZTensor.Ops.mod(lhs, 0)).isInstanceOf(ArithmeticException.class);

        assertThatThrownBy(() -> ZTensor.Ops.mod(12, ZTensor.zeros_like(lhs)))
                .isInstanceOf(ArithmeticException.class);
    }

    static class JsonExampleContainer {
        public ZTensor tensor;

        public JsonExampleContainer() {
        }

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
            if (!(o instanceof JsonExampleContainer)) return false;
            JsonExampleContainer that = (JsonExampleContainer) o;
            return Objects.equals(tensor, that.tensor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tensor);
        }
    }
}
