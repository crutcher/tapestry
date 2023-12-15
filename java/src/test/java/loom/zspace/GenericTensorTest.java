package loom.zspace;

import loom.testing.BaseTestClass;
import org.junit.Test;

import javax.annotation.Nonnull;

public class GenericTensorTest extends BaseTestClass {
  public static class DoubleOps {

    private DoubleOps() {}

    /**
     * Matrix multiplication of {@code lhs * rhs}.
     *
     * @param lhs the left-hand side tensor.
     * @param rhs the right-hand side tensor.
     * @return a new tensor.
     */
    @Nonnull
    public static GenericTensor<Double> matmul(
        @Nonnull GenericTensor<Double> lhs, @Nonnull GenericTensor<Double> rhs) {
      lhs.assertNDim(2);
      if (lhs.shape(1) != rhs.shape(0)) {
        throw new IllegalArgumentException(
            "lhs shape %s not compatible with rhs shape %s"
                .formatted(lhs.shapeAsList(), rhs.shapeAsList()));
      }

      if (rhs.getNDim() > 2 || rhs.getNDim() == 0) {
        throw new IllegalArgumentException(
            "rhs must be a 1D or 2D tensor, got %dD".formatted(rhs.getNDim()));
      }

      boolean rhsIsVector = rhs.getNDim() == 1;
      if (rhsIsVector) {
        rhs = rhs.unsqueeze(1);
      }

      var res = GenericTensor.newZeros(Double[].class, lhs.shape(0), rhs.shape(1));
      var coords = new int[2];
      for (int i = 0; i < lhs.shape(0); ++i) {
        coords[0] = i;
        for (int j = 0; j < rhs.shape(1); ++j) {
          coords[1] = j;
          double sum = 0;
          for (int k = 0; k < lhs.shape(1); ++k) {
            sum += lhs.get(i, k) * rhs.get(k, j);
          }
          res.set(coords, sum);
        }
      }

      if (rhsIsVector) {
        res = res.squeeze(1);
      }

      return res;
    }
  }

  @Test
  public void test_basic() {
    GenericTensor<Double> t = GenericTensor.newScalar(3.0);

    assertThat(t.get()).isEqualTo(3.0);

    assertThat(t.map(v -> v + 1.0).get()).isEqualTo(4.0);
  }
}
