package loom.zspace;

import java.util.Arrays;
import java.util.stream.Collectors;

public interface ZDim {
  /**
   * Assert that all objects have the same number of dimensions.
   *
   * @param objs the objects to check.
   * @throws ZDimMissMatchError if the objects do not have the same number of dimensions.
   */
  static void assertSameZDim(ZDim... objs) throws ZDimMissMatchError {
    int ndim = objs[0].ndim();
    boolean same = true;
    for (ZDim o : objs) {
      if (o.ndim() != ndim) {
        same = false;
      }
    }

    if (same) {
      return;
    }

    throw new ZDimMissMatchError(
        String.format(
            "ZDim mismatch: %s", Arrays.stream(objs).map(ZDim::ndim).collect(Collectors.toList())));
  }

  int ndim();
}
