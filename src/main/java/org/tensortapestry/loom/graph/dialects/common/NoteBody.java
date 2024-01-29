package org.tensortapestry.loom.graph.dialects.common;

import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Jacksonized
@Builder
@JsdType(CommonNodes.NOTE_NODE_TYPE)
public final class NoteBody {

  @Nonnull
  private String message;
}
