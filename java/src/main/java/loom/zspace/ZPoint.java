package loom.zspace;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.errorprone.annotations.Immutable;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaInject;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * An immutable point in a ZSpace.
 *
 * <p>Internally, represented by an immutable ZTensor.
 *
 * <p>Serializes as the JSON representation of a ZTensor.
 */
@ThreadSafe
@Immutable
@JsonSchemaInject(
    json =
        """
    {
        "type": "array",
        "items": {
            "type": "integer"
        }
    }
    """,
    merge = false)
public final class ZPoint extends ImmutableZTensorWrapper<ZPoint> implements HasPermute<ZPoint> {
  /**
   * Private constructor for Jackson.
   *
   * <p>This permits the public constructor to use {@link HasZTensor}.
   *
   * @param tensor the tensor.
   * @return a new ZPoint.
   */
  @JsonCreator
  private static ZPoint privateCreator(ZTensor tensor) {
    return new ZPoint(tensor);
  }

  /**
   * Create a ZPoint of all zeros in the same ZSpace as a reference ZPoint.
   *
   * @param ref a reference ZPoint.
   * @return a new ZPoint.
   */
  @Nonnull
  public static ZPoint newZerosLike(@Nonnull HasZTensor ref) {
    return new ZPoint(ZTensor.newZerosLike(ref));
  }

  /**
   * Create a ZPoint of all zeros in the given ZSpace.
   *
   * @param ndim the number of dimensions.
   * @return a new ZPoint.
   */
  @Nonnull
  public static ZPoint newZeros(int ndim) {
    return new ZPoint(ZTensor.newZeros(ndim));
  }

  /**
   * Create a ZPoint of all ones in the same ZSpace as a reference ZPoint.
   *
   * @param ref a reference ZPoint.
   * @return a new ZPoint.
   */
  @Nonnull
  public static ZPoint newOnesLike(@Nonnull HasZTensor ref) {
    return new ZPoint(ZTensor.newOnesLike(ref));
  }

  /**
   * Create a ZPoint of all ones in the given ZSpace.
   *
   * @param ndim the number of dimensions.
   * @return a new ZPoint.
   */
  @Nonnull
  public static ZPoint newOnes(int ndim) {
    return new ZPoint(ZTensor.newOnes(ndim));
  }

  @Override
  public int getNDim() {
    return tensor.shape(0);
  }

  /**
   * Create a ZPoint of the given coordinates.
   *
   * <p>Equivalent to {@code new ZPoint(coords)}.
   *
   * @param coords the coordinates.
   * @return a new ZPoint.
   */
  @Nonnull
  public static ZPoint of(@Nonnull int... coords) {
    return new ZPoint(coords);
  }

  /**
   * Parse a ZPoint from a string.
   *
   * @param source the string to parse.
   * @return the parsed ZPoint.
   * @throws IllegalArgumentException if the string is not a valid ZPoint.
   */
  @Nonnull
  public static ZPoint parse(@Nonnull String source) {
    return new ZPoint(ZTensor.parse(source));
  }

  /**
   * Create a ZPoint of the given coordinates.
   *
   * @param coords the coordinates.
   */
  public ZPoint(@Nonnull int... coords) {
    this(ZTensor.newVector(coords));
  }

  /**
   * Create a ZPoint of the given coordinates.
   *
   * @param coord the coordinates.
   */
  public ZPoint(@Nonnull HasZTensor coord) {
    super(coord);
    this.tensor.assertNDim(1);
  }

  /**
   * Create a ZPoint of the given coordinates.
   *
   * @param coords the coordinates.
   */
  public ZPoint(@Nonnull Iterable<Integer> coords) {
    this(ZTensor.newVector(coords));
  }

  @Override
  @Nonnull
  protected ZPoint create(@Nonnull HasZTensor tensor) {
    return new ZPoint(tensor);
  }

  /**
   * Get the coordinate at the given dimension.
   *
   * @param i the dimension.
   * @return the coordinate.
   */
  public int get(int i) {
    return tensor.get(i);
  }

  /**
   * Get this as an array.
   *
   * @return the array.
   */
  @Nonnull
  public int[] toArray() {
    return tensor.toT1();
  }

  /**
   * Resolve a dimension index.
   *
   * <p>Negative dimension indices are resolved relative to the number of dimensions.
   *
   * @param dim the dimension index.
   * @return the resolved dimension index.
   * @throws IndexOutOfBoundsException if the index is out of range.
   */
  public int resolveDim(int dim) {
    return IndexingFns.resolveDim(dim, getNDim());
  }

  /**
   * Permute the dimensions of this ZPoint.
   *
   * @param permutation a permutation of the dimensions.
   * @return a new ZPoint.
   */
  @Override
  public ZPoint permute(@Nonnull int... permutation) {
    return new ZPoint(IndexingFns.permute(toArray(), permutation));
  }
}
