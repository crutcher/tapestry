package org.tensortapestry.loom.zspace;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Arrays;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.tensortapestry.loom.zspace.exceptions.ZDimMissMatchError;

/** Interface for objects that have a number of dimensions. */
public interface HasDimension {
  /**
   * Assert that the actual number of dimensions is equal to the expected number of dimensions.
   *
   * @param actual the actual number of dimensions.
   * @param expected the expected number of dimensions.
   */
  static void assertNDim(int actual, int expected) {
    if (actual != expected) {
      throw new ZDimMissMatchError("Expected ndim " + expected + ", got " + actual);
    }
  }

  /**
   * Assert that the object has the given number of dimensions.
   *
   * @param ndim the expected number of dimensions.
   * @throws ZDimMissMatchError if the object does not have the given number of dimensions.
   */
  default void assertNDim(int ndim) {
    assertNDim(getNDim(), ndim);
  }

  /**
   * Assert that all objects have the same number of dimensions.
   *
   * @param objs the objects to check.
   * @throws ZDimMissMatchError if the objects do not have the same number of dimensions.
   */
  static void assertSameNDim(@Nonnull HasDimension... objs) throws ZDimMissMatchError {
    int ndim = objs[0].getNDim();
    for (int i = 1; i < objs.length; ++i) {
      if (objs[i].getNDim() != ndim) {
        throw new ZDimMissMatchError(
          String.format(
            "ZDim mismatch: %s",
            Arrays.stream(objs).map(HasDimension::getNDim).collect(Collectors.toList())
          )
        );
      }
    }
  }

  /** Returns the number of dimensions of this object. */
  @JsonIgnore
  int getNDim();

  /** Is this object a scalar? */
  @JsonIgnore
  default boolean isScalar() {
    return getNDim() == 0;
  }
}
