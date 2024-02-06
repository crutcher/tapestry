package org.tensortapestry.zspace;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.tensortapestry.zspace.exceptions.ZDimMissMatchError;
import org.tensortapestry.zspace.experimental.ZSpaceTestAssertions;
import org.tensortapestry.zspace.ops.DominanceOrderingOps;

public class ZPointTest implements ZSpaceTestAssertions {

    @Test
    public void test_clone() {
        var p = new ZPoint(1, 2, 3);
        assertThat(p.clone()).isEqualTo(p).isSameAs(p);
    }

    @Test
    public void test_create() {
        var p = new ZPoint(1, 2, 3);
        assertThat(p.create(ZTensor.newVector(3, 4)))
                .isInstanceOf(ZPoint.class)
                .isEqualTo(new ZPoint(3, 4));
    }

    @Test
    public void test_of() {
        assertThat(ZPoint.of(1, 2, 3)).isEqualTo(new ZPoint(1, 2, 3));
    }

    @Test
    public void test_constructor() {
        {
            var p = new ZPoint(1, 2, 3);
            assertThat(p.getNDim()).isEqualTo(3);
            assertThat(p).isEqualTo(ZTensor.newVector(1, 2, 3));
        }

        {
            var p = new ZPoint(ZTensor.newVector(1, 2, 3));
            assertThat(p.getNDim()).isEqualTo(3);
            assertThat(p).isEqualTo(ZTensor.newVector(1, 2, 3));
        }

        {
            var p = new ZPoint(List.of(1, 2, 3));
            assertThat(p.getNDim()).isEqualTo(3);
            assertThat(p).isEqualTo(ZTensor.newVector(1, 2, 3));
        }

        assertThatExceptionOfType(ZDimMissMatchError.class)
                .isThrownBy(() -> new ZPoint(ZTensor.newScalar(3)));

        assertThatExceptionOfType(ZDimMissMatchError.class)
                .isThrownBy(() -> new ZPoint(ZTensor.newZeros(2, 3)));
    }

    @Test
    public void test_resolveDim() {
        var p = new ZPoint(1, 2, 3);
        assertThat(p.resolveDim(0)).isEqualTo(0);
        assertThat(p.resolveDim(1)).isEqualTo(1);
        assertThat(p.resolveDim(2)).isEqualTo(2);
        assertThat(p.resolveDim(-1)).isEqualTo(2);
        assertThat(p.resolveDim(-2)).isEqualTo(1);
        assertThat(p.resolveDim(-3)).isEqualTo(0);

        assertThatExceptionOfType(IndexOutOfBoundsException.class)
                .isThrownBy(() -> p.resolveDim(3))
                .withMessageContaining("invalid dimension: index 3 out of range [0, 3)");
        assertThatExceptionOfType(IndexOutOfBoundsException.class)
                .isThrownBy(() -> p.resolveDim(-4))
                .withMessageContaining("invalid dimension: index -4 out of range [0, 3)");
    }

    @Test
    public void test_newOnes() {
        assertThat(ZPoint.newOnes(3)).isEqualTo(new ZPoint(1, 1, 1));
        assertThat(ZPoint.newOnes(0)).isEqualTo(new ZPoint());

        assertThat(ZPoint.newOnesLike(new ZPoint(1, 2, 3))).isEqualTo(new ZPoint(1, 1, 1));
    }

    @Test
    public void test_newZeros() {
        assertThat(ZPoint.newZeros(3)).isEqualTo(new ZPoint(0, 0, 0));
        assertThat(ZPoint.newZeros(0)).isEqualTo(new ZPoint());

        assertThat(ZPoint.newZerosLike(new ZPoint(1, 2, 3))).isEqualTo(new ZPoint(0, 0, 0));
    }

    @Test
    public void test_get() {
        ZPoint p = new ZPoint(1, 2, 3);
        assertThat(p.get(0)).isEqualTo(1);
        assertThat(p.get(1)).isEqualTo(2);
        assertThat(p.get(2)).isEqualTo(3);
    }

    @Test
    public void test_toArray() {
        ZPoint p = new ZPoint(1, 2, 3);
        assertThat(p.toArray()).isEqualTo(new int[]{1, 2, 3});
    }

    @Test
    public void test_hashCode() {
        ZPoint p = new ZPoint(1, 2, 3);
        assertThat(p).hasSameHashCodeAs(new ZPoint(1, 2, 3));
    }

    @Test
    public void test_string_parse_json() {
        ZPoint p = new ZPoint(2, 3);
        String str = "[2, 3]";

        assertThat(p).hasToString(str);
        assertThat(ZPoint.parse(str)).isEqualTo(p);
        assertObjectJsonEquivalence(p, str);

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> ZPoint.parse("abc"));
    }

    @Test
    public void test_permute() {
        var p = new ZPoint(8, 9, 10);
        assertThat(p.resolvePermutation(-2, 2, 0)).isEqualTo(new int[]{1, 2, 0});
        assertThat(p.permute(1, 2, 0)).isEqualTo(new ZPoint(9, 10, 8));
    }

    @Test
    public void test_ordering() {
        var zeros = new ZPoint(0, 0);

        var p10 = new ZPoint(1, 0);
        var p01 = new ZPoint(0, 1);

        assertThat(DominanceOrderingOps.partialOrderByGrid(zeros, zeros))
                .isEqualTo(DominanceOrderingOps.PartialOrdering.EQUAL);
        assertThat(DominanceOrderingOps.lt(zeros, zeros)).isFalse();
        assertThat(DominanceOrderingOps.le(zeros, zeros)).isTrue();
        assertThat(DominanceOrderingOps.eq(zeros, zeros)).isTrue();
        assertThat(DominanceOrderingOps.ne(zeros, zeros)).isFalse();
        assertThat(DominanceOrderingOps.ge(zeros, zeros)).isTrue();
        assertThat(DominanceOrderingOps.gt(zeros, zeros)).isFalse();

        assertThat(zeros.lt(zeros)).isFalse();
        assertThat(zeros.le(zeros)).isTrue();
        assertThat(zeros.eq(zeros)).isTrue();
        assertThat(zeros.ne(zeros)).isFalse();
        assertThat(zeros.ge(zeros)).isTrue();
        assertThat(zeros.gt(zeros)).isFalse();

        assertThat(DominanceOrderingOps.partialOrderByGrid(zeros, p01))
                .isEqualTo(DominanceOrderingOps.PartialOrdering.LESS_THAN);
        assertThat(DominanceOrderingOps.lt(zeros, p01)).isTrue();
        assertThat(DominanceOrderingOps.le(zeros, p01)).isTrue();
        assertThat(DominanceOrderingOps.eq(zeros, p01)).isFalse();
        assertThat(DominanceOrderingOps.ne(zeros, p01)).isTrue();
        assertThat(DominanceOrderingOps.ge(zeros, p01)).isFalse();
        assertThat(DominanceOrderingOps.gt(zeros, p01)).isFalse();
        assertThat(zeros.lt(p01)).isTrue();
        assertThat(zeros.le(p01)).isTrue();
        assertThat(zeros.eq(p01)).isFalse();
        assertThat(zeros.ne(p01)).isTrue();
        assertThat(zeros.ge(p01)).isFalse();
        assertThat(zeros.gt(p01)).isFalse();

        assertThat(DominanceOrderingOps.partialOrderByGrid(p01, zeros))
                .isEqualTo(DominanceOrderingOps.PartialOrdering.GREATER_THAN);
        assertThat(DominanceOrderingOps.lt(p01, zeros)).isFalse();
        assertThat(DominanceOrderingOps.le(p01, zeros)).isFalse();
        assertThat(DominanceOrderingOps.eq(p01, zeros)).isFalse();
        assertThat(DominanceOrderingOps.ne(p01, zeros)).isTrue();
        assertThat(DominanceOrderingOps.ge(p01, zeros)).isTrue();
        assertThat(DominanceOrderingOps.gt(p01, zeros)).isTrue();
        assertThat(p01.lt(zeros)).isFalse();
        assertThat(p01.le(zeros)).isFalse();
        assertThat(p01.eq(zeros)).isFalse();
        assertThat(p01.ne(zeros)).isTrue();
        assertThat(p01.ge(zeros)).isTrue();
        assertThat(p01.gt(zeros)).isTrue();

        assertThat(DominanceOrderingOps.partialOrderByGrid(zeros, p10))
                .isEqualTo(DominanceOrderingOps.PartialOrdering.LESS_THAN);
        assertThat(DominanceOrderingOps.lt(zeros, p10)).isTrue();
        assertThat(DominanceOrderingOps.le(zeros, p10)).isTrue();
        assertThat(DominanceOrderingOps.eq(zeros, p10)).isFalse();
        assertThat(DominanceOrderingOps.ne(zeros, p10)).isTrue();
        assertThat(DominanceOrderingOps.ge(zeros, p10)).isFalse();
        assertThat(DominanceOrderingOps.gt(zeros, p10)).isFalse();
        assertThat(zeros.lt(p10)).isTrue();
        assertThat(zeros.le(p10)).isTrue();
        assertThat(zeros.eq(p10)).isFalse();
        assertThat(zeros.ne(p10)).isTrue();
        assertThat(zeros.ge(p10)).isFalse();
        assertThat(zeros.gt(p10)).isFalse();

        assertThat(DominanceOrderingOps.partialOrderByGrid(p01, p10))
                .isEqualTo(DominanceOrderingOps.PartialOrdering.INCOMPARABLE);
        assertThat(DominanceOrderingOps.lt(p01, p10)).isFalse();
        assertThat(DominanceOrderingOps.le(p01, p10)).isFalse();
        assertThat(DominanceOrderingOps.eq(p01, p10)).isFalse();
        assertThat(DominanceOrderingOps.ne(p01, p10)).isTrue();
        assertThat(DominanceOrderingOps.ge(p01, p10)).isFalse();
        assertThat(DominanceOrderingOps.gt(p01, p10)).isFalse();
        assertThat(p01.lt(p10)).isFalse();
        assertThat(p01.le(p10)).isFalse();
        assertThat(p01.eq(p10)).isFalse();
        assertThat(p01.ne(p10)).isTrue();
        assertThat(p01.ge(p10)).isFalse();
        assertThat(p01.gt(p10)).isFalse();
    }

    @Test
    public void test_addDims() {
        var p = new ZPoint(1, 2, 3);

        assertThat(p.addDims(0, 0)).isSameAs(p);

        assertThat(p.addDims(0, 2)).isEqualTo(new ZPoint(0, 0, 1, 2, 3));
        assertThat(p.addDims(3, 2)).isEqualTo(new ZPoint(1, 2, 3, 0, 0));
        assertThat(p.addDims(2, 2)).isEqualTo(new ZPoint(1, 2, 0, 0, 3));
        assertThat(p.addDims(-2, 2)).isEqualTo(new ZPoint(1, 2, 0, 0, 3));
        assertThat(p.addDims(-1, 2)).isEqualTo(new ZPoint(1, 2, 3, 0, 0));

        assertThatExceptionOfType(IndexOutOfBoundsException.class)
                .isThrownBy(() -> p.addDims(4, 2))
                .withMessageContaining("invalid dimension: index 4 out of range [0, 4)");

        assertThatExceptionOfType(IndexOutOfBoundsException.class)
                .isThrownBy(() -> p.addDims(-5, 2))
                .withMessageContaining("invalid dimension: index -5 out of range [0, 4)");

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> p.addDims(0, -1))
                .withMessageContaining("newDims must be non-negative");
    }
}
