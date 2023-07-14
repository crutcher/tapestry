package loom.alt.densegraph;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@JsonTypeName("Signature")
@Jacksonized
@SuperBuilder
@Getter
public final class EGSignature extends EGNodeBase {
  public final OperatorName op;
  public final boolean external;
}
