package org.tensortapestry.zspace.ops;

import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.experimental.UtilityClass;
import org.tensortapestry.zspace.ZTensor;
import org.tensortapestry.zspace.ZTensorWrapper;

@UtilityClass
public class ChunkOps {

  /**
   * Concatenate tensors along an axis.
   *
   * @param axis the axis.
   * @param tensors the tensors.
   * @return the concatenated tensor.
   */
  @Nonnull
  public ZTensor concat(int axis, @Nonnull ZTensorWrapper... tensors) {
    return concat(axis, Arrays.asList(tensors));
  }

  /**
   * Concatenate tensors along an axis.
   *
   * @param axis the axis.
   * @param tensors the tensors.
   * @return the concatenated tensor.
   */
  @Nonnull
  public ZTensor concat(int axis, @Nonnull List<ZTensorWrapper> tensors) {
    if (tensors.isEmpty()) {
      throw new IllegalArgumentException("tensors is empty");
    }

    var first = tensors.getFirst().unwrap();
    axis = first.resolveDim(axis);

    var shape = first.shapeAsArray();

    for (int i = 1; i < tensors.size(); i++) {
      var tensor = tensors.get(i).unwrap();
      if (tensor.getNDim() != first.getNDim()) {
        throw new IllegalArgumentException(
          "tensors have different ndim: %s != %s".formatted(first.getNDim(), tensor.getNDim())
        );
      }
      shape[axis] += tensor.shape(axis);
      for (int j = 0; j < first.getNDim(); j++) {
        if (j == axis) {
          continue;
        }
        if (tensor.shape(j) != first.shape(j)) {
          throw new IllegalArgumentException(
            "tensors have incompatible shape for axis=%d: %s vs %s".formatted(
                axis,
                first.shapeAsList(),
                tensor.shapeAsList()
              )
          );
        }
      }
    }

    var result = ZTensor.newZeros(shape);
    var last = 0;
    for (var tensor : tensors) {
      var t = tensor.unwrap();
      var k = t.shape(axis);
      result.sliceDim(axis, last, last + k).assign_(t);
      last += k;
    }

    return result;
  }
}
