package loom.alt.densegraph;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@JsonTypeName("OpSignature")
@Jacksonized
@SuperBuilder
@Getter
public final class EGOpSignature extends EGNodeBase {
  public final ScopedName op;
  public final boolean external;

  public boolean equivalent(EGOpSignature that) {
    return this.getAttributes().equals(that.getAttributes())
        && this.op.equals(that.op)
        && this.external == that.external;
  }
}
