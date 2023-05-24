package loom.zspace;

import java.util.Arrays;
import java.util.stream.Collectors;

public interface HasDimension {
  /**
   * Assert that the object has the given number of dimensions.
   *
   * @param ndim the expected number of dimensions.
   * @throws ZDimMissMatchError if the object does not have the given number of dimensions.
   */
  default void assertNDim(int ndim) {
    if (ndim() != ndim) {
      throw new ZDimMissMatchError(String.format("Expected %d dimensions, got %d", ndim, ndim()));
    }
  }

  /**
   * Assert that all objects have the same number of dimensions.
   *
   * @param objs the objects to check.
   * @throws ZDimMissMatchError if the objects do not have the same number of dimensions.
   */
  static void assertSameNDim(HasDimension... objs) throws ZDimMissMatchError {
    int ndim = objs[0].ndim();
    boolean same = true;
    for (HasDimension o : objs) {
      if (o.ndim() != ndim) {
        same = false;
      }
    }

    if (same) {
      return;
    }

    throw new ZDimMissMatchError(
        String.format(
            "ZDim mismatch: %s",
            Arrays.stream(objs).map(HasDimension::ndim).collect(Collectors.toList())));
  }

  int ndim();
}
