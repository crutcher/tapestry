package loom.alt.densegraph;

import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.annotation.Nonnull;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.zspace.ZPoint;

@JsonTypeName("Tensor")
@Jacksonized
@SuperBuilder
@Getter
public final class EGTensor extends EGNodeBase {
  @Nonnull public final ZPoint shape;

  @Nonnull public final String dtype;
}
