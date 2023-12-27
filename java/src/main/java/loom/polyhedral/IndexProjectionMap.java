package loom.polyhedral;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import loom.zspace.DimensionMap;
import loom.zspace.ZAffineMap;
import loom.zspace.ZPoint;

@ThreadSafe
@Immutable
@Jacksonized
@Builder
public class IndexProjectionMap {
  private final DimensionMap inputMap;
  private final DimensionMap outputMap;
  private final ZAffineMap affineMap;
  private final ZPoint sliceShape;
}
