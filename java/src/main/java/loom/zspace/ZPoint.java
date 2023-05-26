package loom.zspace;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.errorprone.annotations.Immutable;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import loom.common.HasToJsonString;

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
public final class ZPoint implements HasDimension, HasPermute, HasToJsonString {
  static class Deserializer extends StdDeserializer<ZPoint> {
    public Deserializer() {
      super(ZPoint.class);
    }

    @Override
    public ZPoint deserialize(
        com.fasterxml.jackson.core.JsonParser p,
        com.fasterxml.jackson.databind.DeserializationContext ctxt)
        throws java.io.IOException {
      return new ZPoint(p.readValueAs(ZTensor.class));
    }
  }

  public static @Nonnull ZPoint zeros(int ndim) {
    return new ZPoint(ZTensor.zeros(ndim));
  }

  @JsonValue
  @Nonnull
  @SuppressWarnings("Immutable")
  public final ZTensor coords;

  public ZPoint(ZTensor coord) {
    coord.assertNdim(1);
    this.coords = coord.immutable();
  }

  public ZPoint(int... coords) {
    this(ZTensor.vector(coords));
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
   * Parse a ZPoint from a string.
   *
   * @param source the string to parse.
   * @return the parsed ZPoint.
   */
  public static ZPoint parse(String source) {
    return new ZPoint(ZTensor.parse(source));
  }

  @Override
  public int ndim() {
    return coords.shape(0);
  }

  /**
   * Permute the dimensions of this ZPoint.
   *
   * @param permutation a permutation of the dimensions.
   * @return a new ZPoint.
   */
  @Override
  public ZPoint permute(int... permutation) {
    var cs = new int[ndim()];
    var perm = Indexing.resolvePermutation(permutation, ndim());
    for (int i = 0; i < ndim(); ++i) {
      cs[i] = coords.get(perm[i]);
    }
    return new ZPoint(cs);
  }

  /** Namespace of ZPoint operations. */
  public static final class Ops {
    // Prevent instantiation.
    private Ops() {}

    /**
     * Compute the partial ordering of two tensors as coordinates in distance from 0.
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return the partial ordering.
     */
    public static PartialOrdering partialCompare(ZTensor lhs, ZTensor rhs) {
      HasDimension.assertSameNDim(lhs, rhs);

      boolean lt = false;
      boolean gt = false;
      for (int[] coords : lhs.byCoords()) {
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
    public static PartialOrdering partialCompare(ZPoint lhs, ZPoint rhs) {
      return partialCompare(lhs.coords, rhs.coords);
    }

    /**
     * Are these points equal under partial ordering?
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return true if the points are equal.
     */
    public static boolean eq(ZPoint lhs, ZPoint rhs) {
      return eq(lhs.coords, rhs.coords);
    }

    public static boolean eq(HasSize lhs, ZPoint rhs) {
      return eq(lhs, rhs.coords);
    }

    public static boolean eq(ZPoint lhs, HasSize rhs) {
      return eq(lhs.coords, rhs);
    }

    public static boolean eq(HasSize lhs, HasSize rhs) {
      return lhs.equals(rhs);
    }

    /**
     * Are these points non-equal under partial ordering?
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return true if the points are non-equal.
     */
    public static boolean ne(ZPoint lhs, ZPoint rhs) {
      return !eq(lhs.coords, rhs.coords);
    }

    public static boolean ne(HasSize lhs, ZPoint rhs) {
      return !eq(lhs, rhs.coords);
    }

    public static boolean ne(ZPoint lhs, HasSize rhs) {
      return !eq(lhs.coords, rhs);
    }

    public static boolean ne(HasSize lhs, HasSize rhs) {
      return !eq(lhs, rhs);
    }

    /**
     * Is `lhs < rhs`?
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return true or false.
     */
    public static boolean lt(ZPoint lhs, ZPoint rhs) {
      return lt(lhs.coords, rhs.coords);
    }

    public static boolean lt(ZTensor lhs, ZPoint rhs) {
      return lt(lhs, rhs.coords);
    }

    public static boolean lt(ZPoint lhs, ZTensor rhs) {
      return lt(lhs.coords, rhs);
    }

    public static boolean lt(ZTensor lhs, ZTensor rhs) {
      return partialCompare(lhs, rhs) == PartialOrdering.LESS_THAN;
    }

    /**
     * Is `lhs <= rhs`?
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return true or false.
     */
    public static boolean le(ZPoint lhs, ZPoint rhs) {
      return le(lhs.coords, rhs.coords);
    }

    public static boolean le(ZPoint lhs, ZTensor rhs) {
      return le(lhs.coords, rhs);
    }

    public static boolean le(ZTensor lhs, ZPoint rhs) {
      return le(lhs, rhs.coords);
    }

    public static boolean le(ZTensor lhs, ZTensor rhs) {
      switch (partialCompare(lhs, rhs)) {
        case LESS_THAN:
        case EQUAL:
          return true;
        default:
          return false;
      }
    }

    /**
     * Is `lhs > rhs`?
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return true or false.
     */
    public static boolean gt(ZPoint lhs, ZPoint rhs) {
      return gt(lhs.coords, rhs.coords);
    }

    public static boolean gt(ZPoint lhs, ZTensor rhs) {
      return gt(lhs.coords, rhs);
    }

    public static boolean gt(ZTensor lhs, ZPoint rhs) {
      return gt(lhs, rhs.coords);
    }

    public static boolean gt(ZTensor lhs, ZTensor rhs) {
      return partialCompare(lhs, rhs) == PartialOrdering.GREATER_THAN;
    }

    /**
     * Is `lhs >= rhs`?
     *
     * @param lhs the left-hand side.
     * @param rhs the right-hand side.
     * @return true or false.
     */
    public static boolean ge(ZPoint lhs, ZPoint rhs) {
      return ge(lhs.coords, rhs.coords);
    }

    public static boolean ge(ZPoint lhs, ZTensor rhs) {
      return ge(lhs.coords, rhs);
    }

    public static boolean ge(ZTensor lhs, ZPoint rhs) {
      return ge(lhs, rhs.coords);
    }

    public static boolean ge(ZTensor lhs, ZTensor rhs) {
      switch (partialCompare(lhs, rhs)) {
        case GREATER_THAN:
        case EQUAL:
          return true;
        default:
          return false;
      }
    }
  }

  public boolean eq(ZPoint rhs) {
    return ZPoint.Ops.eq(this, rhs);
  }

  public boolean eq(HasSize rhs) {
    return ZPoint.Ops.eq(this, rhs);
  }

  public boolean ne(ZPoint rhs) {
    return ZPoint.Ops.ne(this, rhs);
  }

  public boolean ne(HasSize rhs) {
    return ZPoint.Ops.ne(this, rhs);
  }

  public boolean lt(ZPoint rhs) {
    return ZPoint.Ops.lt(this, rhs);
  }

  public boolean lt(ZTensor rhs) {
    return ZPoint.Ops.lt(this, rhs);
  }

  public boolean le(ZPoint rhs) {
    return ZPoint.Ops.le(this, rhs);
  }

  public boolean le(ZTensor rhs) {
    return ZPoint.Ops.le(this, rhs);
  }

  public boolean gt(ZPoint rhs) {
    return ZPoint.Ops.gt(this, rhs);
  }

  public boolean gt(ZTensor rhs) {
    return ZPoint.Ops.gt(this, rhs);
  }

  public boolean ge(ZPoint rhs) {
    return ZPoint.Ops.ge(this, rhs);
  }

  public boolean ge(ZTensor rhs) {
    return ZPoint.Ops.ge(this, rhs);
  }
}
