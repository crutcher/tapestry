package loom.alt.densegraph;

import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@SuperBuilder
public final class EGOperatorDefinition {
  @Nonnull public final ScopedName name;

  @Builder.Default public final boolean external = false;

  // todo: atomic, aligned, projection

  // todo: schema
}
