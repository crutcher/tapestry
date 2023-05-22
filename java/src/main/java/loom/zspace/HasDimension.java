package loom.zspace;

import java.util.Arrays;
import java.util.stream.Collectors;

public interface HasDimension {
  /**
   * Assert that all objects have the same number of dimensions.
   *
   * @param objs the objects to check.
   * @throws ZDimMissMatchError if the objects do not have the same number of dimensions.
   */
  static void assertSameZDim(HasDimension... objs) throws ZDimMissMatchError {
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
