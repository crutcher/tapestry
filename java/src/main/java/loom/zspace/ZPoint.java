package loom.zspace;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.errorprone.annotations.Immutable;
import loom.common.HasToJsonString;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A point in a ZSpace.
 *
 * <p>Internally, represented by an immutable ZTensor.
 *
 * <p>Serializes as the JSON representation of a ZTensor.
 */
@ThreadSafe
@Immutable
@JsonDeserialize(using = ZPoint.Deserializer.class)
public final class ZPoint implements HasPermute<ZPoint>, HasToJsonString {
  // This is asserted to be immutable in the constructor.
  @JsonValue
  @Nonnull
  @SuppressWarnings("Immutable")
  public final ZTensor coords;

  /**
   * Create a ZPoint of the given coordinates.
   *
   * @param coord the coordinates.
   */
  public ZPoint(@Nonnull ZTensor coord) {
    coord.assertNDim(1);
    this.coords = coord.asImmutable();
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
   * @param coords the coordinates.
   */
  public ZPoint(@Nonnull Iterable<Integer> coords) {
    this(ZTensor.newVector(coords));
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
   * Create a ZPoint of all zeros in the same ZSpace as a reference ZPoint.
   *
   * @param ref a reference ZPoint.
   * @return a new ZPoint.
   */
  @Nonnull
  public static ZPoint newZerosLike(@Nonnull ZPoint ref) {
    return newZeros(ref.getNDim());
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

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null || getClass() != other.getClass()) return false;
    var that = (ZPoint) other;
    return eq(that);
  }

  @Override
  public int hashCode() {
    return coords.hashCode();
  }

  @Override
  public String toString() {
    return coords.toString();
  }

  /**
   * Get the coordinate at the given dimension.
   *
   * @param i the dimension.
   * @return the coordinate.
   */
  public int get(int i) {
    return coords.get(i);
  }

  /**
   * Get the coordinates as an array.
   *
   * @return the coordinates.
   */
  @Nonnull
  public int[] toArray() {
    return coords.toT1();
  }

  @Override
  public int getNDim() {
    return coords.shape(0);
  }

  /**
   * Permute the dimensions of this ZPoint.
   *
   * @param permutation a permutation of the dimensions.
   * @return a new ZPoint.
   */
  @Override
  public ZPoint permute(@Nonnull int... permutation) {
    var cs = new int[getNDim()];
    var perm = IndexingFns.resolvePermutation(permutation, getNDim());
    for (int i = 0; i < getNDim(); ++i) {
      cs[i] = coords.get(perm[i]);
    }
    return new ZPoint(cs);
  }

  /**
   * Is `this == rhs`?
   *
   * @param rhs the right-hand side.
   * @return true or false.
   */
  public boolean eq(@Nonnull ZPoint rhs) {
    return ZPoint.Ops.eq(this, rhs);
  }

  /**
   * Is `this == rhs`?
   *
   * @param rhs the right-hand side.
   * @return true or false.
   */
  public boolean eq(@Nonnull ZTensor rhs) {
    return ZPoint.Ops.eq(this, rhs);
  }

  /**
   * Is `this != rhs`?
   *
   * @param rhs the right-hand side.
   * @return true or false.
   */
  public boolean ne(@Nonnull ZPoint rhs) {
    return ZPoint.Ops.ne(this, rhs);
  }

  /**
   * Is `this != rhs`?
   *
   * @param rhs the right-hand side.
   * @return true or false.
   */
  public boolean ne(@Nonnull ZTensor rhs) {
    return ZPoint.Ops.ne(this, rhs);
  }

  /**
   * Is `this < rhs`?
   *
   * @param rhs the right-hand side.
   * @return true or false.
   */
  public boolean lt(@Nonnull ZPoint rhs) {
    return ZPoint.Ops.lt(this, rhs);
  }

  /**
   * Is `this < rhs`?
   *
   * @param rhs the right-hand side.
   * @return true or false.
   */
  public boolean lt(@Nonnull ZTensor rhs) {
    return ZPoint.Ops.lt(this, rhs);
  }

  /**
   * Is `this <= rhs`?
   *
   * @param rhs the right-hand side.
   * @return true or false.
   */
  public boolean le(@Nonnull ZPoint rhs) {
    return ZPoint.Ops.le(this, rhs);
  }

  /**
   * Is `this <= rhs`?
   *
   * @param rhs the right-hand side.
   * @return true or false.
   */
  public boolean le(@Nonnull ZTensor rhs) {
    return ZPoint.Ops.le(this, rhs);
  }

  /**
   * Is `this > rhs`?
   *
   * @param rhs the right-hand side.
   * @return true or false.
   */
  public boolean gt(@Nonnull ZPoint rhs) {
    return ZPoint.Ops.gt(this, rhs);
  }

  /**
   * Is `this > rhs`?
   *
   * @param rhs the right-hand side.
   * @return true or false.
   */
  public boolean gt(@Nonnull ZTensor rhs) {
    return ZPoint.Ops.gt(this, rhs);
  }

  /**
   * Is `this >= rhs`?
   *
   * @param rhs the right-hand side.
   * @return true or false.
   */
  public boolean ge(@Nonnull ZPoint rhs) {
    return ZPoint.Ops.ge(this, rhs);
  }

  /**
   * Is `this >= rhs`?
   *
   * @param rhs the right-hand side.
   * @return true or false.
   */
  public boolean ge(@Nonnull ZTensor rhs) {
    return ZPoint.Ops.ge(this, rhs);
  }

  static final class Deserializer extends StdDeserializer<ZPoint> {
    public Deserializer() {
      super(ZPoint.class);
    }

    @Override
    public ZPoint deserialize(JsonParser p, DeserializationContext context)
        throws java.io.IOException {
      return new ZPoint(p.readValueAs(ZTensor.class));
    }
  }

  /** Namespace of ZPoint operations. */
  public static final class Ops {
    // Prevent instantiation.
    private Ops() {}

    /**
     * Compute the partial ordering of two tensors as coordinates in distance from 0.
     *
     * <p>This ordering is defined to be useful for {@code [start, end)} ranges.
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return the partial ordering.
     */
    public static PartialOrdering partialCompare(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
      HasDimension.assertSameNDim(lhs, rhs);

      boolean lt = false;
      boolean gt = false;
      for (int[] coords : lhs.byCoords(CoordsBufferMode.REUSED)) {
        int cmp = Integer.compare(lhs.get(coords), rhs.get(coords));
        if (cmp < 0) {
          lt = true;
        } else if (cmp > 0) {
          gt = true;
        }
      }
      if (lt && gt) return PartialOrdering.INCOMPARABLE;
      if (lt) return PartialOrdering.LESS_THAN;
      if (gt) return PartialOrdering.GREATER_THAN;
      return PartialOrdering.EQUAL;
    }

    /**
     * Compute the partial ordering of two points.
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return the partial ordering.
     */
    public static PartialOrdering partialCompare(@Nonnull ZPoint lhs, @Nonnull ZPoint rhs) {
      return partialCompare(lhs.coords, rhs.coords);
    }

    /**
     * Are these points equal under partial ordering?
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return true if the points are equal.
     */
    public static boolean eq(@Nonnull ZPoint lhs, @Nonnull ZPoint rhs) {
      return eq(lhs.coords, rhs.coords);
    }

    /**
     * Are these points equal under partial ordering?
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return true if the points are equal.
     */
    public static boolean eq(@Nonnull ZTensor lhs, @Nonnull ZPoint rhs) {
      return eq(lhs, rhs.coords);
    }

    /**
     * Are these points equal under partial ordering?
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return true if the points are equal.
     */
    public static boolean eq(@Nonnull ZPoint lhs, @Nonnull ZTensor rhs) {
      return eq(lhs.coords, rhs);
    }

    /**
     * Are these points equal under partial ordering?
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return true if the points are equal.
     */
    public static boolean eq(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
      return lhs.equals(rhs);
    }

    /**
     * Are these points non-equal under partial ordering?
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return true if the points are non-equal.
     */
    public static boolean ne(@Nonnull ZPoint lhs, @Nonnull ZPoint rhs) {
      return !eq(lhs.coords, rhs.coords);
    }

    /**
     * Are these points non-equal under partial ordering?
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return true if the points are non-equal.
     */
    public static boolean ne(@Nonnull ZTensor lhs, @Nonnull ZPoint rhs) {
      return !eq(lhs, rhs.coords);
    }

    /**
     * Are these points non-equal under partial ordering?
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return true if the points are non-equal.
     */
    public static boolean ne(@Nonnull ZPoint lhs, @Nonnull ZTensor rhs) {
      return !eq(lhs.coords, rhs);
    }

    /**
     * Are these points non-equal under partial ordering?
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return true if the points are non-equal.
     */
    public static boolean ne(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
      return !eq(lhs, rhs);
    }

    /**
     * Is `lhs < rhs`?
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return true or false.
     */
    public static boolean lt(@Nonnull ZPoint lhs, @Nonnull ZPoint rhs) {
      return lt(lhs.coords, rhs.coords);
    }

    /**
     * Is `lhs < rhs`?
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return true or false.
     */
    public static boolean lt(@Nonnull ZTensor lhs, @Nonnull ZPoint rhs) {
      return lt(lhs, rhs.coords);
    }

    /**
     * Is `lhs < rhs`?
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return true or false.
     */
    public static boolean lt(@Nonnull ZPoint lhs, @Nonnull ZTensor rhs) {
      return lt(lhs.coords, rhs);
    }

    /**
     * Is `lhs < rhs`?
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return true or false.
     */
    public static boolean lt(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
      return partialCompare(lhs, rhs) == PartialOrdering.LESS_THAN;
    }

    /**
     * Is `lhs <= rhs`?
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return true or false.
     */
    public static boolean le(@Nonnull ZPoint lhs, @Nonnull ZPoint rhs) {
      return le(lhs.coords, rhs.coords);
    }

    /**
     * Is `lhs <= rhs`?
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return true or false.
     */
    public static boolean le(@Nonnull ZPoint lhs, @Nonnull ZTensor rhs) {
      return le(lhs.coords, rhs);
    }

    /**
     * Is `lhs <= rhs`?
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return true or false.
     */
    public static boolean le(@Nonnull ZTensor lhs, @Nonnull ZPoint rhs) {
      return le(lhs, rhs.coords);
    }

    /**
     * Is `lhs <= rhs`?
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return true or false.
     */
    public static boolean le(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
      return switch (partialCompare(lhs, rhs)) {
        case LESS_THAN, EQUAL -> true;
        default -> false;
      };
    }

    /**
     * Is `lhs > rhs`?
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return true or false.
     */
    public static boolean gt(@Nonnull ZPoint lhs, @Nonnull ZPoint rhs) {
      return gt(lhs.coords, rhs.coords);
    }

    /**
     * Is `lhs > rhs`?
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return true or false.
     */
    public static boolean gt(@Nonnull ZPoint lhs, @Nonnull ZTensor rhs) {
      return gt(lhs.coords, rhs);
    }

    /**
     * Is `lhs > rhs`?
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return true or false.
     */
    public static boolean gt(@Nonnull ZTensor lhs, @Nonnull ZPoint rhs) {
      return gt(lhs, rhs.coords);
    }

    /**
     * Is `lhs > rhs`?
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return true or false.
     */
    public static boolean gt(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
      return partialCompare(lhs, rhs) == PartialOrdering.GREATER_THAN;
    }

    /**
     * Is `lhs >= rhs`?
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return true or false.
     */
    public static boolean ge(@Nonnull ZPoint lhs, @Nonnull ZPoint rhs) {
      return ge(lhs.coords, rhs.coords);
    }

    /**
     * Is `lhs >= rhs`?
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return true or false.
     */
    public static boolean ge(@Nonnull ZPoint lhs, @Nonnull ZTensor rhs) {
      return ge(lhs.coords, rhs);
    }

    /**
     * Is `lhs >= rhs`?
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return true or false.
     */
    public static boolean ge(@Nonnull ZTensor lhs, @Nonnull ZPoint rhs) {
      return ge(lhs, rhs.coords);
    }

    /**
     * Is `lhs >= rhs`?
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return true or false.
     */
    public static boolean ge(@Nonnull ZTensor lhs, @Nonnull ZTensor rhs) {
      return switch (partialCompare(lhs, rhs)) {
        case GREATER_THAN, EQUAL -> true;
        default -> false;
      };
    }
  }
}
