package loom.zspace;

import loom.testing.BaseTestClass;
import org.junit.Test;

public class ImmutableZTensorWrapperTest extends BaseTestClass {
  public static class Wrapper extends ImmutableZTensorWrapper<Wrapper> {
    public Wrapper(HasZTensor tensor) {
      super(tensor);
    }

    @Override
    protected Wrapper create(HasZTensor tensor) {
      return new Wrapper(tensor);
    }
  }

  @Test
  public void test_new() {
    ZTensor tensor = ZTensor.newVector(1, 2, 3);
    assertThat(new Wrapper(tensor).asZTensor()).isEqualTo(tensor).isNotSameAs(tensor);

    ZTensor immutable = tensor.asImmutable();
    assertThat(new Wrapper(immutable).asZTensor()).isEqualTo(immutable).isSameAs(immutable);
  }

  @Test
  public void test_hashCode() {
    ZTensor tensor = ZTensor.newVector(1, 2, 3);
    Wrapper wrapper = new Wrapper(tensor);
    assertThat(wrapper.hashCode()).isEqualTo(tensor.asImmutable().hashCode());
  }

  @Test
  public void test_toString() {
    ZTensor tensor = ZTensor.newVector(1, 2, 3);
    Wrapper wrapper = new Wrapper(tensor);
    assertThat(wrapper).hasToString(tensor.toString());
  }

  @Test
  public void test_clone() {
    Wrapper wrapper = new Wrapper(ZTensor.newVector(1, 2, 3));
    Wrapper clone = wrapper.clone();
    assertThat(clone).isSameAs(wrapper);
  }

  @Test
  public void test_eq() {
    Wrapper wrapper = new Wrapper(ZTensor.newVector(1, 2, 3));
    assertThat(wrapper)
        .isEqualTo(wrapper)
        .isEqualTo(new Wrapper(ZTensor.newVector(1, 2, 3)))
        .isEqualTo(ZTensor.newVector(1, 2, 3))
        .isNotEqualTo(new Wrapper(ZTensor.newVector(1, 2, 4)))
        .isNotEqualTo(new Wrapper(ZTensor.newVector(1, 2, 3, 4)))
        .isNotEqualTo(new Wrapper(ZTensor.newVector(1, 2)))
        .isNotEqualTo(null)
        .isNotEqualTo(new Object());

    assertThatExceptionOfType(ZDimMissMatchError.class)
        .isThrownBy(() -> wrapper.eq(new Wrapper(ZTensor.newVector(1, 2))))
        .withMessage("ZDim shape mismatch: [3] != [2]");
  }

  @Test
  public void test_lt() {
    Wrapper wrapper = new Wrapper(ZTensor.newVector(1, 2, 3));
    assertThat(wrapper.lt(wrapper)).isFalse();
    assertThat(wrapper.lt(new Wrapper(ZTensor.newVector(1, 2, 3)))).isFalse();
    assertThat(wrapper.lt(ZTensor.newVector(1, 2, 3))).isFalse();
    assertThat(wrapper.lt(new Wrapper(ZTensor.newVector(1, 2, 4)))).isTrue();

    // Non-dominated points are not less than each other.
    assertThat(wrapper.lt(new Wrapper(ZTensor.newVector(5, 2, -1)))).isFalse();

    assertThatExceptionOfType(ZDimMissMatchError.class)
        .isThrownBy(() -> wrapper.lt(new Wrapper(ZTensor.newVector(1, 2))))
        .withMessage("ZDim shape mismatch: [3] != [2]");
  }

  @Test
  public void test_le() {
    Wrapper wrapper = new Wrapper(ZTensor.newVector(1, 2, 3));
    assertThat(wrapper.le(wrapper)).isTrue();
    assertThat(wrapper.le(new Wrapper(ZTensor.newVector(1, 2, 3)))).isTrue();
    assertThat(wrapper.le(ZTensor.newVector(1, 2, 3))).isTrue();
    assertThat(wrapper.le(new Wrapper(ZTensor.newVector(1, 2, 4)))).isTrue();
    assertThat(wrapper.le(new Wrapper(ZTensor.newVector(1, 2, 2)))).isFalse();

    // Non-dominated points are not less than or equal to each other.
    assertThat(wrapper.le(new Wrapper(ZTensor.newVector(5, 2, -1)))).isFalse();

    assertThatExceptionOfType(ZDimMissMatchError.class)
        .isThrownBy(() -> wrapper.le(new Wrapper(ZTensor.newVector(1, 2))))
        .withMessage("ZDim shape mismatch: [3] != [2]");
  }

  @Test
  public void test_gt() {
    Wrapper wrapper = new Wrapper(ZTensor.newVector(1, 2, 3));
    assertThat(wrapper.gt(wrapper)).isFalse();
    assertThat(wrapper.gt(new Wrapper(ZTensor.newVector(1, 2, 3)))).isFalse();
    assertThat(wrapper.gt(ZTensor.newVector(1, 2, 3))).isFalse();
    assertThat(wrapper.gt(new Wrapper(ZTensor.newVector(1, 2, 4)))).isFalse();
    assertThat(wrapper.gt(new Wrapper(ZTensor.newVector(1, 2, 2)))).isTrue();

    // Non-dominated points are not greater than each other.
    assertThat(wrapper.gt(new Wrapper(ZTensor.newVector(5, 2, -1)))).isFalse();

    assertThatExceptionOfType(ZDimMissMatchError.class)
        .isThrownBy(() -> wrapper.gt(new Wrapper(ZTensor.newVector(1, 2))))
        .withMessage("ZDim shape mismatch: [3] != [2]");
  }

  @Test
  public void test_ge() {
    Wrapper wrapper = new Wrapper(ZTensor.newVector(1, 2, 3));
    assertThat(wrapper.ge(wrapper)).isTrue();
    assertThat(wrapper.ge(new Wrapper(ZTensor.newVector(1, 2, 3)))).isTrue();
    assertThat(wrapper.ge(ZTensor.newVector(1, 2, 3))).isTrue();
    assertThat(wrapper.ge(new Wrapper(ZTensor.newVector(1, 2, 4)))).isFalse();
    assertThat(wrapper.ge(new Wrapper(ZTensor.newVector(1, 2, 2)))).isTrue();

    // Non-dominated points are not greater than or equal to each other.
    assertThat(wrapper.ge(new Wrapper(ZTensor.newVector(5, 2, -1)))).isFalse();

    assertThatExceptionOfType(ZDimMissMatchError.class)
        .isThrownBy(() -> wrapper.ge(new Wrapper(ZTensor.newVector(1, 2))))
        .withMessage("ZDim shape mismatch: [3] != [2]");
  }

  @Test
  public void test_sumAsInt() {
    Wrapper wrapper = new Wrapper(ZTensor.newVector(1, 2, 3));
    assertThat(wrapper.sumAsInt()).isEqualTo(6);
  }

  @Test
  public void test_prodAsInt() {
    Wrapper wrapper = new Wrapper(ZTensor.newVector(1, 2, 3));
    assertThat(wrapper.prodAsInt()).isEqualTo(6);
  }

  @Test
  public void test_maxAsInt() {
    Wrapper wrapper = new Wrapper(ZTensor.newVector(1, 2, 3));
    assertThat(wrapper.maxAsInt()).isEqualTo(3);
  }

  @Test
  public void test_minAsInt() {
    Wrapper wrapper = new Wrapper(ZTensor.newVector(1, 2, 3));
    assertThat(wrapper.minAsInt()).isEqualTo(1);
  }

  @Test
  public void test_neg() {
    Wrapper wrapper = new Wrapper(ZTensor.newVector(1, 2, 3));
    assertThat(wrapper.neg()).isEqualTo(new Wrapper(ZTensor.newVector(-1, -2, -3)));
  }

  @Test
  public void test_abs() {
    Wrapper wrapper = new Wrapper(ZTensor.newVector(1, -2, 3));
    assertThat(wrapper.abs()).isEqualTo(new Wrapper(ZTensor.newVector(1, 2, 3)));
  }

  @Test
  public void test_add() {
    {
      Wrapper wrapper = new Wrapper(ZTensor.newVector(1, 2, 3));
      assertThat(wrapper.add(new Wrapper(ZTensor.newVector(1, 2, 3))))
          .isEqualTo(new Wrapper(ZTensor.newVector(2, 4, 6)));
    }
    {
      Wrapper wrapper = new Wrapper(ZTensor.newVector(1, 2, 3));
      assertThat(wrapper.add(10)).isEqualTo(new Wrapper(ZTensor.newVector(11, 12, 13)));
    }
    {
      Wrapper wrapper = new Wrapper(ZTensor.newMatrix(new int[] {1}, new int[] {2}));
      assertThat(wrapper.add(new Wrapper(ZTensor.newVector(1, 2, 3))))
          .isEqualTo(new Wrapper(ZTensor.newMatrix(new int[] {2, 3, 4}, new int[] {3, 4, 5})));
    }

    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(
            () -> new Wrapper(ZTensor.newVector(1, 2, 3)).add(new Wrapper(ZTensor.newVector(1, 2))))
        .withMessage("cannot broadcast shapes: [3], [2]");
  }

  @Test
  public void test_sub() {
    {
      Wrapper wrapper = new Wrapper(ZTensor.newVector(1, 2, 3));
      assertThat(wrapper.sub(new Wrapper(ZTensor.newVector(1, 2, 3))))
          .isEqualTo(new Wrapper(ZTensor.newVector(0, 0, 0)));
    }
    {
      Wrapper wrapper = new Wrapper(ZTensor.newVector(1, 2, 3));
      assertThat(wrapper.sub(10)).isEqualTo(new Wrapper(ZTensor.newVector(-9, -8, -7)));
    }
    {
      Wrapper wrapper = new Wrapper(ZTensor.newMatrix(new int[] {1}, new int[] {2}));
      assertThat(wrapper.sub(new Wrapper(ZTensor.newVector(1, 2, 3))))
          .isEqualTo(new Wrapper(ZTensor.newMatrix(new int[] {0, -1, -2}, new int[] {1, 0, -1})));
    }

    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(
            () -> new Wrapper(ZTensor.newVector(1, 2, 3)).sub(new Wrapper(ZTensor.newVector(1, 2))))
        .withMessage("cannot broadcast shapes: [3], [2]");
  }

  @Test
  public void test_mul() {
    {
      Wrapper wrapper = new Wrapper(ZTensor.newVector(1, 2, 3));
      assertThat(wrapper.mul(new Wrapper(ZTensor.newVector(1, 2, 3))))
          .isEqualTo(new Wrapper(ZTensor.newVector(1, 4, 9)));
    }
    {
      Wrapper wrapper = new Wrapper(ZTensor.newVector(1, 2, 3));
      assertThat(wrapper.mul(10)).isEqualTo(new Wrapper(ZTensor.newVector(10, 20, 30)));
    }
    {
      Wrapper wrapper = new Wrapper(ZTensor.newMatrix(new int[] {1}, new int[] {2}));
      assertThat(wrapper.mul(new Wrapper(ZTensor.newVector(1, 2, 3))))
          .isEqualTo(new Wrapper(ZTensor.newMatrix(new int[] {1, 2, 3}, new int[] {2, 4, 6})));
    }

    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(
            () -> new Wrapper(ZTensor.newVector(1, 2, 3)).mul(new Wrapper(ZTensor.newVector(1, 2))))
        .withMessage("cannot broadcast shapes: [3], [2]");
  }

  @Test
  public void test_div() {
    {
      Wrapper wrapper = new Wrapper(ZTensor.newVector(1, 2, 3));
      assertThat(wrapper.div(new Wrapper(ZTensor.newVector(1, 2, 3))))
          .isEqualTo(new Wrapper(ZTensor.newVector(1, 1, 1)));
    }
    {
      Wrapper wrapper = new Wrapper(ZTensor.newVector(1, 2, 3));
      assertThat(wrapper.div(10)).isEqualTo(new Wrapper(ZTensor.newVector(0, 0, 0)));
    }
    {
      Wrapper wrapper = new Wrapper(ZTensor.newMatrix(new int[] {1}, new int[] {2}));
      assertThat(wrapper.div(new Wrapper(ZTensor.newVector(1, 2, 3))))
          .isEqualTo(new Wrapper(ZTensor.newMatrix(new int[] {1, 0, 0}, new int[] {2, 1, 0})));
    }

    {
      Wrapper wrapper = new Wrapper(ZTensor.newVector(1, 2, 3));
      assertThatExceptionOfType(ArithmeticException.class).isThrownBy(() -> wrapper.div(0));
      assertThatExceptionOfType(ArithmeticException.class)
          .isThrownBy(() -> wrapper.div(ZTensor.newVector(0, 1, 2)));
    }

    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(
            () -> new Wrapper(ZTensor.newVector(1, 2, 3)).div(new Wrapper(ZTensor.newVector(1, 2))))
        .withMessage("cannot broadcast shapes: [3], [2]");
  }

  @Test
  public void test_mod() {
    {
      Wrapper wrapper = new Wrapper(ZTensor.newVector(1, 2, 3));
      assertThat(wrapper.mod(new Wrapper(ZTensor.newVector(1, 2, 3))))
          .isEqualTo(new Wrapper(ZTensor.newVector(0, 0, 0)));
    }
    {
      Wrapper wrapper = new Wrapper(ZTensor.newVector(1, 2, 3));
      assertThat(wrapper.mod(10)).isEqualTo(new Wrapper(ZTensor.newVector(1, 2, 3)));
    }
    {
      Wrapper wrapper = new Wrapper(ZTensor.newMatrix(new int[] {1}, new int[] {2}));
      assertThat(wrapper.mod(new Wrapper(ZTensor.newVector(1, 2, 3))))
          .isEqualTo(new Wrapper(ZTensor.newMatrix(new int[] {0, 1, 1}, new int[] {0, 0, 2})));
    }

    {
      Wrapper wrapper = new Wrapper(ZTensor.newVector(1, 2, 3));
      assertThatExceptionOfType(ArithmeticException.class).isThrownBy(() -> wrapper.mod(0));
      assertThatExceptionOfType(ArithmeticException.class)
          .isThrownBy(() -> wrapper.mod(ZTensor.newVector(0, 1, 2)));
    }

    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(
            () -> new Wrapper(ZTensor.newVector(1, 2, 3)).mod(new Wrapper(ZTensor.newVector(1, 2))))
        .withMessage("cannot broadcast shapes: [3], [2]");
  }
}
