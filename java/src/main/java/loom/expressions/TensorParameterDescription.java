package loom.expressions;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import loom.zspace.HasDimension;

@Jacksonized
@Builder
@Data
public final class TensorParameterDescription implements HasDimension {
  public final String name;

  public ImmutableList<String> dimensions;

  @Override
  public int ndim() {
    return dimensions.size();
  }
}
