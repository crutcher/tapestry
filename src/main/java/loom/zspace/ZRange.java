package loom.zspace;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/** Representation of a range of indices in a ZSpace. */
@Data
public class ZRange {
  private final int[] start;
  private final int[] end;

  /**
   * Create a ZRange from a shape.
   *
   * <p>The start is all zeros and the end is the shape.
   *
   * @param shape the shape of the range.
   * @return the range.
   */
  public static ZRange fromShape(int[] shape) {
    return new ZRange(new int[shape.length], shape);
  }

  /**
   * Create a scalar ZRange.
   *
   * <p>The start and end are both empty; this results in a range with no dimensions, i.e. a scalar;
   * and a size of 1.
   *
   * @return the range.
   */
  public static ZRange scalar() {
    return fromShape(new int[] {});
  }

  @Jacksonized
  @Builder
  ZRange(int[] start, int[] end) {
    ZPoint.verifyZPointLE(start, end);

    this.start = start.clone();
    this.end = end.clone();
  }

  /** Return the number of dimensions in the range. */
  public int ndim() {
    return start.length;
  }

  /**
   * Return the shape of the range.
   *
   * @return the shape of the range.
   */
  public int[] shape() {
    int[] shape = new int[ndim()];
    for (int i = 0; i < ndim(); i++) {
      shape[i] = end[i] - start[i];
    }
    return shape;
  }

  /**
   * Return the size of the range.
   *
   * <p>The size is defined as the product of the shape.
   *
   * @return the size.
   */
  public int size() {
    int size = 1;
    for (int i = 0; i < ndim(); i++) {
      size *= end[i] - start[i];
    }
    return size;
  }

  /**
   * Return true if the range is empty.
   *
   * <p>The range is empty if the size is 0.
   *
   * @return true if the range is empty.
   */
  public boolean isEmpty() {
    return size() == 0;
  }

  public boolean contains(int[] index) {
    if (index.length != ndim()) {
      throw new IllegalArgumentException(
          String.format(
              "%s and %s differ in dimensions",
              ZPoint.formatLabeledCoord("index", index),
              ZPoint.formatLabeledCoord("range", start)));
    }

    for (int i = 0; i < ndim(); i++) {
      if (index[i] < start[i] || index[i] >= end[i]) {
        return false;
      }
    }
    return true;
  }
}
