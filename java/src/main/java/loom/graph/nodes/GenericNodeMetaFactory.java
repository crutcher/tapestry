package loom.graph.nodes;

import loom.graph.LoomGraph;

public final class GenericNodeMetaFactory extends LoomGraph.NodeMetaFactory {
  @Override
  public LoomGraph.NodeMeta<?, ?> getMetaForType(String type) {
    return GenericNode.META;
  }
}
