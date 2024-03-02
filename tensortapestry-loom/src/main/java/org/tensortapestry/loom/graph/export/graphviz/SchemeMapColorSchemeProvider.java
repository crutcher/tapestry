package org.tensortapestry.loom.graph.export.graphviz;

import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;
import lombok.Value;

@Value(staticConstructor = "of")
public class SchemeMapColorSchemeProvider implements GraphEntityColorSchemeProvider {

  @Nonnull
  Map<UUID, GraphEntityColorScheme> nodeColorings;

  @Override
  public GraphEntityColorScheme colorSchemeForNode(UUID id) {
    return nodeColorings.get(id);
  }
}
