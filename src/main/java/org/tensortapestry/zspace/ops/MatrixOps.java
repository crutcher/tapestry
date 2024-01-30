package org.tensortapestry.zspace.ops;

import javax.annotation.Nonnull;
import lombok.experimental.UtilityClass;
import org.tensortapestry.zspace.ZTensor;
import org.tensortapestry.zspace.ZTensorWrapper;

/**
 * ZTensor matrix operations.
 */
@UtilityClass
public final class MatrixOps {

  /**
   * Matrix multiplication of {@code lhs * rhs}.
   *
   * @param lhs the left-hand side tensor.
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public static ZTensor matmul(@Nonnull ZTensorWrapper lhs, @Nonnull ZTensorWrapper rhs) {
    var zlhs = lhs.unwrap();
    var zrhs = rhs.unwrap();

    zlhs.assertNDim(2);
    if (zlhs.shape(1) != zrhs.shape(0)) {
      throw new IllegalArgumentException(
        "lhs shape %s not compatible with rhs shape %s".formatted(
            zlhs.shapeAsList(),
            zrhs.shapeAsList()
          )
      );
    }

    if (zrhs.getNDim() > 2 || zrhs.getNDim() == 0) {
      throw new IllegalArgumentException(
        "rhs must be a 1D or 2D tensor, got %dD: %s".formatted(zrhs.getNDim(), zrhs.shapeAsList())
      );
    }

    boolean rhsIsVector = zrhs.getNDim() == 1;
    if (rhsIsVector) {
      zrhs = zrhs.unsqueeze(1);
    }

    var res = ZTensor.newZeros(zlhs.shape(0), zrhs.shape(1));
    var coords = new int[2];
    for (int i = 0; i < zlhs.shape(0); ++i) {
      coords[0] = i;
      for (int j = 0; j < zrhs.shape(1); ++j) {
        coords[1] = j;
        int sum = 0;
        for (int k = 0; k < zlhs.shape(1); ++k) {
          sum += zlhs.get(i, k) * zrhs.get(k, j);
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
