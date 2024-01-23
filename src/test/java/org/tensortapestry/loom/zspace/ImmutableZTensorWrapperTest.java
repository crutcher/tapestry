package org.tensortapestry.loom.zspace;

import javax.annotation.Nonnull;
import org.tensortapestry.loom.testing.BaseTestClass;
import org.junit.Test;

public class ImmutableZTensorWrapperTest extends BaseTestClass {

  public static class Wrapper extends ImmutableZTensorWrapper<Wrapper> {

    public Wrapper(HasZTensor tensor) {
      super(tensor);
    }

    @Override
    protected @Nonnull Wrapper create(@Nonnull HasZTensor tensor) {
      return new Wrapper(tensor);
    }
  }

  public Wrapper createWrapper(HasZTensor tensor) {
    return new Wrapper(tensor);
  }

  @Test
  public void test_new() {
    ZTensor tensor = ZTensor.newVector(1, 2, 3);
    assertThat(createWrapper(tensor).getTensor()).isEqualTo(tensor).isNotSameAs(tensor);

    ZTensor immutable = tensor.asImmutable();
    assertThat(createWrapper(immutable).getTensor()).isEqualTo(immutable).isSameAs(immutable);
  }

  @Test
  public void test_hashCode() {
    ZTensor tensor = ZTensor.newVector(1, 2, 3);
    Wrapper wrapper = createWrapper(tensor);
    assertThat(wrapper.hashCode()).isEqualTo(tensor.asImmutable().hashCode());
  }

  @Test
  public void test_toString() {
    ZTensor tensor = ZTensor.newVector(1, 2, 3);
    Wrapper wrapper = createWrapper(tensor);
    assertThat(wrapper).hasToString(tensor.toString());
  }

  @Test
  public void test_clone() {
    Wrapper wrapper = createWrapper(ZTensor.newVector(1, 2, 3));
    Wrapper clone = wrapper.clone();
    assertThat(clone).isSameAs(wrapper);
  }

  @Test
  public void test_isStrictlyPositive() {
    assertThat(createWrapper(ZTensor.newScalar(1)).isStrictlyPositive()).isTrue();
    assertThat(createWrapper(ZTensor.newScalar(0)).isStrictlyPositive()).isFalse();

    assertThat(createWrapper(ZTensor.newVector(1, 2, 3)).isStrictlyPositive()).isTrue();
    assertThat(createWrapper(ZTensor.newVector(1, 0, 3)).isStrictlyPositive()).isFalse();
    assertThat(createWrapper(ZTensor.newVector(1, -0, 3)).isStrictlyPositive()).isFalse();
  }

  @Test
  public void test_isNonNegative() {
    assertThat(createWrapper(ZTensor.newScalar(1)).isNonNegative()).isTrue();
    assertThat(createWrapper(ZTensor.newScalar(0)).isNonNegative()).isTrue();
    assertThat(createWrapper(ZTensor.newScalar(-1)).isNonNegative()).isFalse();

    assertThat(createWrapper(ZTensor.newVector(1, 2, 3)).isNonNegative()).isTrue();
    assertThat(createWrapper(ZTensor.newVector(1, 0, 3)).isNonNegative()).isTrue();
    assertThat(createWrapper(ZTensor.newVector(1, -0, 3)).isNonNegative()).isTrue();
    assertThat(createWrapper(ZTensor.newVector(1, -1, 3)).isNonNegative()).isFalse();
  }

  @Test
  public void test_eq() {
    Wrapper wrapper = createWrapper(ZTensor.newVector(1, 2, 3));
    assertThat(wrapper)
      .isEqualTo(wrapper)
      .isEqualTo(createWrapper(ZTensor.newVector(1, 2, 3)))
      .isEqualTo(ZTensor.newVector(1, 2, 3))
      .isNotEqualTo(createWrapper(ZTensor.newVector(1, 2, 4)))
      .isNotEqualTo(createWrapper(ZTensor.newVector(1, 2, 3, 4)))
      .isNotEqualTo(createWrapper(ZTensor.newVector(1, 2)))
      .isNotEqualTo(null)
      .isNotEqualTo(new Object());

    assertThatExceptionOfType(ZDimMissMatchError.class)
      .isThrownBy(() -> wrapper.eq(createWrapper(ZTensor.newVector(1, 2))))
      .withMessage("ZDim shape mismatch: [3] != [2]");
  }

  @Test
  public void test_lt() {
    Wrapper wrapper = createWrapper(ZTensor.newVector(1, 2, 3));
    assertThat(wrapper.lt(wrapper)).isFalse();
    assertThat(wrapper.lt(createWrapper(ZTensor.newVector(1, 2, 3)))).isFalse();
    assertThat(wrapper.lt(ZTensor.newVector(1, 2, 3))).isFalse();
    assertThat(wrapper.lt(createWrapper(ZTensor.newVector(1, 2, 4)))).isTrue();

    // Non-dominated points are not less than each other.
    assertThat(wrapper.lt(createWrapper(ZTensor.newVector(5, 2, -1)))).isFalse();

    assertThatExceptionOfType(ZDimMissMatchError.class)
      .isThrownBy(() -> wrapper.lt(createWrapper(ZTensor.newVector(1, 2))))
      .withMessage("ZDim shape mismatch: [3] != [2]");
  }

  @Test
  public void test_le() {
    Wrapper wrapper = createWrapper(ZTensor.newVector(1, 2, 3));
    assertThat(wrapper.le(wrapper)).isTrue();
    assertThat(wrapper.le(createWrapper(ZTensor.newVector(1, 2, 3)))).isTrue();
    assertThat(wrapper.le(ZTensor.newVector(1, 2, 3))).isTrue();
    assertThat(wrapper.le(createWrapper(ZTensor.newVector(1, 2, 4)))).isTrue();
    assertThat(wrapper.le(createWrapper(ZTensor.newVector(1, 2, 2)))).isFalse();

    // Non-dominated points are not less than or equal to each other.
    assertThat(wrapper.le(createWrapper(ZTensor.newVector(5, 2, -1)))).isFalse();

    assertThatExceptionOfType(ZDimMissMatchError.class)
      .isThrownBy(() -> wrapper.le(createWrapper(ZTensor.newVector(1, 2))))
      .withMessage("ZDim shape mismatch: [3] != [2]");
  }

  @Test
  public void test_gt() {
    Wrapper wrapper = createWrapper(ZTensor.newVector(1, 2, 3));
    assertThat(wrapper.gt(wrapper)).isFalse();
    assertThat(wrapper.gt(createWrapper(ZTensor.newVector(1, 2, 3)))).isFalse();
    assertThat(wrapper.gt(ZTensor.newVector(1, 2, 3))).isFalse();
    assertThat(wrapper.gt(createWrapper(ZTensor.newVector(1, 2, 4)))).isFalse();
    assertThat(wrapper.gt(createWrapper(ZTensor.newVector(1, 2, 2)))).isTrue();

    // Non-dominated points are not greater than each other.
    assertThat(wrapper.gt(createWrapper(ZTensor.newVector(5, 2, -1)))).isFalse();

    assertThatExceptionOfType(ZDimMissMatchError.class)
      .isThrownBy(() -> wrapper.gt(createWrapper(ZTensor.newVector(1, 2))))
      .withMessage("ZDim shape mismatch: [3] != [2]");
  }

  @Test
  public void test_ge() {
    Wrapper wrapper = createWrapper(ZTensor.newVector(1, 2, 3));
    assertThat(wrapper.ge(wrapper)).isTrue();
    assertThat(wrapper.ge(createWrapper(ZTensor.newVector(1, 2, 3)))).isTrue();
    assertThat(wrapper.ge(ZTensor.newVector(1, 2, 3))).isTrue();
    assertThat(wrapper.ge(createWrapper(ZTensor.newVector(1, 2, 4)))).isFalse();
    assertThat(wrapper.ge(createWrapper(ZTensor.newVector(1, 2, 2)))).isTrue();

    // Non-dominated points are not greater than or equal to each other.
    assertThat(wrapper.ge(createWrapper(ZTensor.newVector(5, 2, -1)))).isFalse();

    assertThatExceptionOfType(ZDimMissMatchError.class)
      .isThrownBy(() -> wrapper.ge(createWrapper(ZTensor.newVector(1, 2))))
      .withMessage("ZDim shape mismatch: [3] != [2]");
  }

  @Test
  public void test_sumAsInt() {
    Wrapper wrapper = createWrapper(ZTensor.newVector(1, 2, 3));
    assertThat(wrapper.sumAsInt()).isEqualTo(6);
  }

  @Test
  public void test_prodAsInt() {
    Wrapper wrapper = createWrapper(ZTensor.newVector(1, 2, 3));
    assertThat(wrapper.prodAsInt()).isEqualTo(6);
  }

  @Test
  public void test_maxAsInt() {
    Wrapper wrapper = createWrapper(ZTensor.newVector(1, 2, 3));
    assertThat(wrapper.maxAsInt()).isEqualTo(3);
  }

  @Test
  public void test_minAsInt() {
    Wrapper wrapper = createWrapper(ZTensor.newVector(1, 2, 3));
    assertThat(wrapper.minAsInt()).isEqualTo(1);
  }

  @Test
  public void test_neg() {
    Wrapper wrapper = createWrapper(ZTensor.newVector(1, 2, 3));
    assertThat(wrapper.neg()).isEqualTo(createWrapper(ZTensor.newVector(-1, -2, -3)));
  }

  @Test
  public void test_abs() {
    Wrapper wrapper = createWrapper(ZTensor.newVector(1, -2, 3));
    assertThat(wrapper.abs()).isEqualTo(createWrapper(ZTensor.newVector(1, 2, 3)));
  }

  @Test
  public void test_add() {
    {
      Wrapper wrapper = createWrapper(ZTensor.newVector(1, 2, 3));
      assertThat(wrapper.add(createWrapper(ZTensor.newVector(1, 2, 3))))
        .isEqualTo(createWrapper(ZTensor.newVector(2, 4, 6)));
    }
    {
      Wrapper wrapper = createWrapper(ZTensor.newVector(1, 2, 3));
      assertThat(wrapper.add(10)).isEqualTo(createWrapper(ZTensor.newVector(11, 12, 13)));
    }
    {
      Wrapper wrapper = createWrapper(ZTensor.newMatrix(new int[] { 1 }, new int[] { 2 }));
      assertThat(wrapper.add(createWrapper(ZTensor.newVector(1, 2, 3))))
        .isEqualTo(createWrapper(ZTensor.newMatrix(new int[] { 2, 3, 4 }, new int[] { 3, 4, 5 })));
    }

    assertThatExceptionOfType(IndexOutOfBoundsException.class)
      .isThrownBy(() ->
        createWrapper(ZTensor.newVector(1, 2, 3)).add(createWrapper(ZTensor.newVector(1, 2)))
      )
      .withMessage("cannot broadcast shapes: [3], [2]");
  }

  @Test
  public void test_sub() {
    {
      Wrapper wrapper = createWrapper(ZTensor.newVector(1, 2, 3));
      assertThat(wrapper.sub(createWrapper(ZTensor.newVector(1, 2, 3))))
        .isEqualTo(createWrapper(ZTensor.newVector(0, 0, 0)));
    }
    {
      Wrapper wrapper = createWrapper(ZTensor.newVector(1, 2, 3));
      assertThat(wrapper.sub(10)).isEqualTo(createWrapper(ZTensor.newVector(-9, -8, -7)));
    }
    {
      Wrapper wrapper = createWrapper(ZTensor.newMatrix(new int[] { 1 }, new int[] { 2 }));
      assertThat(wrapper.sub(createWrapper(ZTensor.newVector(1, 2, 3))))
        .isEqualTo(
          createWrapper(ZTensor.newMatrix(new int[] { 0, -1, -2 }, new int[] { 1, 0, -1 }))
        );
    }

    assertThatExceptionOfType(IndexOutOfBoundsException.class)
      .isThrownBy(() ->
        createWrapper(ZTensor.newVector(1, 2, 3)).sub(createWrapper(ZTensor.newVector(1, 2)))
      )
      .withMessage("cannot broadcast shapes: [3], [2]");
  }

  @Test
  public void test_mul() {
    {
      Wrapper wrapper = createWrapper(ZTensor.newVector(1, 2, 3));
      assertThat(wrapper.mul(createWrapper(ZTensor.newVector(1, 2, 3))))
        .isEqualTo(createWrapper(ZTensor.newVector(1, 4, 9)));
    }
    {
      Wrapper wrapper = createWrapper(ZTensor.newVector(1, 2, 3));
      assertThat(wrapper.mul(10)).isEqualTo(createWrapper(ZTensor.newVector(10, 20, 30)));
    }
    {
      Wrapper wrapper = createWrapper(ZTensor.newMatrix(new int[] { 1 }, new int[] { 2 }));
      assertThat(wrapper.mul(createWrapper(ZTensor.newVector(1, 2, 3))))
        .isEqualTo(createWrapper(ZTensor.newMatrix(new int[] { 1, 2, 3 }, new int[] { 2, 4, 6 })));
    }

    assertThatExceptionOfType(IndexOutOfBoundsException.class)
      .isThrownBy(() ->
        createWrapper(ZTensor.newVector(1, 2, 3)).mul(createWrapper(ZTensor.newVector(1, 2)))
      )
      .withMessage("cannot broadcast shapes: [3], [2]");
  }

  @Test
  public void test_div() {
    {
      Wrapper wrapper = createWrapper(ZTensor.newVector(1, 2, 3));
      assertThat(wrapper.div(createWrapper(ZTensor.newVector(1, 2, 3))))
        .isEqualTo(createWrapper(ZTensor.newVector(1, 1, 1)));
    }
    {
      Wrapper wrapper = createWrapper(ZTensor.newVector(1, 2, 3));
      assertThat(wrapper.div(10)).isEqualTo(createWrapper(ZTensor.newVector(0, 0, 0)));
    }
    {
      Wrapper wrapper = createWrapper(ZTensor.newMatrix(new int[] { 1 }, new int[] { 2 }));
      assertThat(wrapper.div(createWrapper(ZTensor.newVector(1, 2, 3))))
        .isEqualTo(createWrapper(ZTensor.newMatrix(new int[] { 1, 0, 0 }, new int[] { 2, 1, 0 })));
    }

    {
      Wrapper wrapper = createWrapper(ZTensor.newVector(1, 2, 3));
      assertThatExceptionOfType(ArithmeticException.class).isThrownBy(() -> wrapper.div(0));
      assertThatExceptionOfType(ArithmeticException.class)
        .isThrownBy(() -> wrapper.div(ZTensor.newVector(0, 1, 2)));
    }

    assertThatExceptionOfType(IndexOutOfBoundsException.class)
      .isThrownBy(() ->
        createWrapper(ZTensor.newVector(1, 2, 3)).div(createWrapper(ZTensor.newVector(1, 2)))
      )
      .withMessage("cannot broadcast shapes: [3], [2]");
  }

  @Test
  public void test_mod() {
    {
      Wrapper wrapper = createWrapper(ZTensor.newVector(1, 2, 3));
      assertThat(wrapper.mod(createWrapper(ZTensor.newVector(1, 2, 3))))
        .isEqualTo(createWrapper(ZTensor.newVector(0, 0, 0)));
    }
    {
      Wrapper wrapper = createWrapper(ZTensor.newVector(1, 2, 3));
      assertThat(wrapper.mod(10)).isEqualTo(createWrapper(ZTensor.newVector(1, 2, 3)));
    }
    {
      Wrapper wrapper = createWrapper(ZTensor.newMatrix(new int[] { 1 }, new int[] { 2 }));
      assertThat(wrapper.mod(createWrapper(ZTensor.newVector(1, 2, 3))))
        .isEqualTo(createWrapper(ZTensor.newMatrix(new int[] { 0, 1, 1 }, new int[] { 0, 0, 2 })));
    }

    {
      Wrapper wrapper = createWrapper(ZTensor.newVector(1, 2, 3));
      assertThatExceptionOfType(ArithmeticException.class).isThrownBy(() -> wrapper.mod(0));
      assertThatExceptionOfType(ArithmeticException.class)
        .isThrownBy(() -> wrapper.mod(ZTensor.newVector(0, 1, 2)));
    }

    assertThatExceptionOfType(IndexOutOfBoundsException.class)
      .isThrownBy(() ->
        createWrapper(ZTensor.newVector(1, 2, 3)).mod(createWrapper(ZTensor.newVector(1, 2)))
      )
      .withMessage("cannot broadcast shapes: [3], [2]");
  }

  @Test
  public void test_pow() {
    {
      Wrapper wrapper = createWrapper(ZTensor.newVector(1, 2, 3));
      assertThat(wrapper.pow(createWrapper(ZTensor.newVector(1, 2, 3))))
        .isEqualTo(createWrapper(ZTensor.newVector(1, 4, 27)));
    }
    {
      Wrapper wrapper = createWrapper(ZTensor.newVector(1, 2, 3));
      assertThat(wrapper.pow(10)).isEqualTo(createWrapper(ZTensor.newVector(1, 1024, 59049)));
    }
    {
      Wrapper wrapper = createWrapper(ZTensor.newMatrix(new int[] { 1 }, new int[] { 2 }));
      assertThat(wrapper.pow(createWrapper(ZTensor.newVector(1, 2, 3))))
        .isEqualTo(createWrapper(ZTensor.newMatrix(new int[] { 1, 1, 1 }, new int[] { 2, 4, 8 })));
    }

    assertThatExceptionOfType(IndexOutOfBoundsException.class)
      .isThrownBy(() ->
        createWrapper(ZTensor.newVector(1, 2, 3)).pow(createWrapper(ZTensor.newVector(1, 2)))
      )
      .withMessage("cannot broadcast shapes: [3], [2]");
  }

  @Test
  public void test_log() {
    {
      Wrapper wrapper = createWrapper(ZTensor.newVector(10, 19, 8));
      assertThat(wrapper.log(createWrapper(ZTensor.newVector(2, 2, 3))))
        .isEqualTo(createWrapper(ZTensor.newVector(3, 4, 1)));
    }
    {
      Wrapper wrapper = createWrapper(ZTensor.newVector(10, 19, 8));
      assertThat(wrapper.log(2)).isEqualTo(createWrapper(ZTensor.newVector(3, 4, 3)));
    }
    {
      Wrapper wrapper = createWrapper(ZTensor.newMatrix(new int[] { 14 }, new int[] { 8 }));
      assertThat(wrapper.log(createWrapper(ZTensor.newVector(2, 2, 3))))
        .isEqualTo(createWrapper(ZTensor.newMatrix(new int[] { 3, 3, 2 }, new int[] { 3, 3, 1 })));
    }

    {
      Wrapper wrapper = createWrapper(ZTensor.newVector(1, 2, 3));
      assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> wrapper.log(0));
      assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> wrapper.log(ZTensor.newVector(0, 1, 2)));
    }

    assertThatExceptionOfType(IndexOutOfBoundsException.class)
      .isThrownBy(() ->
        createWrapper(ZTensor.newVector(1, 2, 3)).log(createWrapper(ZTensor.newVector(1, 2)))
      )
      .withMessage("cannot broadcast shapes: [3], [2]");
  }
}
