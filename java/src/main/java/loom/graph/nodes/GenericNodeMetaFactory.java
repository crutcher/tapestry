package loom.graph.nodes;

import loom.graph.LoomGraph;

public final class GenericNodeMetaFactory extends LoomGraph.NodeMetaFactory {
  @Override
  public LoomGraph.NodePrototype<?, ?> getPrototypeForType(String type) {
    return GenericNode.PROTOTYPE;
  }
}
