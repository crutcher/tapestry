package loom.graph.nodes;

import loom.graph.LoomGraph;

public final class GenericNodeMetaFactory extends LoomGraph.NodeMetaFactory {
  @Override
  public LoomGraph.NodePrototype<?, ?> getMetaForType(String type) {
    return GenericNode.PROTOTYPE;
  }
}
