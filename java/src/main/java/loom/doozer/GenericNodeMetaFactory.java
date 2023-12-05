package loom.doozer;

import loom.doozer.nodes.GenericNode;

public final class GenericNodeMetaFactory extends DoozerGraph.NodeMetaFactory {
  @Override
  public DoozerGraph.NodeMeta<?, ?> getMeta(String type) {
    return GenericNode.META;
  }
}
