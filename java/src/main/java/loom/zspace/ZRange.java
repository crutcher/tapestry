package loom.zspace;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/** Representation of a range of indices in a ZSpace. */
@Data
public class ZRange {
  // Immutable, and verified that start <= end at construction.
  private final ZPoint start;
  private final ZPoint end;

  /**
   * Create a ZRange from a shape.
   *
   * <p>The start is all zeros and the end is the shape.
   *
   * @param shape the shape of the range.
   * @return the range.
   */
  public static ZRange of(int... shape) {
    return of(ZPoint.of(shape));
  }

  public static ZRange of(ZPoint shape) {
    return new ZRange(new ZPoint(new int[shape.ndim()]), shape);
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
    return of(new int[] {});
  }

  @Jacksonized
  @Builder
  ZRange(ZPoint start, ZPoint end) {
    ZPoint.verifyZPointLE(start, end);
    this.start = start;
    this.end = end;
  }

  /** Return the number of dimensions in the range. */
  public int ndim() {
    return start.ndim();
  }

  /**
   * Return the shape of the range.
   *
   * @return the shape of the range.
   */
  public int[] shape() {
    int[] shape = new int[ndim()];
    for (int i = 0; i < ndim(); i++) {
      shape[i] = end.coords[i] - start.coords[i];
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
      size *= end.coords[i] - start.coords[i];
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
              ZPoint.formatLabeledCoord("range", start.coords)));
    }

    for (int i = 0; i < ndim(); i++) {
      if (index[i] < start.coords[i] || index[i] >= end.coords[i]) {
        return false;
      }
    }
    return true;
  }
}
