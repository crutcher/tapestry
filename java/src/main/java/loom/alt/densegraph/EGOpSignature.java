package loom.alt.densegraph;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Objects;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@JsonTypeName("OpMeta")
@Jacksonized
@SuperBuilder
@Getter
public final class EGOpSignature extends EGNodeBase {
  public final ScopedName op;
  public final boolean external;

  @Nullable public final EGPolyhedralSignature polySig;

  public boolean equivalent(EGOpSignature that) {
    return this.op.equals(that.op)
        && this.external == that.external
        && Objects.equals(this.polySig, that.polySig);
  }
}
