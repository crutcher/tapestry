package org.tensortapestry.loom.graph.export.graphviz;

import java.util.UUID;
import org.tensortapestry.loom.graph.LoomNodeWrapper;

@SuppressWarnings("unused")
public interface EntityColorSchemeEnv {
  default GraphEntityColorScheme colorSchemeForNode(LoomNodeWrapper<?> node) {
    return colorSchemeForNode(node.getId());
  }

  GraphEntityColorScheme colorSchemeForNode(UUID id);
}
