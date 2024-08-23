package org.tensortapestry.weft.metakernels.expressions;

import java.util.Map;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.tensortapestry.common.collections.CollectionContracts;
import org.tensortapestry.zspace.ZPoint;

@Value
@Builder
public class TensorShapePatternMatch {

  @Value
  @Builder
  public static class DimMatch {

    String name;
    int index;
    int value;
  }

  @Value
  @Builder
  public static class GroupMatch {

    String name;
    int start;
    int end;
    ZPoint value;
  }

  @Nonnull
  TensorShapePatternMatcher pattern;

  @Nonnull
  ZPoint shape;

  @Singular
  @Nonnull
  Map<String, GroupMatch> groups;

  @Singular
  @Nonnull
  Map<String, DimMatch> dims;

  public TensorShapePatternMatch(
    @Nonnull TensorShapePatternMatcher pattern,
    @Nonnull ZPoint shape,
    @Nonnull Map<String, GroupMatch> groups,
    @Nonnull Map<String, DimMatch> dims
  ) {
    CollectionContracts.expectDistinct(dims.keySet(), groups.keySet(), "dims", "groups", "keys");
    CollectionContracts.expectMapKeysMatchItemKeys(dims, DimMatch::getName);
    CollectionContracts.expectMapKeysMatchItemKeys(groups, GroupMatch::getName);

    this.pattern = pattern;
    this.shape = shape;
    this.dims = Map.copyOf(dims);
    this.groups = Map.copyOf(groups);
  }
}
