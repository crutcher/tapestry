package loom.graph.nodes;

import loom.graph.LoomGraph;

public final class GenericNodeMetaFactory extends LoomGraph.NodeMetaFactory {
  @Override
  public LoomGraph.NodeMeta<?, ?> getMeta(String type) {
    return GenericNode.META;
  }
}
