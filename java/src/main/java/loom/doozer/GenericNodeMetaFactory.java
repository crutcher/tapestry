package loom.doozer;

import loom.doozer.nodes.GenericNode;

public final class GenericNodeMetaFactory extends DoozerGraph.Node.NodeMetaFactory {
  @Override
  public DoozerGraph.Node.NodeMeta<?, ?> getMeta(String type) {
    return new GenericNode.Meta();
  }
}
