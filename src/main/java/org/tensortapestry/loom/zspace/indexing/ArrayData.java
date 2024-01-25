package org.tensortapestry.loom.zspace.indexing;

import java.util.Arrays;
import java.util.Map;
import lombok.Value;

/**
 * (shape, flat data) pair.
 */
@Value
public class ArrayData implements Map.Entry<int[], int[]> {

  int[] shape;
  int[] data;

  public ArrayData(int[] shape, int[] data) {
    this.shape = shape;
    this.data = data;

    int expectedSize = IndexingFns.shapeToSize(shape);
    if (expectedSize != data.length) {
      throw new IllegalArgumentException(
        "Shape size (%d) != data length (%d): %s".formatted(
            expectedSize,
            data.length,
            Arrays.toString(shape)
          )
      );
    }
  }

  @Override
  public int[] getKey() {
    return shape;
  }

  @Override
  public int[] getValue() {
    return data;
  }

  @Override
  public int[] setValue(int[] value) {
    throw new UnsupportedOperationException();
  }
}
