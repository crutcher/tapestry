package org.tensortapestry.loom.graph.export.graphviz;

import java.util.UUID;
import org.tensortapestry.loom.graph.LoomNodeWrapper;

public interface GraphEntityColorSchemeProvider {
  default GraphEntityColorScheme colorSchemeForNode(LoomNodeWrapper node) {
    return colorSchemeForNode(node.getId());
  }

  GraphEntityColorScheme colorSchemeForNode(UUID id);
}
