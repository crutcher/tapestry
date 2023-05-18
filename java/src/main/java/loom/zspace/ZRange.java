package loom.zspace;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import loom.linear.LongOps;

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
  public static ZRange of(long... shape) {
    return of(ZPoint.of(shape));
  }

  public static ZRange of(ZPoint shape) {
    return new ZRange(new ZPoint(new long[shape.ndim()]), shape);
  }

  public static ZRange between(ZPoint start, ZPoint end) {
    return new ZRange(start, end);
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
    return of(ZPoint.scalar());
  }

  @Jacksonized
  @Builder
  ZRange(ZPoint start, ZPoint end) {
    ZPoint.verifyZPointLE(start, end);
    this.start = start;
    this.end = end;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("zr[");
    for (int idx = 0; idx < ndim(); ++idx) {
      if (idx > 0) {
        sb.append(", ");
      }
      sb.append(start.coords[idx]);
      sb.append(":");
      sb.append(end.coords[idx]);
    }
    sb.append("]");
    return sb.toString();
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
  public long[] shape() {
    long[] shape = new long[ndim()];
    for (int i = 0, d = ndim(); i < d; i++) {
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
  public long size() {
    long size = 1;
    for (int i = 0, d = ndim(); i < d; i++) {
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

  public boolean isScalar() {
    return ndim() == 0;
  }

  public boolean contains(long[] index) {
    return LongOps.le(start.coords, index) && LongOps.lt(index, end.coords);
  }
}
